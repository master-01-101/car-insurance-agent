import type { ReactNode } from 'react'
import {
  CheckCircleFilled,
  CloseCircleFilled,
  InfoCircleFilled,
  WarningFilled,
} from '@ant-design/icons'

export type StatusTone = 'info' | 'success' | 'warning' | 'danger'

const TONE_ICON: Record<StatusTone, ReactNode> = {
  info: <InfoCircleFilled />,
  success: <CheckCircleFilled />,
  warning: <WarningFilled />,
  danger: <CloseCircleFilled />,
}

interface StatusCardProps {
  tone: StatusTone
  title: string
  desc?: string
  children?: ReactNode
}

/** 业务状态卡片：报价 / 核保 / 支付 / 转人工等结果的统一呈现 */
export function StatusCard({ tone, title, desc, children }: StatusCardProps) {
  return (
    <div className={`status-card status-card--${tone}`}>
      <span className="status-icon" aria-hidden="true">
        {TONE_ICON[tone]}
      </span>
      <div style={{ minWidth: 0 }}>
        <div className="status-title">{title}</div>
        {desc && <div className="status-desc">{desc}</div>}
        {children}
      </div>
    </div>
  )
}
