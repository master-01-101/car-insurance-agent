"""内部身份证 OCR 接口测试：使用虚构数据，不调用真实百度。"""

import asyncio
import io
import os
from datetime import datetime, timedelta, timezone
from unittest.mock import AsyncMock, patch

import httpx
from PIL import Image

from app.main import app


def make_image(fmt="PNG"):
    output = io.BytesIO()
    Image.new("RGB", (8, 8), "white").save(output, format=fmt)
    return output.getvalue()


def make_request(
    side,
    image,
    mime_type="image/png",
    url="https://files.example.test/sample",
):
    return {
        "requestId": "req-1",
        "conversationId": "conversation-1",
        "materialId": "material-1",
        "side": side,
        "readUrl": url,
        "expiresAt": (
            datetime.now(timezone.utc) + timedelta(minutes=5)
        ).isoformat(),
        "mimeType": mime_type,
        "sizeBytes": len(image),
    }


async def main():
    headers = {
        "Authorization": "Bearer test-service-token"
    }

    front_image = make_image()
    back_image = make_image("WEBP")

    transport = httpx.ASGITransport(app=app)

    with patch.dict(
        os.environ,
        {
            "SERVICE_TOKEN": "test-service-token",
            "OCR_READ_HOSTS": "files.example.test",
            "OCR_MODE": "baidu",
        },
    ):
        async with httpx.AsyncClient(
            transport=transport,
            base_url="http://testserver",
        ) as client:

            with patch(
                "app.ocr_api.fetch_image",
                new=AsyncMock(return_value=front_image),
            ):
                with patch(
                    "app.ocr_api.recognize",
                    new=AsyncMock(
                        return_value={
                            "fields": {
                                "name": "测试甲",
                                "id_number": "TEST0001",
                            },
                            "needs_review": False,
                            "provider": "baidu",
                        }
                    ),
                ) as fake_ocr:
                    response = await client.post(
                        "/internal/ai/ocr/id-card",
                        headers=headers,
                        json=make_request("front", front_image),
                    )

                    assert response.status_code == 200, response.text
                    body = response.json()

                    assert body["status"] == "succeeded"
                    assert not body["handoffRequired"]

                    fake_ocr.assert_awaited_once_with(
                        "id_front",
                        front_image,
                        "image/png",
                    )
                    print("PASS: 身份证正面成功识别")

            with patch(
                "app.ocr_api.fetch_image",
                new=AsyncMock(return_value=back_image),
            ):
                with patch(
                    "app.ocr_api.recognize",
                    new=AsyncMock(
                        return_value={
                            "fields": {
                                "issuing_authority": "测试机关",
                                "valid_to": "20300101",
                            },
                            "needs_review": False,
                            "provider": "baidu",
                        }
                    ),
                ) as fake_ocr:
                    response = await client.post(
                        "/internal/ai/ocr/id-card",
                        headers=headers,
                        json=make_request(
                            "id_back",
                            back_image,
                            "image/webp",
                        ),
                    )

                    assert response.status_code == 200, response.text
                    assert response.json()["status"] == "succeeded"

                    fake_ocr.assert_awaited_once()
                    assert fake_ocr.await_args.args[0] == "id_back"
                    assert fake_ocr.await_args.args[2] == "image/png"

                    print("PASS: 身份证反面 WebP 转换后识别")

            with patch(
                "app.ocr_api.fetch_image",
                new=AsyncMock(return_value=front_image),
            ):
                with patch(
                    "app.ocr_api.recognize",
                    new=AsyncMock(
                        return_value={
                            "fields": {"name": "测试甲"},
                            "needs_review": True,
                            "missing_fields": ["id_number"],
                            "warnings": [
                                "idcard_number_check_failed"
                            ],
                        }
                    ),
                ):
                    response = await client.post(
                        "/internal/ai/ocr/id-card",
                        headers=headers,
                        json=make_request("front", front_image),
                    )

                    body = response.json()

                    assert body["status"] == "needs_review"
                    assert body["handoffRequired"]
                    assert body["missingFields"] == ["id_number"]
                    assert body["errorCode"] == "OCR_REVIEW_REQUIRED"

                    print("PASS: 字段缺失要求核对与转人工")

            blocked = await client.post(
                "/internal/ai/ocr/id-card",
                headers=headers,
                json=make_request(
                    "front",
                    front_image,
                    url="https://evil.example.test/secret",
                ),
            )
            assert blocked.status_code == 422, blocked.text

            unauthorized = await client.post(
                "/internal/ai/ocr/id-card",
                json=make_request("front", front_image),
            )
            assert unauthorized.status_code == 422, unauthorized.text

            print("PASS: 非允许域名和未认证请求被拒绝")


if __name__ == "__main__":
    asyncio.run(main())