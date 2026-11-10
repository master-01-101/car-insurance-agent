"""小规模术语检索。模型回答只使用命中词条；向量检索见 rag.py。"""
import json
from pathlib import Path

TERMS = json.loads((Path(__file__).parent / 'terms.json').read_text(encoding='utf-8'))

def search(query: str) -> list[dict]:
    return [item for item in TERMS if any(word in query for word in item['aliases'])]
