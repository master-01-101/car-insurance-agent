"""本地回归测试，用模拟模型响应验证接口行为，不消耗模型额度。"""
import json
import os
import unittest
from copy import deepcopy
from unittest.mock import patch

import httpx
from fastapi.testclient import TestClient
from app import main


class MultiTurnTests(unittest.TestCase):
    def setUp(self):
        self.env = patch.dict(os.environ, {
            'MODE': 'demo', 'MODEL_KEY': 'test-model-key', 'MODEL_URL': 'https://model.test/v1',
            'MODEL_NAME': 'test', 'SERVICE_TOKEN': 'test-service', 'QDRANT_URL': '',
        })
        self.env.start()
        self.addCleanup(self.env.stop)
        main.business.records.clear()
        main.business.results.clear()
        main.conversation_memory.clear()
        main.locks.clear()
        self.outputs = []
        self.requests = []
        original_client = httpx.AsyncClient
        def factory(**kwargs):
            return original_client(transport=httpx.MockTransport(self.model_response), **kwargs)
        self.mock = patch('app.providers.httpx.AsyncClient', side_effect=factory)
        self.mock.start()
        self.addCleanup(self.mock.stop)
        self.client = TestClient(main.app)
        self.addCleanup(self.client.close)
        self.headers = {'Authorization': 'Bearer test-service', 'X-User-Id': 'one',
                        'X-Salesperson-Id': 's1'}

    def model_response(self, request):
        self.requests.append(json.loads(request.content))
        answer = self.outputs.pop(0)
        if answer == 'invalid':
            return httpx.Response(200, json={'choices': [{'message': {'content': 'hello'}}]})
        return httpx.Response(200, json={'choices': [{'message': {'tool_calls': [
            {'function': {'name': 'interpret', 'arguments': json.dumps(answer)}}]}}]})

    def chat(self, message, answer, cid=None, plate=None, headers=None):
        self.outputs.append(answer)
        response = self.client.post('/ai/chat', headers=headers or self.headers,
            json={'message': message, 'conversation_id': cid, 'plate': plate})
        return response

    def start(self, plate='粤B12345'):
        r = self.chat('推荐方案', {'action': 'recommend'}, plate=plate)
        self.assertEqual(r.status_code, 200, r.text)
        return r.json()['conversation']

    def context(self):
        return json.loads(self.requests[-1]['messages'][1]['content'].split('\n', 1)[1])['business']

    def test_followups_merge_pending_plan_and_confirm(self):
        record = self.start()
        cid = record['id']
        r = self.chat('三者改成两百万', {'action': 'modify', 'amount': 2000000}, cid)
        self.assertEqual(r.status_code, 200)
        r = self.chat('还是三百万吧', {'action': 'modify', 'amount': 3000000}, cid)
        self.assertEqual(self.context()['current_plan']['third_party_liability'], 2000000)
        self.assertEqual(self.context()['history'][-1]['user'], '三者改成两百万')
        r = self.chat('车损也不要了', {'action': 'modify', 'vehicle_damage': False}, cid)
        record = r.json()['conversation']
        plan = record['pending']['value']
        self.assertEqual(plan['third_party_liability'], 3000000)
        self.assertFalse(plan['vehicle_damage'])
        self.assertTrue(plan['compulsory'])
        self.assertNotIn('estimated_price', plan)
        self.assertIn('300 万元', r.json()['reply'])
        token = record['pending']['token']
        confirmed = self.client.post(f'/ai/conversations/{cid}/confirm', headers=self.headers,
                                     json={'token': token})
        self.assertEqual(confirmed.status_code, 200)
        self.assertEqual(confirmed.json()['plan'], plan)
        self.assertIsNone(confirmed.json()['pending'])

    def test_clarification_preserves_pending_and_is_available_on_reply(self):
        record = self.start()
        cid = record['id']
        pending = deepcopy(record['pending'])
        r = self.chat('那个不要了', {'action': 'clarify', 'question': '你要取消哪个险种？'}, cid)
        self.assertEqual(r.json()['conversation']['pending'], pending)
        r = self.chat('车损险', {'action': 'modify', 'vehicle_damage': False}, cid)
        self.assertEqual(self.context()['history'][-1]['assistant'], '你要取消哪个险种？')
        self.assertFalse(r.json()['conversation']['pending']['value']['vehicle_damage'])

    def test_users_and_vehicles_have_separate_context(self):
        one = self.start()
        self.chat('三者改200万', {'action': 'modify', 'amount': 2000000}, one['id'])
        self.start('粤B23456')
        self.assertEqual(self.context()['history'], [])
        other = {**self.headers, 'X-User-Id': 'two'}
        r = self.chat('推荐方案', {'action': 'recommend'}, plate='粤B12345', headers=other)
        self.assertEqual(r.status_code, 200)
        self.assertEqual(self.context()['history'], [])
        request_count = len(self.requests)
        r = self.client.post('/ai/chat', headers=other,
            json={'message': '修改', 'conversation_id': one['id']})
        self.assertEqual(r.status_code, 404)
        self.assertEqual(len(self.requests), request_count)

    def test_malformed_model_response_has_no_business_effect(self):
        record = self.start()
        pending = deepcopy(record['pending'])
        for answer in ('invalid', {'action': 'modify'}, {'action': 'clarify'}):
            r = self.chat('修改', answer, record['id'])
            self.assertEqual(r.status_code, 502)
            self.assertEqual(main.business.records[record['id']]['pending'], pending)
        self.assertEqual(len(next(iter(main.conversation_memory.values()))['turns']), 1)

    def test_context_excludes_ocr_and_confirmation_token(self):
        record = self.start()
        stored = main.business.records[record['id']]
        stored['materials']['id_front'] = {'id_number': 'secret-id'}
        stored['pending']['value']['private_field'] = 'private-plan'
        self.chat('状态', {'action': 'status'}, record['id'])
        text = json.dumps(self.context())
        self.assertNotIn('secret-id', text)
        self.assertNotIn('private-plan', text)
        self.assertNotIn(record['pending']['token'], text)
        self.assertEqual(self.context()['material_types'], ['id_front'])

    def test_model_does_not_overwrite_pending_material(self):
        record = self.start()
        stored = main.business.records[record['id']]
        stored['pending'] = {'kind': 'id_front', 'token': 'material', 'value': {'name': 'test'}}
        r = self.chat('推荐方案', {'action': 'recommend'}, record['id'])
        self.assertEqual(r.status_code, 409)
        self.assertEqual(stored['pending']['token'], 'material')

    def test_cancel_clears_previous_discussion(self):
        record = self.start()
        self.chat('取消修改', {'action': 'cancel'}, record['id'])
        self.chat('状态', {'action': 'status'}, record['id'])
        self.assertEqual(len(self.context()['history']), 1)
        self.assertEqual(self.context()['history'][0]['user'], '取消修改')

    def test_bounded_memory_and_expiry(self):
        with patch.object(main, 'MAX_MEMORY_SESSIONS', 2):
            for i in range(3):
                main.remember_turn(('u', 's', str(i)), [], 'hi', 'ok')
            self.assertEqual(len(main.conversation_memory), 2)
            self.assertNotIn(('u', 's', '0'), main.conversation_memory)
        key = ('u', 's', '1')
        for i in range(10):
            main.remember_turn(key, main.recent_turns(key), str(i), 'ok')
        self.assertEqual(len(main.recent_turns(key)), 6)
        main.conversation_memory[key]['updated_at'] -= 1801
        self.assertEqual(main.recent_turns(key), [])

    def test_old_confirm_token_is_rejected_after_modification(self):
        record = self.start()
        self.chat('改200万', {'action': 'modify', 'amount': 2000000}, record['id'])
        r = self.client.post(f'/ai/conversations/{record["id"]}/confirm', headers=self.headers,
                             json={'token': record['pending']['token']})
        self.assertEqual(r.status_code, 409)

    def test_handoff_prevents_further_model_calls(self):
        record = self.start()
        self.chat('人工', {'action': 'handoff'}, record['id'])
        count = len(self.requests)
        r = self.client.post('/ai/chat', headers=self.headers,
                            json={'message': '修改方案', 'conversation_id': record['id']})
        self.assertEqual(r.status_code, 200)
        self.assertEqual(len(self.requests), count)
        self.assertEqual(r.json()['conversation']['state'], 'human')

if __name__ == '__main__':
    unittest.main(verbosity=2)
