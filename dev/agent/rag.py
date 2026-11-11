"""可选 Qdrant 检索。运行 python -m app.rag 初始化知识库。
依赖现有 httpx，使用 OpenAI 兼容 embeddings 接口和 Qdrant REST。
"""
import asyncio
import os
import httpx
from dotenv import load_dotenv
from .knowledge import TERMS
load_dotenv()

async def embed(client, texts):
    r = await client.post(os.environ['EMBEDDING_URL'].rstrip('/') + '/embeddings',
        headers={'Authorization':'Bearer ' + os.environ['EMBEDDING_KEY']},
        json={'model':os.environ['EMBEDDING_MODEL'], 'input':texts})
    r.raise_for_status()
    return [row['embedding'] for row in sorted(r.json()['data'],key=lambda row:row['index'])]

async def initialize():
    async with httpx.AsyncClient(timeout=60) as client:
        vectors = await embed(client,[item['term'] + ' ' + ' '.join(item['aliases']) + ' ' + item['definition'] for item in TERMS])
        url = os.environ['QDRANT_URL'].rstrip('/') + '/collections/insurance_terms'
        headers = {'api-key':os.getenv('QDRANT_KEY','')}
        r = await client.get(url,headers=headers)
        if r.status_code == 404:
            r = await client.put(url,headers=headers,json={'vectors':{'size':len(vectors[0]),'distance':'Cosine'}})
        r.raise_for_status()
        r = await client.put(url + '/points?wait=true',headers=headers,
            json={'points':[{'id':i,'vector':vector,'payload':item} for i,(vector,item) in enumerate(zip(vectors,TERMS))]})
        r.raise_for_status()

async def retrieve(query):
    async with httpx.AsyncClient(timeout=30) as client:
        vector = (await embed(client,[query]))[0]
        r = await client.post(os.environ['QDRANT_URL'].rstrip('/') + '/collections/insurance_terms/points/search',
            headers={'api-key':os.getenv('QDRANT_KEY','')},
            json={'vector':vector,'limit':3,'with_payload':True,'score_threshold':0.7})
        r.raise_for_status()
        return [row['payload'] for row in r.json()['result']]

if __name__ == '__main__':
    asyncio.run(initialize())
