"""本机调用百度身份证 OCR，仅打印状态和字段名。"""

import asyncio
import io
import os
from pathlib import Path

from dotenv import load_dotenv
from fastapi import HTTPException
from PIL import Image, ImageOps

load_dotenv(".env", override=True)

from app.ocr_idcard import recognize_id_card


async def check(kind: str, variable: str):
    path = os.getenv(variable, "").strip()

    if not path:
        print(f"SKIP: {kind} 未配置图片路径")
        return

    source = Path(path)

    if not source.is_file():
        print(f"FAIL: {kind} 图片路径不存在")
        return

    try:
        raw = source.read_bytes()

        with Image.open(io.BytesIO(raw)) as image:
            image_format = image.format

            if image_format == "WEBP":
                output = io.BytesIO()
                ImageOps.exif_transpose(image).convert("RGB").save(
                    output,
                    "PNG",
                )
                raw = output.getvalue()
                mime = "image/png"
            else:
                mime = {
                    "JPEG": "image/jpeg",
                    "PNG": "image/png",
                    "BMP": "image/bmp",
                }.get(image_format)

        if not mime:
            print(f"FAIL: {kind} 图片格式不支持")
            return

        result = await recognize_id_card(kind, raw, mime)

    except HTTPException as exc:
        print(
            f"FAIL: {kind} HTTP {exc.status_code}: {exc.detail}"
        )
        return
    except OSError:
        print(f"FAIL: {kind} 无法读取图片")
        return

    print(f'{kind} 字段名: {sorted(result["fields"])}')
    print(f'{kind} 需要核对: {result["needs_review"]}')
    print(f'{kind} 缺失字段: {result["missing_fields"]}')
    print(f'{kind} 警告: {result["warnings"]}')


async def main():
    await check("id_front", "OCR_ID_FRONT_IMAGE")
    await check("id_back", "OCR_ID_BACK_IMAGE")


if __name__ == "__main__":
    asyncio.run(main())