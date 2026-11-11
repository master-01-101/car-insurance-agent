import os
os.environ['MODE']='demo'
os.environ['SERVICE_TOKEN']='test'
os.environ.pop('MODEL_KEY',None)
from fastapi.testclient import TestClient
from app.main import app
client=TestClient(app)
headers={'Authorization':'Bearer test','X-User-Id':'u1','X-Salesperson-Id':'s1'}
def post(path, body=None, **kw):
    r=client.post(path,json=body,headers=headers,**kw)
    assert r.status_code==200, r.text
    return r.json()
r=post('/ai/chat',{'plate':'粤B12345','message':'推荐方案'})
cid=r['conversation']['id']
def confirm(r):
    return post(f'/ai/conversations/{cid}/confirm', {'token':r['pending']['token']})
confirm(r['conversation'])
r=post('/ai/chat',{'conversation_id':cid,'message':'三者改到200万'})
assert r['conversation']['pending']['value']['third_party_liability']==2000000
confirm(r['conversation'])
assert client.post('/ai/chat',json={'conversation_id':cid,'message':'报价'},headers=headers).status_code==409
for kind in ('license_front','license_back','id_front','id_back'):
    r=client.post(f'/ai/conversations/{cid}/materials/{kind}', files={'file':('demo.png',b'demo','image/png')},headers=headers)
    assert r.status_code==200,r.text
    confirm(r.json())
r=post('/ai/chat',{'conversation_id':cid,'message':'报价'})
assert r['conversation']['state']=='approved'
r=post('/ai/chat',{'conversation_id':cid,'message':'我付过了'})
assert r['conversation']['state']=='paying'
assert client.post('/ai/chat',json={'conversation_id':cid,'message':'保单'},headers=headers).status_code==409
post(f'/demo/{cid}/pay')
assert post('/ai/chat',{'conversation_id':cid,'message':'保单'})['conversation']['state']=='issued'
assert client.get(f'/ai/conversations/{cid}',headers={**headers,'X-User-Id':'other'}).status_code==404
r=post('/ai/chat',{'plate':'粤B54321','message':'人工'})
assert post('/ai/chat',{'conversation_id':r['conversation']['id'],'message':'推荐方案'})['conversation']['state']=='human'
assert client.post('/ai/batch',json={'items':[{'message':'推荐','plate':'粤B22222'}]},headers=headers).status_code==403
print('PASS: 端到端、确认、报价前置、支付核验、身份隔离、人工暂停、批量权限')
