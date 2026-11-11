"""百度身份证正反面识别；只返回待核对字段，不保存身份证图片。"""

import base64
import os
from urllib.parse import urlencode

import httpx
from dotenv import load_dotenv
from fastapi import HTTPException

load_dotenv()

TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token"
IDCARD_URL = "https://aip.baidubce.com/rest/2.0/ocr/v1/idcard"

MAX_IMAGE_BYTES = 4 * 1024 * 1024
SUPPORTED_TYPES = {"image/jpeg", "image/png", "image/bmp"}

FRONT_FIELDS = {
    "姓名": "name",
    "性别": "gender",
    "民族": "ethnicity",
    "出生": "birth_date",
    "住址": "address",
    "公民身份号码": "id_number",
}

BACK_FIELDS = {
    "签发机关": "issuing_authority",
    "签发日期": "valid_from",
    "失效日期": "valid_to",
    "有效期限": "valid_to",
}

REQUIRED_FIELDS = {
    "id_front": ("name", "id_number"),
    "id_back": ("issuing_authority", "valid_to"),
}


def map_idcard_result(kind: str, data: dict) -> dict:
    """把百度返回的中文字段转成系统字段，并标记需要人工核对的情况。"""

    if kind not in REQUIRED_FIELDS:
        raise ValueError("kind 必须是 id_front 或 id_back")

    if data.get("error_code"):
        raise HTTPException(status_code=502, detail="百度身份证 OCR 调用失败")

    words = data.get("words_result")
    if not isinstance(words, dict):
        raise HTTPException(status_code=502, detail="百度身份证 OCR 返回格式不正确")

    mapping = FRONT_FIELDS if kind == "id_front" else BACK_FIELDS
    fields = {}

    for chinese_name, field_name in mapping.items():
        item = words.get(chinese_name)
        value = item.get("words") if isinstance(item, dict) else None

        if isinstance(value, str) and value.strip():
            fields[field_name] = value.strip()

    missing_fields = [
        name for name in REQUIRED_FIELDS[kind] if not fields.get(name)
    ]

    warnings = []
    image_status = data.get("image_status")

    if image_status != "normal":
        warnings.append("image_status:" + str(image_status or "missing"))

    if kind == "id_front" and data.get("idcard_number_type") != 1:
        warnings.append("idcard_number_check_failed")

    return {
        "fields": fields,
        "missing_fields": missing_fields,
        "needs_review": bool(missing_fields or warnings),
        "warnings": warnings,
        "provider": "baidu",
    }


def _credential(*names: str) -> str:
    for name in names:
        value = os.getenv(name, "").strip()
        if value:
            return value
    return ""


async def recognize_id_card(
    kind: str,
    image: bytes,
    content_type: str,
) -> dict:
    """调用百度身份证 OCR；不在日志中输出图片、密钥或识别字段。"""

    if kind not in REQUIRED_FIELDS:
        raise HTTPException(status_code=422, detail="身份证页类型错误")

    if content_type not in SUPPORTED_TYPES:
        raise HTTPException(
            status_code=415,
            detail="身份证 OCR 支持 JPEG、PNG、BMP",
        )

    if not image or len(image) > MAX_IMAGE_BYTES:
        raise HTTPException(
            status_code=413,
            detail="身份证图片为空或超过 4MB",
        )

    api_key = _credential("BAIDU_API_KEY", "BAIDU_OCR_API_KEY")
    secret_key = _credential("BAIDU_SECRET_KEY", "BAIDU_OCR_SECRET_KEY")

    if not api_key or not secret_key:
        raise HTTPException(
            status_code=503,
            detail="未配置百度 OCR 的 API Key 和 Secret Key",
        )

    side = "front" if kind == "id_front" else "back"

    payload = {
        "id_card_side": side,
        "image": base64.b64encode(image).decode("ascii"),
    }

    if len(urlencode(payload).encode("ascii")) > 8_000_000:
        raise HTTPException(
            status_code=413,
            detail="身份证图片编码后超过百度接口限制",
        )

    try:
        async with httpx.AsyncClient(timeout=30) as client:
            token_response = await client.post(
                TOKEN_URL,
                data={
                    "grant_type": "client_credentials",
                    "client_id": api_key,
                    "client_secret": secret_key,
                },
            )
            token_response.raise_for_status()

            token = token_response.json().get("access_token")
            if not token:
                raise HTTPException(
                    status_code=502,
                    detail="百度 OCR 未签发访问令牌",
                )

            response = await client.post(
                IDCARD_URL,
                params={"access_token": token},
                data=payload,
            )
            response.raise_for_status()
            result = response.json()

    except httpx.HTTPError as exc:
        raise HTTPException(
            status_code=502,
            detail="百度身份证 OCR 服务不可用",
        ) from exc

    except ValueError as exc:
        raise HTTPException(
            status_code=502,
            detail="百度身份证 OCR 返回了非 JSON 内容",
        ) from exc

    return map_idcard_result(kind, result)