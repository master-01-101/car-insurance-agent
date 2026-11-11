import asyncio
import os
from pathlib import Path

from dotenv import load_dotenv

load_dotenv(".env", override=True)

from app.providers import recognize


async def check(kind: str, path_text: str) -> dict:
    path = Path(path_text)

    if not path.is_file():
        raise SystemExit(f"找不到 {kind} 图片，请检查路径")

    suffix = path.suffix.lower()
    if suffix not in (".jpg", ".jpeg", ".png"):
        raise SystemExit(f"{kind} 请使用 JPG 或 PNG 图片")

    content_type = "image/png" if suffix == ".png" else "image/jpeg"
    result = await recognize(kind, path.read_bytes(), content_type)

    print(f"{kind}：")
    print("  字段名：", list(result["fields"]))
    print("  需要核对：", result["needs_review"])
    print("  缺失字段：", result.get("missing_fields", []))

    return result["fields"]


async def main():
    if os.getenv("OCR_MODE", "").lower() != "baidu":
        raise SystemExit("请检查 .env：OCR_MODE 应为 baidu")

    front_path = os.environ["OCR_FRONT_IMAGE"]
    back_path = os.environ["OCR_BACK_IMAGE"]

    front = await check("license_front", front_path)
    back = await check("license_back", back_path)

    front_plate = front.get("plate_number")
    back_plate = back.get("plate_number")

    if front_plate and back_plate and front_plate == back_plate:
        print("PASS: 正副页车牌一致")
    elif not front_plate or not back_plate:
        print("REVIEW: 至少一页未识别出车牌，需要核对")
    else:
        print("REVIEW: 正副页车牌不一致，需要核对")


asyncio.run(main())