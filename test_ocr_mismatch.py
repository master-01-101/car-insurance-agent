import asyncio
import os
from unittest.mock import AsyncMock, patch

import httpx
from dotenv import load_dotenv

load_dotenv()
os.environ['MODE'] = 'demo'
os.environ['SERVICE_TOKEN'] = 'local-ocr-mismatch-test'

from app.business import business
from app.main import app
from app.models import Principal


async def main():
    principal = Principal(
        user_id='ocr-mismatch-test-user',
        salesperson_id='sales-1',
        role='user',
    )
    record = await business.call(
        'open', principal, None, {'plate': '粤B67890'}
    )
    cid = record['id']
    url = f'/ai/conversations/{cid}/materials'
    headers = {
        'Authorization': 'Bearer local-ocr-mismatch-test',
        'X-User-Id': principal.user_id,
        'X-Salesperson-Id': principal.salesperson_id,
    }

    transport = httpx.ASGITransport(app=app)
    async with httpx.AsyncClient(
        transport=transport, base_url='http://testserver'
    ) as client:
        with patch(
            'app.main.recognize',
            new=AsyncMock(return_value={
                'fields': {'plate_number': '粤B67890'},
                'needs_review': False,
            }),
        ):
            front = await client.post(
                f'{url}/license_front',
                headers=headers,
                files={'file': ('front.png', b'front-test-image', 'image/png')},
            )
        assert front.status_code == 200, front.text

        token = front.json()['pending']['token']
        confirmed = await client.post(
            f'/ai/conversations/{cid}/confirm',
            headers=headers,
            json={'token': token},
        )
        assert confirmed.status_code == 200, confirmed.text

        with patch(
            'app.main.recognize',
            new=AsyncMock(return_value={
                'fields': {'plate_number': '京A12345'},
                'needs_review': False,
            }),
        ):
            back = await client.post(
                f'{url}/license_back',
                headers=headers,
                files={'file': ('back.png', b'back-test-image', 'image/png')},
            )

        assert back.status_code == 409, back.text
        assert '车牌' in back.json()['detail'], back.text

        state = await client.get(
            f'/ai/conversations/{cid}',
            headers=headers,
        )
        assert state.status_code == 200, state.text
        assert 'license_back' not in state.json()['materials']

    print('PASS: 不同车辆的行驶证副页被 409 拦截，未进入待确认材料')


if __name__ == '__main__':
    asyncio.run(main())