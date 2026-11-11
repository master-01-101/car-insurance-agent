import type { FlowStageKey, FlowStageStatus } from '@/types'

/**
 * 车险办理的七个阶段（来自《车险出单需求.md》，属于确定的业务流程）。
 * 阶段状态由后端业务状态驱动，前端不自行推断。
 */

export interface FlowStageDef {
  key: FlowStageKey
  index: number
  label: string
  desc: string
}

export const FLOW_STAGES: FlowStageDef[] = [
  {
    key: 'collect',
    index: 1,
    label: '收集信息',
    desc: '新用户上传行驶证正副页做 OCR；老用户凭车牌号或车架号查询去年信息。',
  },
  {
    key: 'recommend',
    index: 2,
    label: '智能推荐',
    desc: '给出保险公司、险种与价格方案，支持用自然语言修改。',
  },
  {
    key: 'identity',
    index: 3,
    label: '身份证上传',
    desc: '提交投保人身份证正反面影像。',
  },
  {
    key: 'quote',
    index: 4,
    label: '报价',
    desc: '核对投保影像与关键信息后发起报价。',
  },
  {
    key: 'underwriting',
    index: 5,
    label: '核保',
    desc: '保险公司核查；失败时按提示补充验车照片或视频。',
  },
  {
    key: 'payment',
    index: 6,
    label: '支付',
    desc: '跳转第三方平台支付，返回后以服务端结果为准。',
  },
  {
    key: 'policy',
    index: 7,
    label: '保单',
    desc: '支付成功后生成保单明细。',
  },
]

/**
 * 各阶段状态（界面演示用）。
 * 待 GET /api/conversations/{id} 返回 currentStage 后由后端驱动。
 */
export const STAGE_STATUS_PLACEHOLDER: Record<FlowStageKey, FlowStageStatus> = {
  collect: 'done',
  recommend: 'current',
  identity: 'pending',
  quote: 'pending',
  underwriting: 'pending',
  payment: 'pending',
  policy: 'locked',
}

export function getStageIndex(key: FlowStageKey): number {
  return FLOW_STAGES.find((stage) => stage.key === key)?.index ?? 1
}

export function getStageLabel(key: FlowStageKey): string {
  return FLOW_STAGES.find((stage) => stage.key === key)?.label ?? '未知阶段'
}
