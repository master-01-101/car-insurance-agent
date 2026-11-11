"""将业务员知识表中的口语映射导入 app/terms.json。"""
import json
import re
import sys
from pathlib import Path

try:
    from openpyxl import load_workbook
except ImportError as exc:
    raise SystemExit(
        '缺少 openpyxl，请先运行：python -m pip install openpyxl'
    ) from exc


ROOT = Path(__file__).resolve().parent
DEFAULT_WORKBOOK = ROOT / '3.业务员出单知识库.xlsx'
OUTPUT = ROOT / 'app' / 'terms.json'


def aliases_of(value):
    parts = re.split(r'[/／、;；\n]+', str(value or ''))
    seen = set()
    aliases = []

    for part in parts:
        word = part.strip()
        if len(word) < 2 or word.casefold() in seen:
            continue
        aliases.append(word)
        seen.add(word.casefold())

    return aliases


def make_item(category, source, oral, definition, clarification=''):
    aliases = aliases_of(oral)
    if not aliases:
        return None

    if '全险' in aliases:
        definition = (
            '“全险”没有统一的险种组合，'
            '须向用户询问具体险种和保额。'
        )
    elif clarification and clarification != '无':
        definition += '；办理前需确认：' + clarification

    return {
        'term': aliases[0],
        'code': source.replace('#', '_'),
        'aliases': aliases,
        'definition': definition,
        'category': category,
        'source': source,
        'reviewer': '待团队审核',
    }


def import_rows(workbook, workbook_name):
    items = []
    sheets = ('险种口语速查', '保司口语映射', '场景口语')

    missing = [
        name for name in sheets
        if name not in workbook.sheetnames
    ]
    if missing:
        raise ValueError('知识库缺少工作表：' + '、'.join(missing))

    for name in sheets:
        sheet = workbook[name]

        for row_num, row in enumerate(
            sheet.iter_rows(min_row=2, values_only=True),
            start=2,
        ):
            if not row or not row[0] or not row[1]:
                continue

            oral = str(row[0]).strip()
            second = str(row[1]).strip()
            third = (
                str(row[2]).strip()
                if len(row) > 2 and row[2]
                else ''
            )
            fourth = (
                str(row[3]).strip()
                if len(row) > 3 and row[3]
                else ''
            )
            source = f'{workbook_name}#{name}:{row_num}'

            if name == '险种口语速查':
                definition = (
                    f'{second}；对应险种：{third}'
                    if third else second
                )
                item = make_item(
                    'coverage',
                    source,
                    oral,
                    definition,
                    fourth,
                )

            elif name == '保司口语映射':
                definition = f'保险公司名称：{second}'
                if third:
                    definition += f'；备注：{third}'

                item = make_item(
                    'insurer',
                    source,
                    oral,
                    definition,
                )

            else:
                item = make_item(
                    'scenario',
                    source,
                    oral,
                    second,
                )
                if item and '网约车' in item['aliases']:
                    item['aliases'].append('跑滴滴')

            if item:
                items.append(item)

    return items


def main():
    workbook_path = (
        Path(sys.argv[1]).expanduser()
        if len(sys.argv) > 1
        else DEFAULT_WORKBOOK
    )

    if not workbook_path.is_file():
        raise SystemExit(f'找不到知识库文件：{workbook_path}')

    workbook = load_workbook(
        workbook_path,
        read_only=True,
        data_only=True,
    )
    try:
        imported = import_rows(
            workbook,
            workbook_path.name,
        )
    finally:
        workbook.close()

    existing = (
        json.loads(OUTPUT.read_text(encoding='utf-8'))
        if OUTPUT.exists()
        else []
    )
    if not isinstance(existing, list):
        raise ValueError('app/terms.json 必须是词条列表')

    prefix = workbook_path.name + '#'
    original = [
        item for item in existing
        if not str(item.get('source', '')).startswith(prefix)
    ]
    combined = original + imported

    temporary = OUTPUT.with_suffix('.json.tmp')
    temporary.write_text(
        json.dumps(combined, ensure_ascii=False, indent=2) + '\n',
        encoding='utf-8',
    )
    temporary.replace(OUTPUT)

    print(
        f'PASS: 保留原有词条 {len(original)} 条，'
        f'导入表格词条 {len(imported)} 条，'
        f'合计 {len(combined)} 条'
    )


if __name__ == '__main__':
    main()