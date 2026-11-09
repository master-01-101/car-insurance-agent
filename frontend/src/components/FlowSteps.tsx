import { CheckOutlined } from '@ant-design/icons'
import type { FlowStageKey, FlowStageStatus } from '@/types'
import { FLOW_STAGES } from '@/data/flow'

const STATUS_TEXT: Record<FlowStageStatus, string> = {
  done: '已完成',
  current: '进行中',
  pending: '待开始',
  locked: '未解锁',
}

interface FlowStepsProps {
  /** 阶段状态由后端业务状态驱动，这里只负责渲染 */
  statusMap: Record<FlowStageKey, FlowStageStatus>
}

export function FlowSteps({ statusMap }: FlowStepsProps) {
  return (
    <div className="flow" role="list" aria-label="车险办理阶段">
      {FLOW_STAGES.map((stage) => {
        const status = statusMap[stage.key]
        return (
          <div key={stage.key} className={`flow-step flow-step--${status}`} role="listitem">
            <div className="flow-index" aria-hidden="true">
              {status === 'done' ? <CheckOutlined style={{ fontSize: 8.5 }} /> : stage.index}
            </div>
            <div style={{ minWidth: 0 }}>
              <div className="flow-label">
                {stage.label}
                <span className="visually-hidden">（{STATUS_TEXT[status]}）</span>
              </div>
              <div className="flow-state">
                {STATUS_TEXT[status]}
                {status === 'current' ? ` · ${stage.desc}` : ''}
              </div>
            </div>
          </div>
        )
      })}
    </div>
  )
}
