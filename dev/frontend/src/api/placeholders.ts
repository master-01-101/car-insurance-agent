/**
 * 未导入能力注册表（占位符唯一数据源）
 * ---------------------------------------------------------------------------
 * 本文件记录「前端 UI 已就位、后端 / Agent 能力尚未导入」的全部位置。
 * 任何页面需要展示后端或 Agent 数据时，都通过 <PlaceholderBlock id="..." />
 * 引用这里的条目，避免各处提示文案不一致。
 *
 * 路径均为团队建议值（见《前端.md》第四节），需与成员 B / 成员 C 联调确认。
 * 新增未导入能力时：在此追加条目，并在页面中引用，最后同步 AGENTS.md。
 */

export type PlaceholderOwner =
  | '成员B：后端 API'
  | '成员C：Agent / 智能体接口'
  | '成员B + 成员C'

export interface PlaceholderContent {
  /** 未导入能力的名称 */
  title: string
  /** 为什么这里留空、缺什么 */
  desc: string
  /** 预期接口（待确认） */
  endpoints: readonly string[]
  /** 由谁补齐 */
  owner: PlaceholderOwner
  /** 占位符出现的界面位置 */
  ui: string
}

const SPECS = {
  'auth-login': {
    title: '登录与身份鉴权未导入',
    desc: '没有真实的账号校验、token 下发和角色判定。当前登录按钮仅在本地写入演示身份，刷新后角色来自 localStorage，不代表服务端授权结果。',
    endpoints: ['POST /api/auth/login', 'GET /api/auth/profile'],
    owner: '成员B：后端 API',
    ui: '登录页 / 全局路由守卫',
  },
  'conversation-list': {
    title: '会话列表与新建会话接口未导入',
    desc: '会话按车牌号分组、刷新后重新加载、新建会话（第二个车牌号自动开启新会话）都依赖后端。左侧列表现为静态占位。',
    endpoints: ['GET /api/conversations', 'POST /api/conversations', 'PATCH /api/conversations/{id}'],
    owner: '成员B：后端 API',
    ui: '侧边栏会话列表 / 聊天页',
  },
  'chat-history': {
    title: '历史消息接口未导入',
    desc: '进入会话时无法拉取历史消息，也不会做分页与已读状态同步。下方消息均为静态占位，仅用于确认气泡、方案卡片与状态卡片的排版。',
    endpoints: ['GET /api/conversations/{id}/messages?cursor=&limit='],
    owner: '成员B：后端 API',
    ui: '聊天页消息区',
  },
  'chat-send': {
    title: '消息发送与 Agent 回复接口未导入',
    desc: '用户消息无法提交，Agent 也不会返回内容。输入框可输入但发送不会产生请求，仅在本地弹出提示。',
    endpoints: ['POST /api/conversations/{id}/messages'],
    owner: '成员C：Agent / 智能体接口',
    ui: '聊天页输入区',
  },
  'agent-stream': {
    title: '流式输出（SSE）未导入',
    desc: '当前只保留了普通 HTTP 的占位，打字机效果、增量渲染、中断重连都未实现。按《前端.md》要求先保证可用，再做流式。',
    endpoints: ['GET /api/conversations/{id}/stream (text/event-stream)'],
    owner: '成员B + 成员C',
    ui: '聊天页消息区',
  },
  'license-upload': {
    title: '行驶证影像上传服务未导入',
    desc: '没有对象存储或文件上传接口，正页 / 副页无法真正上传、预览服务端文件、删除或重传，进度条不会推进。',
    endpoints: ['POST /api/files (multipart)', 'DELETE /api/files/{fileId}'],
    owner: '成员B：后端 API',
    ui: '聊天页「车辆资料」区块',
  },
  'ocr-license': {
    title: '行驶证 OCR 识别未导入',
    desc: '正副页识别结果（号牌号码、车架号、发动机号等）不会被回填，字段异常标记与「确认 / 修正」逻辑无从触发。',
    endpoints: ['POST /api/ocr/license'],
    owner: '成员B + 成员C',
    ui: '聊天页「OCR 结果确认」',
  },
  'vehicle-query': {
    title: '老用户续保信息查询未导入',
    desc: '按车牌号或车架号查去年保单、直接复用去年方案并报价的能力依赖后端历史数据，当前无法查询。',
    endpoints: ['GET /api/vehicles/lookup?plateNumber=&vin='],
    owner: '成员B：后端 API',
    ui: '聊天页 / 车险入口',
  },
  'plan-recommend': {
    title: '智能推荐方案生成未导入',
    desc: '推荐方案（保险公司、险种、保额、价格）来自 Agent 推理，前端不做任何金额计算。卡片中的数字全部为静态占位。',
    endpoints: ['POST /api/plans/recommend'],
    owner: '成员C：Agent / 智能体接口',
    ui: '聊天页方案卡片',
  },
  'plan-revise': {
    title: '自然语言修改方案未导入',
    desc: '例如「平安、交强险、三者调到 200 万」这类指令需要 Agent 解析并返回新的方案版本，前端只负责展示后端确认后的版本号。',
    endpoints: ['POST /api/plans/{planId}/revise'],
    owner: '成员C：Agent / 智能体接口',
    ui: '聊天页方案卡片 / 输入区',
  },
  'idcard-upload': {
    title: '身份证影像上传与识别未导入',
    desc: '正反面上传、影像状态、关键信息回显均依赖上传与 OCR 接口；页面不展示完整证件号，只保留脱敏展示位。',
    endpoints: ['POST /api/files (multipart)', 'POST /api/ocr/idcard'],
    owner: '成员B + 成员C',
    ui: '聊天页「身份证影像」区块',
  },
  quote: {
    title: '报价接口未导入',
    desc: '无法调用保险公司报价，报价状态、明细、有效期均为空。前端不会自行判断报价是否成功。',
    endpoints: ['POST /api/quotes', 'GET /api/quotes/{quoteId}'],
    owner: '成员B：后端 API',
    ui: '聊天页报价区块 / 订单页',
  },
  underwriting: {
    title: '核保接口未导入',
    desc: '核保通过 / 失败 / 需补充验车材料三种结果都无法获取，失败原因与补充材料入口仅为静态展示。',
    endpoints: ['POST /api/underwriting', 'POST /api/underwriting/{caseId}/materials'],
    owner: '成员B：后端 API',
    ui: '聊天页核保区块',
  },
  payment: {
    title: '第三方支付未导入',
    desc: '支付下单、跳转第三方、支付回调校验都未接入。只有后端确认支付成功后才能展示「已支付 / 已出单」，当前一律不展示。',
    endpoints: ['POST /api/payments', 'GET /api/payments/{paymentId}', 'POST /api/payments/callback'],
    owner: '成员B：后端 API',
    ui: '聊天页支付区块 / 保单页',
  },
  policy: {
    title: '保单明细接口未导入',
    desc: '投保人、被保人、车辆、险种花费、生效与截止日期、后续维护条款均需后端数据，当前为文档骨架占位。',
    endpoints: ['GET /api/policies/{policyId}'],
    owner: '成员B：后端 API',
    ui: '保单页',
  },
  'agent-workbench': {
    title: '业务员批量操作接口未导入',
    desc: '客户 / 车辆 / 订单列表与批量报价、批量核保、批量支付、批量转人工都依赖后端批量接口，表格为静态占位。',
    endpoints: ['GET /api/workbench/orders', 'POST /api/workbench/batch'],
    owner: '成员B：后端 API',
    ui: '业务员工作台',
  },
  handoff: {
    title: '人工接管接口未导入',
    desc: '转人工申请、排队位次、人工坐席回复、接管状态同步都未接入，页面的转人工按钮只弹出占位提示。',
    endpoints: ['POST /api/handoff', 'GET /api/handoff/{ticketId}'],
    owner: '成员B + 成员C',
    ui: '聊天页 / 人工接管页',
  },
  'knowledge-base': {
    title: '行业术语知识库未导入',
    desc: '知识库用于把「三者」「车损」等口语与行业术语对齐，查询接口未接入，术语解释不会出现在对话中。',
    endpoints: ['GET /api/knowledge/terms?q='],
    owner: '成员C：Agent / 智能体接口',
    ui: '聊天页右侧栏',
  },
} as const satisfies Record<string, PlaceholderContent>

export type PlaceholderId = keyof typeof SPECS

export interface PlaceholderSpec extends PlaceholderContent {
  id: PlaceholderId
}

export const PLACEHOLDERS: Record<PlaceholderId, PlaceholderContent> = SPECS

export function getPlaceholder(id: PlaceholderId): PlaceholderSpec {
  return { id, ...PLACEHOLDERS[id] }
}

export const PLACEHOLDER_LIST: PlaceholderSpec[] = (
  Object.keys(PLACEHOLDERS) as PlaceholderId[]
).map(getPlaceholder)

/** 未导入能力总数（用于界面角标） */
export const PLACEHOLDER_COUNT = PLACEHOLDER_LIST.length
