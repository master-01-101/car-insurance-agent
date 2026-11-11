"""用空白图片检查身份证识别接口权限；不上传真实证件。"""

import asyncio
import base64
import io
import os

import httpx
from dotenv import load_dotenv
from PIL import Image


TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token"
IDCARD_URL = "https://aip.baidubce.com/rest/2.0/ocr/v1/idcard"


def get_env(*names):
    for name in names:
        value = os.getenv(name, "").strip()
        if value:
            return value
    return ""


async def main():
    load_dotenv(".env", override=True)

    api_key = get_env("BAIDU_API_KEY", "BAIDU_OCR_API_KEY")
    secret_key = get_env(
        "BAIDU_SECRET_KEY",
        "BAIDU_OCR_SECRET_KEY",
    )

    if not api_key or not secret_key:
        print(
            "FAIL: 找不到百度 API Key 或 Secret Key；"
            "只核对变量名，不要发送密钥"
        )
        return

    output = io.BytesIO()
    Image.new("RGB", (400, 250), "white").save(
        output,
        format="PNG",
    )
    image_b64 = base64.b64encode(
        output.getvalue()
    ).decode("ascii")

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

            token_data = token_response.json()
            token = token_data.get("access_token")

            if not token:
                print(
                    "FAIL: 未获取访问令牌；错误类别:",
                    token_data.get("error", "unknown"),
                )
                return

            result_response = await client.post(
                IDCARD_URL,
                params={"access_token": token},
                data={
                    "id_card_side": "front",
                    "image": image_b64,
                },
            )
            result_response.raise_for_status()
            result = result_response.json()

    except (httpx.HTTPError, ValueError):
        print(
            "FAIL: 请求失败或返回格式错误；"
            "请检查网络、代理和百度服务"
        )
        return

    code = result.get("error_code")

    if code == 216633:
        print(
            "PASS: 百度接口已处理请求；"
            "空白图不是身份证，所以返回 216633"
        )
    elif code == 6:
        print(
            "FAIL: 应用没有身份证识别接口权限（错误码 6）"
        )
    elif code == 17:
        print(
            "FAIL: 当前调用额度已用尽（错误码 17）"
        )
    elif code is not None:
        print("REVIEW: 百度返回错误码", code)
    elif isinstance(result.get("words_result"), dict):
        print(
            "PASS: 百度接口返回了识别结构；"
            "请核对是否使用了预期应用"
        )
    else:
        print("REVIEW: 百度返回的结构与预期不同")


if __name__ == "__main__":
    asyncio.run(main())