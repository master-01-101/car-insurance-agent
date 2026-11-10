import asyncio
import os
import re
import secrets
from collections import defaultdict, OrderedDict
from time import monotonic
from copy import deepcopy
from typing import Literal
from uuid import uuid4
from dotenv import load_dotenv
load_dotenv()
import httpx
from fastapi import FastAPI, Depends, Header, HTTPException, UploadFile, File
from fastapi.responses import JSONResponse
from .models import Principal, ChatRequest, BatchRequest, ConfirmRequest
from .providers import recognize
from .agent_graph import agent_graph
from .business import business

app = FastAPI(title='车险 AI 服务', version='1.0.0')
locks = defaultdict(asyncio.Lock)
# 本地开发的有界会话记忆；生产多进程需要共享存储，由后端提供。
conversation_memory = OrderedDict()
MEMORY_TTL_SECONDS = 1800
MAX_MEMORY_SESSIONS = 512
MAX_HISTORY_TURNS = 6


def recent_turns(key):
    now = monotonic()
    expired = [session for session, entry in conversation_memory.items()
               if now - entry['updated_at'] >= MEMORY_TTL_SECONDS]
    for session in expired:
        del conversation_memory[session]
    entry = conversation_memory.get(key)
    return deepcopy(entry['turns']) if entry else []


def remember_turn(key, history, message, reply):
    conversation_memory[key] = {
        'updated_at': monotonic(),
        'turns': (history + [{'user': message, 'assistant': reply}])[-MAX_HISTORY_TURNS:],
    }
    conversation_memory.move_to_end(key)
    while len(conversation_memory) > MAX_MEMORY_SESSIONS:
        conversation_memory.popitem(last=False)


async def auth(authorization: str = Header(), x_user_id: str = Header(),
               x_salesperson_id: str = Header(), x_role: Literal['user','salesperson'] = Header(default='user')):
    token = os.getenv('SERVICE_TOKEN', '')
    if not token or not secrets.compare_digest(authorization, 'Bearer ' + token):
        raise HTTPException(401, '服务认证失败')
    # 本接口只供可信后端调用；这些身份头必须由后端根据登录态写入。
    return Principal(user_id=x_user_id, salesperson_id=x_salesperson_id, role=x_role)

@app.exception_handler(httpx.HTTPError)
async def upstream_error(request, exc):
    return JSONResponse(status_code=502, content={'detail': '上游接口失败或超时，请查询状态后重试。'})

@app.get('/health')
async def health():
    return {'status': 'ok', 'mode': os.getenv('MODE', 'demo'), 'engine': 'langgraph'}

def public(record):
    result = dict(record)
    result['materials'] = {kind: {'confirmed': True} for kind in record.get('materials', {})}
    pending = result.get('pending')
    if pending and pending['kind'] != 'plan':
        pending = dict(pending)
        pending['value'] = {k: ('***' + str(v)[-4:] if k in ('id_number','name','address','owner') else v)
                            for k,v in pending['value'].items()}
        result['pending'] = pending
    return result

async def handle(req, principal):
    plates = re.findall(r'[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼][A-Z][A-Z0-9]{5,6}', req.message.upper())
    if len(set(plates)) > 1:
        raise HTTPException(422, '请每条消息办理一辆车，业务员可使用批量接口')
    plate = (req.plate or (plates[0] if plates else None))
    if plate:
        if not re.fullmatch(r'[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼][A-Z][A-Z0-9]{5,6}', plate.upper()):
            raise HTTPException(422, '请提供有效车牌号')
        record = await business.call('open', principal, None, {'plate': plate.upper()})
        cid = record['id']
    else:
        cid = req.conversation_id
    if not cid:
        return {'reply': '请提供车牌号开始办理。', 'demo': os.getenv('MODE','demo') == 'demo'}
    async with locks[(principal.user_id, cid)]:
        memory_key = (principal.user_id, principal.salesperson_id, cid)
        history = recent_turns(memory_key)
        result = await agent_graph.ainvoke({
            'event': 'chat', 'principal': principal, 'conversation_id': cid,
            'message': req.message, 'history': history,
        })
        if result.get('clear_history'):
            conversation_memory.pop(memory_key, None)
            history = []
        if not result.get('skip_history'):
            remember_turn(memory_key, history, req.message, result['reply'])
        return {'reply': result['reply'], 'conversation': public(result['record']),
                'demo': os.getenv('MODE', 'demo') == 'demo'}

@app.post('/ai/chat')
async def chat(req: ChatRequest, p: Principal = Depends(auth)):
    return await handle(req, p)

@app.get('/ai/conversations/{cid}')
async def state(cid: str, p: Principal = Depends(auth)):
    return public(await business.call('get', p, cid))

@app.post('/ai/conversations/{cid}/confirm')
async def confirm(cid: str, req: ConfirmRequest, p: Principal = Depends(auth)):
    async with locks[(p.user_id, cid)]:
        result = await agent_graph.ainvoke({
            'event': 'confirm', 'principal': p, 'conversation_id': cid,
            'confirmation_token': req.token,
        })
        return public(result['record'])

@app.post('/ai/conversations/{cid}/materials/{kind}')
async def upload(cid: str, kind: Literal['license_front','license_back','id_front','id_back','inspection'],
                 file: UploadFile = File(), p: Principal = Depends(auth)):
    if file.content_type not in ('image/jpeg','image/png','application/pdf','video/mp4'):
        raise HTTPException(415, '支持 JPEG、PNG、PDF 和验车 MP4')
    if file.content_type == 'video/mp4' and kind != 'inspection':
        raise HTTPException(415, '证件材料需要图片或 PDF')
    data = await file.read(10 * 1024 * 1024 + 1)
    if not data or len(data) > 10 * 1024 * 1024:
        raise HTTPException(413, '文件为空或超过 10MB')
    async with locks[(p.user_id,cid)]:
        record = await business.call('get', p, cid)
        if record['state'] == 'human':
            raise HTTPException(409, '人工接管中')
        # 原始文件通过受信任业务网关保存；AI 服务不直接写业务数据库。
        if os.getenv('MODE','demo') != 'demo':
            import base64
            saved = await business.call('upload_material', p, cid,
                {'kind': kind, 'content_type': file.content_type, 'base64': base64.b64encode(data).decode()}, key=str(uuid4()))
            material_id = saved['material_id']
        else:
            material_id = 'demo-' + str(uuid4())
        if kind == 'inspection':
            return public(await business.call('supplement', p, cid, {'material_id': material_id}))
        result = await recognize(kind, data, file.content_type)
        if result['needs_review']:
            record = await business.call('handoff', p, cid, {'reason': 'OCR 字段缺失或冲突', 'material_id': material_id})
            return {'reply': '请核对识别错误字段，已提交人工处理。', 'conversation': public(record)}
        fields = {**result['fields'], 'material_id': material_id}
        return public(await business.call('stage', p, cid, {'kind': kind, 'value': fields}))

@app.post('/ai/batch')
async def batch(req: BatchRequest, p: Principal = Depends(auth)):
    if p.role != 'salesperson':
        raise HTTPException(403, '仅业务员可批量操作')
    results = []
    for item in req.items:
        try:
            results.append({'ok': True, 'result': await handle(item,p)})
        except (HTTPException, httpx.HTTPError, ValueError) as exc:
            results.append({'ok': False, 'error': getattr(exc,'detail','上游处理失败')})
    return {'results': results}

@app.post('/demo/{cid}/pay')
async def demo_pay(cid: str, p: Principal = Depends(auth)):
    if os.getenv('MODE','demo') != 'demo':
        raise HTTPException(404)
    return public(await business.call('demo_pay',p,cid))
