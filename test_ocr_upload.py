import asyncio
import os
from unittest.mock import AsyncMock, patch

import httpx
from dotenv import load_dotenv

load_dotenv(".env", override=True)

from app.business import business
from app.main import app
from app.models import Principal


FRONT_RESULT = {
    "words_result": {
        "号牌号码": {"words": "粤B67890"},
        "车辆识别代号": {"words": "LSV12345678901234"},
        "所有人": {"words": "测试用户"},
        "车辆类型": {"words": "小型汽车"},
        "使用性质": {"words": "非营运"},
    }
}

BACK_RESULT = {
    "words_result": {
        "号牌号码": {"words": "粤B67890"},
        "核定载人数": {"words": "5人"},
    }
}


async def main():
    token = os.getenv("SERVICE_TOKEN")
    if not token:
        raise RuntimeError("未读取到 .env 中的 SERVICE_TOKEN")

    principal = Principal(
        user_id="ocr-upload-test",
        salesperson_id="sales-1",
        role="user",
    )
    headers = {
        "Authorization": f"Bearer {token}",
        "X-User-Id": principal.user_id,
        "X-Salesperson-Id": principal.salesperson_id,
    }

    business.records.clear()
    business.results.clear()

    with patch.dict(os.environ, {"MODE": "demo", "OCR_MODE": "baidu"}):
        conversation = await business.call(
            "open", principal, None, {"plate": "粤B67890"}
        )
        cid = conversation["id"]

        transport = httpx.ASGITransport(app=app)
        async with httpx.AsyncClient(
            transport=transport, base_url="http://test"
        ) as client:
            for kind, raw_result, side in (
                ("license_front", FRONT_RESULT, "front"),
                ("license_back", BACK_RESULT, "back"),
            ):
                with patch(
                    "app.ocr_baidu.recognize_vehicle_license",
                    new_callable=AsyncMock,
                    return_value=raw_result,
                ) as mock_ocr:
                    response = await client.post(
                        f"/ai/conversations/{cid}/materials/{kind}",
                        headers=headers,
                        files={
                            "file": (
                                "test.jpg",
                                b"fake-image-for-test",
                                "image/jpeg",
                            )
                        },
                    )

                    assert response.status_code == 200, response.text
                    mock_ocr.assert_awaited_once_with(
                        b"fake-image-for-test", side
                    )

                pending = response.json()["pending"]
                assert pending["kind"] == kind
                assert pending["value"]["plate_number"] == "粤B67890"

                confirmation = await client.post(
                    f"/ai/conversations/{cid}/confirm",
                    headers=headers,
                    json={"token": pending["token"]},
                )
                assert confirmation.status_code == 200, confirmation.text
                assert kind in confirmation.json()["materials"]

                print(f"PASS: {kind} 上传、识别、确认")

    print("PASS: 行驶证正副页上传流程")


asyncio.run(main())