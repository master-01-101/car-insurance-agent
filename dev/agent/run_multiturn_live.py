"""用你本地模型配置进行验收。启动 FastAPI 后从项目根目录运行本脚本。"""
from pathlib import Path
from uuid import uuid4
import sys
import httpx
from dotenv import dotenv_values


def main():
    config = dotenv_values(Path(__file__).with_name('.env'))
    if not config.get('MODEL_KEY'):
        raise RuntimeError('.env 未配置 MODEL_KEY，无法验证真实模型')
    if config.get('MODE', 'demo') != 'demo':
        raise RuntimeError('此脚本仅供 MODE=demo 的测试业务使用')
    headers = {'Authorization': 'Bearer ' + config.get('SERVICE_TOKEN', ''),
               'X-User-Id': 'multiturn-test-' + uuid4().hex,
               'X-Salesperson-Id': 'sales-test'}
    with httpx.Client(base_url='http://127.0.0.1:8000', headers=headers, timeout=60) as client:
        def send(message, cid=None):
            body = {'message': message, 'conversation_id': cid}
            if cid is None:
                body['plate'] = '粤B67890'
            response = client.post('/ai/chat', json=body)
            if response.status_code != 200:
                raise RuntimeError(f'接口返回 {response.status_code}: {response.text}')
            result = response.json()
            print('\n用户：', message)
            print('助手：', result['reply'])
            return result['conversation']

        record = send('请推荐一份车险方案')
        cid = record['id']
        record = send('把三者险改成两百万，车损险保留', cid)
        assert record['pending']['value']['third_party_liability'] == 2000000, '两百万提取失败'
        record = send('还是三百万吧', cid)
        assert record['pending']['value']['third_party_liability'] == 3000000, '省略险种的追问失败'
        previous = record['pending']
        record = send('刚才提到了三者险和车损险，那个不要了', cid)
        assert record['pending'] == previous, '指代不清时不应修改待确认方案'
        record = send('我说的是车损险', cid)
        plan = record['pending']['value']
        assert plan['vehicle_damage'] is False, '澄清后的取消车损失败'
        assert plan['third_party_liability'] == 3000000, '修改车损不应覆盖三者保额'
        response = client.post(f'/ai/conversations/{cid}/confirm', json={'token': record['pending']['token']})
        response.raise_for_status()
        saved = response.json()
        assert saved['pending'] is None and saved['plan'] == plan, '方案确认保存失败'
        print('\nPASS：200万 → 300万 → 指代不清先追问 → 取消车损 → 确认保存')
        print('模型语义由此次实际运行验证；业务仍使用模拟数据。')


if __name__ == '__main__':
    if hasattr(sys.stdout, 'reconfigure'):
        sys.stdout.reconfigure(encoding='utf-8')
    try:
        main()
    except (AssertionError, KeyError, TypeError, RuntimeError, httpx.HTTPError) as exc:
        print('\nFAIL：', str(exc))
        print('请复制本次输出和服务端错误日志用于定位。')
        sys.exit(1)
