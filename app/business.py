"""业务网关适配层。生产状态、权限和幂等由成员 B 保存和校验。"""
import copy
import os
from uuid import uuid4
import httpx
from fastapi import HTTPException

class Business:
    def __init__(self):
        self.records = {}
        self.results = {}

    async def call(self, operation, principal, conversation_id, payload=None, key=None):
        payload = payload or {}
        if os.getenv('MODE', 'demo') != 'demo':
            async with httpx.AsyncClient(timeout=30) as client:
                response = await client.post(os.environ['BUSINESS_URL'].rstrip('/') + '/ai-tools/' + operation,
                    headers={'Authorization': 'Bearer ' + os.getenv('BUSINESS_TOKEN', '')},
                    json={'principal': principal.model_dump(), 'conversation_id': conversation_id,
                          'arguments': payload, 'idempotency_key': key})
                response.raise_for_status()
                return response.json()
        owner = principal.user_id
        cache_key = (owner, operation, key)
        if key and cache_key in self.results:
            return copy.deepcopy(self.results[cache_key])
        if operation == 'open':
            existing = next((r for r in self.records.values() if r['owner'] == owner and r['plate'] == payload['plate']), None)
            if existing:
                return copy.deepcopy(existing)
            conversation_id = str(uuid4())
            self.records[conversation_id] = {'id': conversation_id, 'owner': owner,
                'salesperson_id': principal.salesperson_id, 'plate': payload['plate'],
                'title': payload['plate'], 'state': 'collecting', 'materials': {},
                'plan': None, 'pending': None, 'policy': None, 'payment': None, 'version': 0}
        record = self.records.get(conversation_id)
        if not record or record['owner'] != owner:
            raise HTTPException(404, '会话不存在')
        if record['state'] == 'human' and operation not in ('get', 'open'):
            raise HTTPException(409, '已由人工接管，自动操作暂停')
        if operation == 'stage':
            if record['state'] not in ('collecting', 'ready'):
                raise HTTPException(409, '当前阶段不能修改资料或方案')
            record['pending'] = {'token': str(uuid4()), **payload}
        elif operation == 'confirm':
            pending = record['pending']
            if not pending or pending['token'] != payload['token']:
                raise HTTPException(409, '确认已失效，请刷新待确认内容')
            if pending['kind'] == 'plan':
                record['plan'] = pending['value']
            else:
                record['materials'][pending['kind']] = pending['value']
            record['pending'] = None
            record['state'] = 'ready'
        elif operation == 'recommend' or operation == 'history':
            # 清晰标注测试价格；续保资料不自动视为已确认。
            return {'insurer': '演示保险公司', 'third_party_liability': 1000000,
                    'vehicle_damage': True, 'compulsory': True, 'estimated_price': 1234,
                    'price_source': '测试数据'}
        elif operation == 'quote':
            needed = {'license_front', 'license_back', 'id_front', 'id_back'}
            if record['state'] not in ('ready', 'quoted') or not record['plan'] or record['pending']:
                raise HTTPException(409, '请先确认方案及全部待确认内容')
            if not needed.issubset(record['materials']):
                raise HTTPException(409, '缺少已确认材料 ' + ','.join(sorted(needed - record['materials'].keys())))
            record['quote'] = {'total': 1234, 'source': '测试数据', 'currency': 'CNY'}
            record['state'] = 'quoted'
        elif operation == 'underwriting':
            if record['state'] not in ('quoted', 'supplement', 'approved'):
                raise HTTPException(409, '须先完成报价')
            if record['plate'].endswith('999') and 'inspection' not in record['materials']:
                record['state'] = 'supplement'
                record['required_materials'] = ['inspection']
            else:
                record['state'] = 'approved'
                record['required_materials'] = []
        elif operation == 'supplement':
            if record['state'] != 'supplement':
                raise HTTPException(409, '当前未要求补件')
            record['materials']['inspection'] = payload
        elif operation == 'payment':
            if record['state'] not in ('approved', 'paying', 'paid', 'issued'):
                raise HTTPException(409, '核保通过后才能支付')
            if not record['payment']:
                record['payment'] = {'id': str(uuid4()), 'status': 'pending', 'url': None,
                                     'notice': '模拟支付，使用 demo-pay 接口模拟平台回调'}
                record['state'] = 'paying'
        elif operation == 'demo_pay':
            if record['state'] != 'paying':
                raise HTTPException(409, '未创建待支付订单')
            record['payment']['status'] = 'paid'
            record['state'] = 'paid'
        elif operation == 'policy':
            if record['state'] not in ('paid', 'issued'):
                raise HTTPException(409, '服务端尚未确认支付成功')
            record['state'] = 'issued'
            record['policy'] = {'number': 'DEMO-' + record['id'], 'vehicle': record['plate'],
                'applicant': '测试投保人', 'insured': '测试被保险人', 'plan': record['plan'],
                'items': [{'type': 'compulsory', 'premium': 234}, {'type': 'commercial', 'premium': 1000}],
                'effective_at': '2027-01-01', 'expires_at': '2027-12-31',
                'terms': '仅演示，不具有保险效力'}
        elif operation == 'handoff':
            record['state'] = 'human'
            record['handoff'] = {'id': str(uuid4()), **payload}
        elif operation == 'cancel':
            record['pending'] = None
        elif operation not in ('get', 'open'):
            raise HTTPException(400, '未知业务操作')
        record['version'] += 1
        result = copy.deepcopy(record)
        if key:
            self.results[cache_key] = result
        return result

business = Business()
