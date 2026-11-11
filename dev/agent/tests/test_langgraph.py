"""验证真实 LangGraph 执行路径；模型使用模拟输出，无需 API 密钥。"""
import asyncio
import os
import unittest
from unittest.mock import AsyncMock, patch

from fastapi import HTTPException
from app import agent_graph as graph_module
from app.business import business
from app.models import Intent, Principal


class GraphRoutingTests(unittest.IsolatedAsyncioTestCase):
    async def asyncSetUp(self):
        self.env = patch.dict(os.environ, {'MODE': 'demo'})
        self.env.start()
        self.addCleanup(self.env.stop)
        business.records.clear()
        business.results.clear()
        self.principal = Principal(user_id='graph-user', salesperson_id='sales-1')
        self.record = await business.call('open', self.principal, None, {'plate': '粤B12345'})
        self.graph = graph_module.build_agent_graph()

    def state(self, **changes):
        return {'event': 'chat', 'principal': self.principal,
                'conversation_id': self.record['id'], 'message': '推荐方案',
                'history': [], **changes}

    async def run_nodes(self, state):
        nodes = []
        async for update in self.graph.astream(state, stream_mode='updates'):
            nodes.extend(update.keys())
        return nodes

    async def test_recommendation_uses_separate_prepare_stage_reply_nodes(self):
        with patch.object(graph_module, 'understand', AsyncMock(return_value=Intent(action='recommend'))):
            nodes = await self.run_nodes(self.state())
        self.assertEqual(nodes, ['load_record', 'understand_intent', 'prepare_plan',
                                 'stage_plan', 'describe_plan'])
        self.assertIsNotNone(business.records[self.record['id']]['pending'])

    async def test_clarify_never_enters_mutation_nodes(self):
        intent = Intent(action='clarify', question='请问具体修改哪个险种？')
        with patch.object(graph_module, 'understand', AsyncMock(return_value=intent)):
            nodes = await self.run_nodes(self.state(message='那个'))
        self.assertEqual(nodes, ['load_record', 'understand_intent', 'ask_clarification'])
        self.assertIsNone(business.records[self.record['id']]['pending'])

    async def test_confirmation_uses_token_branch_without_model(self):
        staged = await business.call('stage', self.principal, self.record['id'],
                                    {'kind': 'plan', 'value': {'third_party_liability': 3000000}})
        with patch.object(graph_module, 'understand', AsyncMock()) as model:
            nodes = await self.run_nodes(self.state(event='confirm', confirmation_token=staged['pending']['token']))
        model.assert_not_awaited()
        self.assertEqual(nodes, ['load_record', 'confirm_pending'])
        self.assertEqual(business.records[self.record['id']]['plan']['third_party_liability'], 3000000)

    async def test_human_branch_bypasses_model(self):
        await business.call('handoff', self.principal, self.record['id'], {'reason': 'test'})
        with patch.object(graph_module, 'understand', AsyncMock()) as model:
            nodes = await self.run_nodes(self.state())
        model.assert_not_awaited()
        self.assertEqual(nodes, ['load_record', 'human_paused'])

    async def test_quote_routes_to_underwriting(self):
        record = business.records[self.record['id']]
        record['state'] = 'ready'
        record['plan'] = {'third_party_liability': 3000000}
        record['materials'] = {kind: {} for kind in ('license_front', 'license_back', 'id_front', 'id_back')}
        with patch.object(graph_module, 'understand', AsyncMock(return_value=Intent(action='quote'))):
            nodes = await self.run_nodes(self.state(message='报价'))
        self.assertEqual(nodes, ['load_record', 'understand_intent', 'execute_business',
                                 'submit_underwriting', 'describe_business'])
        self.assertEqual(record['state'], 'approved')

    async def test_failed_quote_does_not_submit_underwriting(self):
        with patch.object(graph_module, 'understand', AsyncMock(return_value=Intent(action='quote'))):
            with patch.object(business, 'call', wraps=business.call) as calls:
                with self.assertRaises(HTTPException) as caught:
                    await self.graph.ainvoke(self.state(message='报价'))
        self.assertEqual(caught.exception.status_code, 409)
        self.assertNotIn('underwriting', [call.args[0] for call in calls.await_args_list])

    async def test_parallel_invocations_do_not_share_state(self):
        other = Principal(user_id='other', salesperson_id='sales-1')
        other_record = await business.call('open', other, None, {'plate': '粤B23456'})
        with patch.object(graph_module, 'understand', AsyncMock(return_value=Intent(action='recommend'))):
            first, second = await asyncio.gather(
                self.graph.ainvoke(self.state()),
                self.graph.ainvoke(self.state(principal=other, conversation_id=other_record['id'])),
            )
        self.assertEqual(first['record']['owner'], 'graph-user')
        self.assertEqual(second['record']['owner'], 'other')
        self.assertNotEqual(first['record']['pending']['token'], second['record']['pending']['token'])

if __name__ == '__main__':
    unittest.main(verbosity=2)
