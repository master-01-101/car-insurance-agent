import { Tag } from 'antd'
import type { WorkbenchOrderStatus } from '@/types'

const ORDER_STATUS: Record<WorkbenchOrderStatus, { label: string; color: string }> = {
  collecting: { label: '收集信息', color: '#475569' },
  recommending: { label: '方案推荐', color: '#0369a1' },
  quoting: { label: '报价中', color: '#0369a1' },
  underwriting: { label: '核保中', color: '#b45309' },
  paying: { label: '支付中', color: '#b45309' },
  issued: { label: '已出单', color: '#15803d' },
  failed: { label: '失败', color: '#dc2626' },
}

export function OrderStatusTag({ status }: { status: WorkbenchOrderStatus }) {
  const meta = ORDER_STATUS[status]
  return (
    <Tag
      bordered={false}
      style={{ background: `${meta.color}1a`, color: meta.color, fontWeight: 500 }}
    >
      {meta.label}
    </Tag>
  )
}
