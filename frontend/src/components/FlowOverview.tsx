import { FLOW_STAGES } from '@/data/flow'

/** 七阶段横向概览（车险入口页使用） */
export function FlowOverview() {
  return (
    <div className="flow-overview" role="list" aria-label="车险办理流程概览">
      {FLOW_STAGES.map((stage) => (
        <div key={stage.key} className="flow-overview-item" role="listitem">
          <span className="flow-overview-index">
            {String(stage.index).padStart(2, '0')}
          </span>
          <span className="flow-overview-label">{stage.label}</span>
          <span className="flow-overview-desc">{stage.desc}</span>
        </div>
      ))}
    </div>
  )
}
