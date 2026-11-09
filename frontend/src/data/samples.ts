/**
 * 静态占位数据（Static layout samples）
 * ---------------------------------------------------------------------------
 * ⚠️ 仅用于渲染布局、确认间距与视觉层级，**不是业务数据**：
 *   - 不是真实保单、真实报价或真实客户信息；
 *   - 不参与任何计算与判断；
 *   - 所有引用处都会同时显示「静态占位」标记。
 * 对应接口导入后，直接删除本文件与引用即可。
 */

import type {
  ChatMessage,
  Conversation,
  HandoffTicket,
  InsurancePlan,
  PolicyDetail,
  VehicleInfo,
  WorkbenchRow,
} from '@/types'

export const SAMPLE_CONVERSATIONS: Conversation[] = [
  {
    conversationId: 'conv-sample-1',
    plateNumber: '粤B·8K2M7',
    currentStage: 'recommend',
    lastMessagePreview: '推荐方案已生成，可继续说「三者调到 200 万」',
    updatedAt: '2026-11-09T10:12:00+08:00',
  },
  {
    conversationId: 'conv-sample-2',
    plateNumber: '沪A·3F7T2',
    currentStage: 'quote',
    lastMessagePreview: '待确认关键信息后报价',
    updatedAt: '2026-11-08T18:40:00+08:00',
  },
  {
    conversationId: 'conv-sample-3',
    plateNumber: '京N·6Q1L9',
    currentStage: 'policy',
    lastMessagePreview: '保单已生成',
    updatedAt: '2026-11-07T09:05:00+08:00',
  },
]

export const SAMPLE_PLAN: InsurancePlan = {
  planId: 'plan-sample-1',
  conversationId: 'conv-sample-1',
  version: 'v1',
  insuranceCompany: '中国平安（示例）',
  totalPremium: 3268,
  confirmed: false,
  createdAt: '2026-11-09T10:12:00+08:00',
  items: [
    {
      itemId: 'item-1',
      coverageName: '交强险',
      coverageAmount: 122000,
      premium: 950,
      required: true,
    },
    {
      itemId: 'item-2',
      coverageName: '第三者责任险',
      coverageAmount: 2000000,
      premium: 1280,
      required: false,
    },
    {
      itemId: 'item-3',
      coverageName: '车损险',
      coverageAmount: 186000,
      premium: 1038,
      required: false,
    },
  ],
}

export const SAMPLE_MESSAGES: ChatMessage[] = [
  {
    conversationId: 'conv-sample-1',
    messageId: 'msg-sample-1',
    role: 'assistant',
    messageType: 'text',
    content:
      '您好，我是车险出单助手。本次会话围绕车牌 粤B·8K2M7 办理。\n请上传行驶证正、副页（自动 OCR 识别）；若是老用户，也可以直接提供车牌号或车架号，我按去年信息续保。',
    createdAt: '2026-11-09T10:02:00+08:00',
  },
  {
    conversationId: 'conv-sample-1',
    messageId: 'msg-sample-2',
    role: 'user',
    messageType: 'text',
    content: '老用户，车牌 粤B·8K2M7，先看方案。',
    createdAt: '2026-11-09T10:03:00+08:00',
  },
  {
    conversationId: 'conv-sample-1',
    messageId: 'msg-sample-3',
    role: 'assistant',
    messageType: 'status',
    content: '已匹配到该车牌去年的投保记录，跳过重复信息收集，直接进入方案推荐。',
    createdAt: '2026-11-09T10:04:00+08:00',
    payload: {
      statusCode: 'vehicle-matched',
      statusLabel: '已匹配历史投保记录',
      tone: 'success',
      detail: '数据来源：续保查询接口（未导入），此处为静态占位。',
    },
  },
  {
    conversationId: 'conv-sample-1',
    messageId: 'msg-sample-4',
    role: 'assistant',
    messageType: 'plan',
    content: '根据去年的方案，推荐如下组合。可以直接说「平安、交强险、三者调到 200 万」来修改。',
    createdAt: '2026-11-09T10:12:00+08:00',
    payload: {
      planId: SAMPLE_PLAN.planId,
      version: SAMPLE_PLAN.version,
      insuranceCompany: SAMPLE_PLAN.insuranceCompany,
      totalPremium: SAMPLE_PLAN.totalPremium,
      items: SAMPLE_PLAN.items,
    },
  },
  {
    conversationId: 'conv-sample-1',
    messageId: 'msg-sample-5',
    role: 'assistant',
    messageType: 'handoff',
    content:
      'OCR 识别的车主姓名与「交强险」投保人不一致，已按规则发起转人工核对。人工坐席接入前，对话会保持只读。',
    createdAt: '2026-11-09T10:14:00+08:00',
  },
]

/** 车辆资料卡片字段（除车牌外均为待回填状态） */
export const SAMPLE_VEHICLE: VehicleInfo = {
  plateNumber: '粤B·8K2M7',
  vin: undefined,
  engineNo: undefined,
  ownerName: undefined,
  registerDate: undefined,
  usageNature: undefined,
  model: undefined,
}

/** 行驶证 OCR 待确认字段 */
export const OCR_FIELDS: Array<{ key: keyof VehicleInfo; label: string }> = [
  { key: 'plateNumber', label: '号牌号码' },
  { key: 'vin', label: '车辆识别代号' },
  { key: 'engineNo', label: '发动机号码' },
  { key: 'ownerName', label: '所有人' },
  { key: 'registerDate', label: '注册日期' },
  { key: 'usageNature', label: '使用性质' },
]

export const SAMPLE_WORKBENCH_ROWS: WorkbenchRow[] = [
  {
    orderId: 'ORD-20261109-001',
    customerName: '张*',
    plateNumber: '粤B·8K2M7',
    status: 'recommending',
    premium: null,
    updatedAt: '2026-11-09T10:12:00+08:00',
  },
  {
    orderId: 'ORD-20261109-002',
    customerName: '李*',
    plateNumber: '沪A·3F7T2',
    status: 'quoting',
    premium: null,
    updatedAt: '2026-11-09T09:48:00+08:00',
  },
  {
    orderId: 'ORD-20261108-014',
    customerName: '王*',
    plateNumber: '京N·6Q1L9',
    status: 'issued',
    premium: 4312,
    updatedAt: '2026-11-08T17:20:00+08:00',
  },
  {
    orderId: 'ORD-20261108-009',
    customerName: '赵*',
    plateNumber: '粤B·2H8K4',
    status: 'underwriting',
    premium: null,
    updatedAt: '2026-11-08T15:02:00+08:00',
  },
  {
    orderId: 'ORD-20261107-021',
    customerName: '陈*',
    plateNumber: '沪C·9M3D1',
    status: 'failed',
    premium: null,
    updatedAt: '2026-11-07T11:35:00+08:00',
  },
]

export const SAMPLE_POLICY: PolicyDetail = {
  policyId: 'policy-sample-1',
  policyNo: 'PICC-2026-0000-0000（示例）',
  applicant: { name: '张*', idType: '居民身份证', idMasked: '4403**********8821' },
  insured: { name: '张*', idType: '居民身份证', idMasked: '4403**********8821' },
  vehicle: {
    plateNumber: '粤B·8K2M7',
    vin: 'LSV**********1234',
    engineNo: 'EA***21',
    ownerName: '张*',
    registerDate: '2021-06-18',
    usageNature: '非营运',
    model: '示例车型',
  },
  items: [
    {
      itemId: 'item-1',
      coverageName: '交强险',
      coverageAmount: 122000,
      premium: 950,
      required: true,
    },
    {
      itemId: 'item-2',
      coverageName: '第三者责任险',
      coverageAmount: 2000000,
      premium: 1280,
      required: false,
    },
    {
      itemId: 'item-3',
      coverageName: '车损险',
      coverageAmount: 186000,
      premium: 1038,
      required: false,
    },
  ],
  totalPremium: 3268,
  effectiveFrom: '2026-11-10',
  effectiveTo: '2027-11-09',
  terms: [
    '保单生效后 24 小时内可申请批改，批改需重新核保。',
    '出险后请于 48 小时内报案，并保留现场影像。',
    '条款全文以保险公司正式保单为准（条款接口未导入）。',
  ],
}

export const SAMPLE_HANDOFF_TICKET: HandoffTicket = {
  ticketId: 'HO-20261109-0007',
  conversationId: 'conv-sample-1',
  status: 'queued',
  reason: 'OCR 车主姓名与投保人不一致',
  queuePosition: 2,
  assignedAgentName: undefined,
  createdAt: '2026-11-09T10:14:00+08:00',
}
