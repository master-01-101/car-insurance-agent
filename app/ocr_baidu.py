import base64
import os

import httpx
from fastapi import HTTPException


TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token"
LICENSE_URL = "https://aip.baidubce.com/rest/2.0/ocr/v1/vehicle_license"


async def recognize_vehicle_license(image: bytes, side: str) -> dict:
    """side 为 front（正页）或 back（副页）。返回服务商原始识别结果。"""
    if side not in ("front", "back"):
        raise ValueError("side 必须是 front 或 back")
    if not image:
        raise ValueError("图片不能为空")

    api_key = os.getenv("BAIDU_OCR_API_KEY")
    secret_key = os.getenv("BAIDU_OCR_SECRET_KEY")
    if not api_key or not secret_key:
        raise HTTPException(503, "尚未配置百度 OCR 凭证")

    async with httpx.AsyncClient(timeout=40) as client:
        token_response = await client.post(
            TOKEN_URL,
            params={
                "grant_type": "client_credentials",
                "client_id": api_key,
                "client_secret": secret_key,
            },
        )
        token_response.raise_for_status()
        token = token_response.json().get("access_token")
        if not token:
            raise HTTPException(502, "无法取得 OCR 访问令牌")

        ocr_response = await client.post(
            LICENSE_URL,
            params={"access_token": token},
            data={
                "image": base64.b64encode(image).decode("ascii"),
                "vehicle_license_side": side,
            },
        )
        ocr_response.raise_for_status()
        result = ocr_response.json()

    if "error_code" in result:
        raise HTTPException(502, f"OCR 识别失败，错误码：{result['error_code']}")

    return result