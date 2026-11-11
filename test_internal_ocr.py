import asyncio
import io
import os
from datetime import datetime, timedelta, timezone
from unittest.mock import AsyncMock, patch

os.environ['MODE'] = 'demo'
os.environ['SERVICE_TOKEN'] = 'internal-ocr-test-token'
os.environ['OCR_READ_HOSTS'] = 'files.example.test'

import httpx
from PIL import Image

from app.main import app
from app.ocr_api import prepare_image


image_buffer = io.BytesIO()
Image.new('RGB', (10, 10), 'white').save(image_buffer, 'PNG')
png_bytes = image_buffer.getvalue()


async def main():
    request_body = {
        'requestId': 'test-request-1',
        'conversationId': 'test-conversation-1',
        'materialId': 'test-material-1',
        'side': 'front',
        'readUrl': 'https://files.example.test/license.png',
        'expiresAt': (
            datetime.now(timezone.utc) + timedelta(minutes=5)
        ).isoformat(),
        'mimeType': 'image/png',
        'sizeBytes': len(png_bytes),
        'expectedPlateNumber': '粤B67890',
    }

    transport = httpx.ASGITransport(app=app)
    async with httpx.AsyncClient(
        transport=transport,
        base_url='http://testserver',
    ) as client:

        async def call_ocr(mock_result):
            with patch(
                'app.ocr_api.fetch_image',
                new=AsyncMock(return_value=png_bytes),
            ), patch(
                'app.ocr_api.recognize',
                new=AsyncMock(return_value=mock_result),
            ):
                return await client.post(
                    '/internal/ai/ocr/vehicle-license',
                    json=request_body,
                    headers={
                        'Authorization': 'Bearer internal-ocr-test-token'
                    },
                )

        normal = await call_ocr({
            'fields': {
                'plate_number': '粤B67890',
                'vin': 'TESTVIN123',
            },
            'needs_review': False,
        })
        assert normal.status_code == 200, normal.text
        assert normal.json()['status'] == 'succeeded'
        assert normal.json()['handoffRequired'] is False
        print('PASS: 正常识别')

        mismatch = await call_ocr({
            'fields': {
                'plate_number': '京A12345',
            },
            'needs_review': False,
        })
        assert mismatch.status_code == 200, mismatch.text
        assert mismatch.json()['errorCode'] == 'VEHICLE_PLATE_MISMATCH'
        assert mismatch.json()['detectedPlateNumber'] == '京A12345'
        assert mismatch.json()['handoffRequired'] is False
        print('PASS: 车牌冲突返回结构化结果')

        review = await call_ocr({
            'fields': {},
            'needs_review': False,
        })
        assert review.status_code == 200, review.text
        assert review.json()['errorCode'] == 'OCR_REVIEW_REQUIRED'
        assert review.json()['handoffRequired'] is True
        assert 'plate_number' in review.json()['missingFields']
        print('PASS: 缺少关键字段要求人工核对')

        request_body['readUrl'] = 'https://untrusted.example/test.png'
        rejected = await call_ocr({
            'fields': {'plate_number': '粤B67890'},
            'needs_review': False,
        })
        assert rejected.status_code == 422, rejected.text
        print('PASS: 拒绝非允许域名')

    webp_buffer = io.BytesIO()
    Image.new('RGB', (10, 10), 'white').save(
        webp_buffer,
        'WEBP',
    )
    converted, mime_type = prepare_image(
        webp_buffer.getvalue(),
        'image/webp',
    )
    assert mime_type == 'image/png'
    assert converted.startswith(b'\x89PNG')
    print('PASS: WebP 转换为 PNG')


if __name__ == '__main__':
    asyncio.run(main())