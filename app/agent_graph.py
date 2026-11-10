"""车险智能体的 LangGraph 执行图。

每次请求带入最近对话；图内状态只在本次执行中流转。
跨请求记忆仍由 FastAPI 层维护，业务状态每次从业务接口重新读取。
"""
from typing import Literal, TypedDict

from fastapi import HTTPException
from langgraph.graph import END, START, StateGraph

from .business import business
from .knowledge import search
from .models import Intent, Principal
from .providers import understand


class AgentState(TypedDict, total=False):
    event: Literal['chat', 'confirm']
    principal: Principal
    conversation_id: str
    message: str
    confirmation_token: str
    history: list[dict[str, str]]
    record: dict
    intent: Intent
    proposed_plan: dict
    reply: str
    clear_history: bool
    skip_history: bool


def model_context(record, history):
    fields = ('insurer', 'third_party_liability', 'vehicle_damage', 'compulsory')

    def select(plan):
        return {key: plan[key] for key in fields if key in plan} if plan else None

    pending = record.get('pending') or {}
    current = pending.get('value') if pending.get('kind') == 'plan' else record.get('plan')
    return {
        'state': record['state'],
        'current_plan': select(current),
        'confirmed_plan': select(record.get('plan')),
        'pending_kind': pending.get('kind'),
        'material_types': list(record.get('materials', {})),
        'history': history,
    }


def plan_summary(plan):
    parts = []
    if plan.get('insurer'):
        parts.append('保险公司为' + str(plan['insurer']))
    if plan.get('third_party_liability') is not None:
        parts.append(f"三者险保额为 {plan['third_party_liability'] / 10000:g} 万元")
    if plan.get('vehicle_damage') is not None:
        parts.append('车损险' + ('保留' if plan['vehicle_damage'] else '不投保'))
    if plan.get('compulsory') is not None:
        parts.append('交强险' + ('包含' if plan['compulsory'] else '不包含'))
    return '，'.join(parts)


async def load_record(state: AgentState):
    record = await business.call('get', state['principal'], state['conversation_id'])
    return {'record': record}


def route_entry(state: AgentState):
    # 确认由独立入口和确认令牌驱动，不经过语言模型决策。
    if state['event'] == 'confirm':
        return 'confirm_pending'
    if state['record']['state'] == 'human':
        return 'human_paused'
    return 'understand_intent'


def human_paused(state: AgentState):
    return {'reply': '当前会话已由人工接管。', 'skip_history': True}


async def understand_intent(state: AgentState):
    intent = await understand(state['message'], context=model_context(state['record'], state['history']))
    return {'intent': intent}


def route_intent(state: AgentState):
    return {
        'clarify': 'ask_clarification',
        'modify': 'prepare_plan',
        'recommend': 'prepare_plan',
        'history': 'prepare_plan',
        'confirm': 'request_confirmation',
        'help': 'answer_help',
        'status': 'describe_business',
        'quote': 'execute_business',
        'underwriting': 'execute_business',
        'payment': 'execute_business',
        'policy': 'execute_business',
        'cancel': 'execute_business',
        'handoff': 'execute_business',
    }[state['intent'].action]


def ask_clarification(state: AgentState):
    return {'reply': state['intent'].question}


async def prepare_plan(state: AgentState):
    record = state['record']
    intent = state['intent']
    pending = record.get('pending')
    if pending and pending['kind'] != 'plan':
        raise HTTPException(409, '请先确认或取消待确认材料，再修改方案')
    if intent.action != 'modify':
        plan = await business.call(intent.action, state['principal'], state['conversation_id'])
        return {'proposed_plan': plan}
    plan = dict(record.get('plan') or {})
    if pending:
        plan = dict(pending['value'])
    if not plan:
        raise HTTPException(409, '请先获取方案')
    changes = intent.model_dump(exclude_none=True)
    for field, target in [('amount', 'third_party_liability'),
                          ('vehicle_damage', 'vehicle_damage'), ('insurer', 'insurer')]:
        if field in changes:
            plan[target] = changes[field]
    plan.pop('estimated_price', None)
    plan['price_source'] = '修改后待重新报价'
    return {'proposed_plan': plan}


async def stage_plan(state: AgentState):
    record = await business.call('stage', state['principal'], state['conversation_id'],
                                 {'kind': 'plan', 'value': state['proposed_plan']})
    return {'record': record}


def describe_plan(state: AgentState):
    # 展示业务接口返回的实际待确认方案。
    plan = state['record']['pending']['value']
    reply = '待确认方案：' + plan_summary(plan) + '。请核对后确认。'
    if state['intent'].action == 'modify':
        reply += '保费需重新报价，当前尚未保存为已确认方案。'
    return {'reply': reply}


def request_confirmation(state: AgentState):
    return {'reply': '请在界面核对当前待确认内容，通过 /confirm 接口提交对应 token。'}


async def confirm_pending(state: AgentState):
    token = state['confirmation_token']
    record = await business.call('confirm', state['principal'], state['conversation_id'],
                                 {'token': token}, key=token)
    return {'record': record}


async def execute_business(state: AgentState):
    action = state['intent'].action
    record = state['record']
    cid = state['conversation_id']
    payload = {}
    if action == 'handoff':
        payload = {'reason': state['message'],
                   'summary': {'state': record['state'], 'plate': record['plate']}}
    updated = await business.call(action, state['principal'], cid, payload,
                                   f'{cid}:{record["version"]}:{action}')
    return {'record': updated, 'clear_history': action in ('cancel', 'handoff')}


def route_after_business(state: AgentState):
    return 'submit_underwriting' if state['intent'].action == 'quote' else 'describe_business'


async def submit_underwriting(state: AgentState):
    cid = state['conversation_id']
    record = await business.call('underwriting', state['principal'], cid,
                                 key=f'{cid}:underwrite:{state["record"]["version"]}')
    return {'record': record}


def describe_business(state: AgentState):
    descriptions = {
        'collecting': '正在收集资料，请补齐材料并确认投保方案。',
        'ready': '当前资料或方案已确认，报价前还需检查材料是否齐全。',
        'quoted': '报价已返回，等待核保结果。',
        'supplement': '核保需要补充验车材料，请按 required_materials 上传后再次提交核保。',
        'approved': '核保已通过，可以发起支付。',
        'paying': '支付单已创建，等待支付平台确认结果。',
        'paid': '业务系统已确认支付成功，可以查询保单。',
        'issued': '业务系统已确认出单，请查看保单明细。',
        'human': '已提交人工处理，当前会话的自动办理已暂停。',
    }
    reply = descriptions.get(state['record']['state'], '业务系统已返回结果，请查看当前状态。')
    if state['intent'].action == 'cancel':
        reply = '已取消待确认操作。'
    return {'reply': reply}


def answer_help(state: AgentState):
    terms = search(state['message'])
    reply = '请上传行驶证，或输入“查询去年方案”“推荐方案”。'
    if terms:
        reply = '\n'.join(item['term'] + '：' + item['definition'] for item in terms)
    return {'reply': reply}


def build_agent_graph():
    graph = StateGraph(AgentState)
    for node in (load_record, human_paused, understand_intent, ask_clarification,
                 prepare_plan, stage_plan, describe_plan, request_confirmation,
                 confirm_pending, execute_business, submit_underwriting, describe_business, answer_help):
        graph.add_node(node.__name__, node)
    graph.add_edge(START, 'load_record')
    graph.add_conditional_edges('load_record', route_entry,
                               {name: name for name in ('confirm_pending', 'human_paused', 'understand_intent')})
    graph.add_conditional_edges('understand_intent', route_intent,
        {name: name for name in ('ask_clarification', 'prepare_plan', 'request_confirmation',
                                'answer_help', 'describe_business', 'execute_business')})
    graph.add_edge('prepare_plan', 'stage_plan')
    graph.add_edge('stage_plan', 'describe_plan')
    graph.add_conditional_edges('execute_business', route_after_business,
        {name: name for name in ('submit_underwriting', 'describe_business')})
    graph.add_edge('submit_underwriting', 'describe_business')
    for name in ('human_paused', 'ask_clarification', 'describe_plan',
                 'request_confirmation', 'confirm_pending', 'describe_business', 'answer_help'):
        graph.add_edge(name, END)
    return graph.compile()


agent_graph = build_agent_graph()
