/**
 * 按业务模块封装的接口层（契约骨架）
 * ---------------------------------------------------------------------------
 * 页面组件只允许从这里取接口，不写散落的硬编码 URL（《前端.md》第四节）。
 *
 * 当前状态：所有方法都直接 reject(NotImportedError)，不会发起网络请求。
 * 导入真实后端 / Agent 接口时，把对应方法的 `notImported(...)` 换成
 * `request({ url, method, data })` 即可，函数签名与调用方无需改动。
 */

import { notImported } from './notImported'
import type {
  ApplicantConfirmPayload,
  AuthUser,
  BatchAction,
  ChatMessage,
  Conversation,
  FlowStageKey,
  HandoffTicket,
  InsurancePlan,
  OcrResult,
  OcrSide,
  PaymentOrder,
  PolicyDetail,
  QuoteResult,
  UnderwritingResult,
  UploadedFileRef,
  UserRole,
  VehicleInfo,
  VehicleProfileConfirmPayload,
  WorkbenchRow,
} from '@/types'

// ---------------------------------------------------------------------------
// 登录与身份
// ---------------------------------------------------------------------------
export const authApi = {
  /** 待导入：真实账号校验、token 下发、角色判定 */
  login(_payload: { account: string; password: string; role: UserRole }): Promise<AuthUser> {
    return notImported('auth-login')
  },
  logout(): Promise<void> {
    return notImported('auth-login')
  },
}

// ---------------------------------------------------------------------------
// 会话与消息
// ---------------------------------------------------------------------------
export const conversationApi = {
  list(): Promise<Conversation[]> {
    return notImported('conversation-list')
  },
  create(_plateNumber: string): Promise<Conversation> {
    return notImported('conversation-list')
  },
  rename(_conversationId: string, _plateNumber: string): Promise<Conversation> {
    return notImported('conversation-list')
  },
}

export const chatApi = {
  history(_conversationId: string, _cursor?: string): Promise<ChatMessage[]> {
    return notImported('chat-history')
  },
  send(_conversationId: string, _content: string): Promise<ChatMessage> {
    return notImported('chat-send')
  },
  /** 流式输出留待第二阶段，先保留普通 HTTP 契约 */
  stream(_conversationId: string): Promise<void> {
    return notImported('agent-stream')
  },
}

// ---------------------------------------------------------------------------
// 车辆资料 / OCR
// ---------------------------------------------------------------------------
export const vehicleApi = {
  /** 老用户凭车牌号或车架号查询去年保单 */
  lookup(_params: { plateNumber?: string; vin?: string }): Promise<VehicleInfo | null> {
    return notImported('vehicle-query')
  },
  /**
   * 用户确认/修正 OCR 结果后保存车辆资料。
   * 建议由成员 B 提供（前端不直接写库），返回保存后的最新资料与缺失字段。
   * 见 docs/前端API对接清单.md 第 2 节。
   */
  confirmProfile(
    _conversationId: string,
    _payload: VehicleProfileConfirmPayload,
  ): Promise<{ vehicle: VehicleInfo; missingFields: string[]; stage?: FlowStageKey }> {
    return notImported('ocr-license')
  },
}

/** 投保人身份信息确认（身份证 OCR 之后） */
export const applicantApi = {
  confirm(
    _conversationId: string,
    _payload: ApplicantConfirmPayload,
  ): Promise<{ applicantMasked: { name: string; idMasked?: string }; stage?: FlowStageKey }> {
    return notImported('idcard-upload')
  },
}

export const documentApi = {
  uploadLicense(
    _file: File,
    _category: 'license-main' | 'license-sub',
  ): Promise<UploadedFileRef> {
    return notImported('license-upload')
  },
  uploadIdCard(
    _file: File,
    _category: 'idcard-front' | 'idcard-back',
  ): Promise<UploadedFileRef> {
    return notImported('idcard-upload')
  },
  remove(_fileId: string): Promise<void> {
    return notImported('license-upload')
  },
}

export const ocrApi = {
  /** 前端只传 fileId，不回传图片内容；识别结果带 confidence 供高亮 */
  recognizeLicense(_fileId: string, _side: OcrSide): Promise<OcrResult> {
    return notImported('ocr-license')
  },
  recognizeIdCard(
    _fileId: string,
    _side: OcrSide,
  ): Promise<{ name?: string; idMasked?: string; validTo?: string }> {
    return notImported('idcard-upload')
  },
}

// ---------------------------------------------------------------------------
// 方案 / 报价 / 核保 / 支付 / 保单
// ---------------------------------------------------------------------------
export const planApi = {
  recommend(_conversationId: string): Promise<InsurancePlan> {
    return notImported('plan-recommend')
  },
  /** 自然语言修改方案，返回后端确认后的新版本 */
  revise(_planId: string, _instruction: string): Promise<InsurancePlan> {
    return notImported('plan-revise')
  },
  confirm(_planId: string): Promise<InsurancePlan> {
    return notImported('plan-revise')
  },
}

export const quoteApi = {
  create(_conversationId: string, _planId: string): Promise<QuoteResult> {
    return notImported('quote')
  },
  detail(_quoteId: string): Promise<QuoteResult> {
    return notImported('quote')
  },
}

export const underwritingApi = {
  submit(_quoteId: string): Promise<UnderwritingResult> {
    return notImported('underwriting')
  },
  uploadMaterials(_caseId: string, _fileIds: string[]): Promise<UnderwritingResult> {
    return notImported('underwriting')
  },
}

export const paymentApi = {
  create(_quoteId: string): Promise<PaymentOrder> {
    return notImported('payment')
  },
  /** 支付结果只能由服务端回调确认 */
  detail(_paymentId: string): Promise<PaymentOrder> {
    return notImported('payment')
  },
}

export const policyApi = {
  detail(_policyId: string): Promise<PolicyDetail> {
    return notImported('policy')
  },
  list(): Promise<PolicyDetail[]> {
    return notImported('policy')
  },
}

// ---------------------------------------------------------------------------
// 业务员工作台 / 人工接管 / 知识库
// ---------------------------------------------------------------------------
export const workbenchApi = {
  orders(_keyword?: string): Promise<WorkbenchRow[]> {
    return notImported('agent-workbench')
  },
  batch(_orderIds: string[], _action: BatchAction): Promise<{ accepted: number }> {
    return notImported('agent-workbench')
  },
}

export const handoffApi = {
  request(_conversationId: string, _reason: string): Promise<HandoffTicket> {
    return notImported('handoff')
  },
  detail(_ticketId: string): Promise<HandoffTicket> {
    return notImported('handoff')
  },
}

export const knowledgeApi = {
  lookupTerm(_keyword: string): Promise<Array<{ term: string; explanation: string }>> {
    return notImported('knowledge-base')
  },
}

export * from './placeholders'
export * from './notImported'
