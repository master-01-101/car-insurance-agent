/**
 * 与后端约定的 TypeScript 类型（草案）
 * 依据《前端.md》第五节「建议统一的数据展示约定」整理。
 *
 * 注意：以下字段仍需与成员 B（后端）、成员 C（Agent）在联调前确认，
 * 未确认字段统一标记为「待确认」。
 */

/** 统一响应结构（团队约定，待与后端确认） */
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

/** 角色：普通用户 / 业务员 */
export type UserRole = 'user' | 'agent'

export interface AuthUser {
  userId: string
  userName: string
  role: UserRole
  /** 登录时由后端下发的业务员 ID；普通用户也需要携带该 ID 关联业务员 */
  agentId?: string
}

// ---------------------------------------------------------------------------
// 会话与消息
// ---------------------------------------------------------------------------

/** 车险办理的七个阶段 */
export type FlowStageKey =
  | 'collect'
  | 'recommend'
  | 'identity'
  | 'quote'
  | 'underwriting'
  | 'payment'
  | 'policy'

export type FlowStageStatus = 'done' | 'current' | 'pending' | 'locked'

export interface Conversation {
  conversationId: string
  /** 会话标题即车牌号 */
  plateNumber: string
  vehicleId?: string
  currentStage: FlowStageKey
  lastMessagePreview?: string
  updatedAt: string
}

/** 消息角色 */
export type ChatRole = 'user' | 'assistant' | 'tool'

/** 消息类型：文本 / 图片 / 方案卡片 / 业务状态卡片 / 转人工 */
export type MessageType = 'text' | 'image' | 'plan' | 'status' | 'handoff'

export interface ChatMessage {
  conversationId: string
  messageId: string
  role: ChatRole
  content: string
  messageType: MessageType
  createdAt: string
  /** 方案卡片、状态卡片、图片等结构化载荷（字段待与成员 C 确认） */
  payload?: PlanPayload | StatusPayload | ImagePayload | null
}

export interface PlanPayload {
  planId: string
  version: string
  insuranceCompany: string
  totalPremium: number
  items: PlanItem[]
}

export interface StatusPayload {
  statusCode: string
  statusLabel: string
  tone: 'info' | 'success' | 'warning' | 'danger'
  detail?: string
}

export interface ImagePayload {
  url: string
  fileName: string
  category: 'license-main' | 'license-sub' | 'idcard-front' | 'idcard-back' | 'inspection'
}

// ---------------------------------------------------------------------------
// 车辆与资料
// ---------------------------------------------------------------------------

export interface VehicleInfo {
  vehicleId?: string
  plateNumber?: string
  vin?: string
  engineNo?: string
  ownerName?: string
  registerDate?: string
  usageNature?: string
  model?: string
}

export interface OcrField {
  key: keyof VehicleInfo | string
  label: string
  /** OCR 识别值，未识别时为 null */
  value: string | null
  /** 是否为缺失 / 异常字段，需要用户确认或修正 */
  abnormal: boolean
  confirmed: boolean
}

export type UploadCategory =
  | 'license-main'
  | 'license-sub'
  | 'idcard-front'
  | 'idcard-back'
  | 'inspection'

export interface UploadedImage {
  /** 前端本地预览地址 */
  localUrl: string
  fileName: string
  size: number
  category: UploadCategory
  status: 'pending' | 'uploading' | 'done' | 'error'
  /** 服务端文件标识，上传成功后返回 */
  fileId?: string
  /** 上传进度 0-100 */
  progress?: number
}

export type InsuranceType = 'auto' | 'property' | 'life'

// ---------------------------------------------------------------------------
// 方案 / 报价 / 核保 / 支付 / 保单
// ---------------------------------------------------------------------------

export interface PlanItem {
  itemId: string
  /** 险种名称，如 交强险 / 三者险 / 车损险 */
  coverageName: string
  /** 保额（元），不适用时为 null */
  coverageAmount: number | null
  /** 保费（元） */
  premium: number
  /** 是否必选 */
  required: boolean
}

export interface InsurancePlan {
  planId: string
  conversationId: string
  version: string
  insuranceCompany: string
  items: PlanItem[]
  totalPremium: number
  /** 方案是否已由用户确认 */
  confirmed: boolean
  createdAt: string
}

export type QuoteStatus =
  | 'idle'
  | 'quoting'
  | 'quoted'
  | 'failed'
  | 'expired'

export interface QuoteResult {
  quoteId: string
  status: QuoteStatus
  totalPremium: number
  /** 报价有效期 */
  validUntil?: string
  items: PlanItem[]
}

export type UnderwritingStatus = 'pending' | 'pass' | 'reject' | 'need-material'

export interface UnderwritingResult {
  caseId: string
  status: UnderwritingStatus
  /** 失败原因，或需补充的材料说明 */
  reason?: string
  requiredMaterials?: Array<'inspection-photo' | 'inspection-video'>
}

export type PaymentStatus = 'idle' | 'pending' | 'success' | 'failed' | 'closed'

export interface PaymentOrder {
  paymentId: string
  status: PaymentStatus
  amount: number
  /** 第三方支付渠道 */
  channel?: string
  paidAt?: string
}

export interface PolicyDetail {
  policyId: string
  policyNo: string
  applicant: { name: string; idType?: string; idMasked?: string }
  insured: { name: string; idType?: string; idMasked?: string }
  vehicle: VehicleInfo
  items: PlanItem[]
  totalPremium: number
  effectiveFrom: string
  effectiveTo: string
  /** 后续维护条款 */
  terms?: string[]
}

// ---------------------------------------------------------------------------
// 业务员工作台 / 人工接管
// ---------------------------------------------------------------------------

export type WorkbenchOrderStatus =
  | 'collecting'
  | 'recommending'
  | 'quoting'
  | 'underwriting'
  | 'paying'
  | 'issued'
  | 'failed'

export interface WorkbenchRow {
  orderId: string
  customerName: string
  plateNumber: string
  status: WorkbenchOrderStatus
  premium: number | null
  updatedAt: string
}

export type BatchAction = 'quote' | 'underwriting' | 'payment' | 'handoff'

export type HandoffStatus = 'none' | 'requested' | 'queued' | 'serving' | 'closed'

export interface HandoffTicket {
  ticketId: string
  conversationId: string
  status: HandoffStatus
  reason: string
  /** 排位或预计等待时间（待后端确认口径） */
  queuePosition?: number
  assignedAgentName?: string
  createdAt: string
}
