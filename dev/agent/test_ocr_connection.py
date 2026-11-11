import asyncio
import os
from unittest.mock import AsyncMock, patch

from app.providers import recognize

fake_baidu_result = {
    "words_result": {
        "号牌号码": {"words": "粤B67890"},
        "车辆识别代号": {"words": "LSV12345678901234"},
        "所有人": {"words": "测试用户"},
        "使用性质": {"words": "非营运"},
    }
}

async def main():
    with patch.dict(os.environ, {"MODE": "demo", "OCR_MODE": "baidu"}):
        with patch(
            "app.ocr_baidu.recognize_vehicle_license",
            new_callable=AsyncMock,
            return_value=fake_baidu_result,
        ) as mock_ocr:
            result = await recognize(
                "license_front",
                b"fake-image-for-test",
                "image/jpeg",
            )

            mock_ocr.assert_awaited_once_with(
                b"fake-image-for-test",
                "front",
            )
            assert result["fields"]["plate_number"] == "粤B67890"
            assert result["needs_review"] is False

    print("PASS: providers.py 已调用百度 OCR 函数并转换结果")

asyncio.run(main())