"""本地术语检索：从具体口语映射到规范释义。"""
import json
from pathlib import Path


TERMS = json.loads(
    (Path(__file__).parent / 'terms.json').read_text(encoding='utf-8')
)


def search(query: str, limit: int = 5) -> list[dict]:
    """优先匹配长说法，并避免「交三不计免」又命中「交三」。"""
    text = query.casefold()
    candidates = []

    for index, item in enumerate(TERMS):
        for alias in item.get('aliases', []):
            word = str(alias).strip().casefold()
            if len(word) < 2:
                continue

            position = text.find(word)
            if position >= 0:
                candidates.append(
                    (-len(word), position, index, position + len(word))
                )

    candidates.sort()
    occupied = [False] * len(text)
    chosen = []
    chosen_items = set()

    for negative_length, start, index, end in candidates:
        if index in chosen_items or any(occupied[start:end]):
            continue

        occupied[start:end] = [True] * (end - start)
        chosen_items.add(index)
        chosen.append((start, negative_length, index))

    chosen.sort()
    return [
        TERMS[index]
        for _, _, index in chosen[:max(0, limit)]
    ]
