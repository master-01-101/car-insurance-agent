"""从口语词条提取用户明确说出的方案选择。"""
import re

from .knowledge import TERMS
from .models import Intent


QUESTION_WORDS = (
    '什么意思', '是什么', '啥意思', '怎么理解', '有什么区别',
    '介绍一下', '解释', '多少合适', '买多少', '要多少钱',
    '保什么', '怎么选',
)
SELECTION_WORDS = (
    '我要', '我想', '给我', '帮我', '投',
    '买', '上', '按', '推荐', '方案',
)
THIRD_PARTY = ('交三', '三者', '三责')
DAMAGE_INCLUDED = ('交三车', '交三损', '含损', '含车损')


def mentioned_insurers(message: str) -> list[str]:
    matches = []
    text = message.casefold()

    for item in TERMS:
        if item.get('category') != 'insurer':
            continue

        name = (
            item.get('definition', '')
            .split('；', 1)[0]
            .removeprefix('保险公司名称：')
        )
        if not name:
            continue

        aliases = [
            str(alias).strip()
            for alias in item.get('aliases', [])
        ]
        for alias in aliases:
            start = text.find(alias.casefold())
            if len(alias) >= 2 and start >= 0:
                matches.append(
                    (-len(alias), start, start + len(alias), name)
                )

    # 长名称优先，避免「太平洋」又被识别成「太平」。
    matches.sort()
    occupied = [False] * len(text)
    chosen = []

    for _, start, end, name in matches:
        if any(occupied[start:end]):
            continue
        occupied[start:end] = [True] * (end - start)
        chosen.append(name)

    return list(dict.fromkeys(chosen))


def amount_in(message: str) -> int | None:
    amounts = re.findall(r'(\d+(?:\.\d+)?)\s*万', message)
    if len(amounts) == 1:
        amount = int(float(amounts[0]) * 10000)
        return amount if 0 < amount <= 100000000 else None
    return None


def resolve_plan_request(
    message: str,
    intent: Intent,
    record: dict,
) -> Intent:
    """只处理明确的投保口语；知识问答及其他业务意图沿用模型结果。"""
    if any(word in message for word in QUESTION_WORDS):
        return intent

    if intent.action not in (
        'help',
        'recommend',
        'modify',
        'clarify',
    ):
        return intent

    insurers = mentioned_insurers(message)
    shorthand = any(word in message for word in THIRD_PARTY)

    selecting = (
        any(word in message for word in SELECTION_WORDS)
        or bool(insurers and shorthand)
        or bool(shorthand and amount_in(message))
    )
    if not selecting:
        return intent

    if '全险' in message:
        return Intent(
            action='clarify',
            question=(
                '“全险”没有统一险种组合。'
                '请说明想投保哪些险种，以及三者险保额。'
            ),
        )

    if len(insurers) > 1:
        return Intent(
            action='clarify',
            question='提到了多家保险公司，请明确选哪一家。',
        )

    if shorthand and '交三不计免' in message:
        return Intent(
            action='clarify',
            question=(
                '请确认想投保的具体险种和三者险保额；'
                '产品责任以当前可投方案为准。'
            ),
        )

    amount = intent.amount or amount_in(message)
    if shorthand and amount is None:
        return Intent(
            action='clarify',
            question=(
                '交强险加三者险可以办理，'
                '请问三者险保额选多少万？'
            ),
        )

    damage = intent.vehicle_damage

    if re.search(
        r'(?:不含|不要|取消|不投|去掉|别买|不买)\s*(?:车损|损险|损)',
        message,
    ):
        damage = False
    elif any(word in message for word in DAMAGE_INCLUDED):
        damage = True
    elif '交三' in message and '车损' not in message:
        damage = False

    insurer = insurers[0] if insurers else None

    existing = record.get('pending') or {}
    has_plan = bool(
        existing.get('kind') == 'plan'
        or record.get('plan')
    )

    action = intent.action
    if action in ('help', 'clarify'):
        action = 'modify' if has_plan else 'recommend'
    elif action == 'modify' and not has_plan:
        action = 'recommend'

    return Intent(
        action=action,
        amount=amount,
        vehicle_damage=damage,
        insurer=insurer,
    )