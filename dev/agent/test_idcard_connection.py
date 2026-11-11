"""验证身份证 OCR 已接入上传流程；不调用真实百度。"""

import asyncio
import os
from io import BytesIO
from unittest.mock import AsyncMock, patch

from starlette.datastructures import UploadFile

from app import main
from app.models import Principal
from app.providers import recognize


async def run():
    principal = Principal(
        user_id="test-user",
        salesperson_id="sales-1",
        role="user",
    )

    record = {
        "id": "test-conversation",
        "plate": "粤B68003",
        "state": "collecting",
        "materials": {},
        "pending": None,
    }

    async def fake_business_call(
        operation,
        p,
        cid,
        payload=None,
        **kwargs,
    ):
        if operation == "get":
            return record

        if operation == "stage":
            assert payload["kind"] in ("id_front", "id_back")
            return {
                **record,
                "pending": {
                    "token": "test-token",
                    "kind": payload["kind"],
                    "value": payload["value"],
                },
            }

        raise AssertionError(f"意外的业务调用: {operation}")

    with patch.dict(
        os.environ,
        {"MODE": "demo", "OCR_MODE": "baidu"},
    ):
        with patch.object(
            main.business,
            "call",
            new=AsyncMock(side_effect=fake_business_call),
        ):
            for kind in ("id_front", "id_back"):
                fields = (
                    {"name": "测试甲", "id_number": "TEST0001"}
                    if kind == "id_front"
                    else {
                        "issuing_authority": "测试机关",
                        "valid_to": "20300101",
                    }
                )

                with patch(
                    "app.ocr_idcard.recognize_id_card",
                    new=AsyncMock(
                        return_value={
                            "fields": fields,
                            "needs_review": False,
                        }
                    ),
                ) as fake_ocr:
                    result = await main.upload(
                        "test-conversation",
                        kind,
                        UploadFile(
                            file=BytesIO(b"synthetic-image"),
                            filename="test.png",
                            headers={"content-type": "image/png"},
                        ),
                        principal,
                    )

                    fake_ocr.assert_awaited_once_with(
                        kind,
                        b"synthetic-image",
                        "image/png",
                    )
                    assert result["pending"]["kind"] == kind
                    print(
                        f"PASS: {kind} 上传 → 身份证 OCR → 待确认资料"
                    )

        with patch.dict(os.environ, {"OCR_MODE": "demo"}):
            demo = await recognize(
                "id_front",
                b"synthetic-image",
                "image/png",
            )
            assert demo["fields"]["demo_document"] == "id_front"
            print("PASS: OCR_MODE=demo 仍保留模拟识别")


if __name__ == "__main__":
    asyncio.run(run())