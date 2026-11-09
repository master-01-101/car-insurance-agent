import { useState, type Key } from 'react'
import { Button, Table, Tag } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { PageHeader } from '@/components/PageHeader'
import { PlaceholderBlock } from '@/components/PlaceholderBlock'
import { OrderStatusTag } from '@/components/StatusTag'
import { useNotImported } from '@/components/useNotImported'
import { SAMPLE_WORKBENCH_ROWS } from '@/data/samples'
import { useAppStore } from '@/store/AppStore'
import type { BatchAction, WorkbenchRow } from '@/types'
import { formatMoney, formatTime } from '@/utils/format'

const BATCH_ACTIONS: Array<{ key: BatchAction; label: string }> = [
  { key: 'quote', label: '批量报价' },
  { key: 'underwriting', label: '批量核保' },
  { key: 'payment', label: '批量支付' },
  { key: 'handoff', label: '批量转人工' },
]

const METRICS = [
  { label: '待处理', note: '统计接口未导入' },
  { label: '核保中', note: '统计接口未导入' },
  { label: '已出单', note: '统计接口未导入' },
  { label: '需人工', note: '统计接口未导入' },
]

const columns: ColumnsType<WorkbenchRow> = [
  {
    title: '订单号',
    dataIndex: 'orderId',
    key: 'orderId',
    render: (value: string) => <span className="mono">{value}</span>,
  },
  { title: '客户', dataIndex: 'customerName', key: 'customerName' },
  { title: '车牌号', dataIndex: 'plateNumber', key: 'plateNumber' },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    render: (value: WorkbenchRow['status']) => <OrderStatusTag status={value} />,
  },
  {
    title: '保费',
    dataIndex: 'premium',
    key: 'premium',
    align: 'right',
    render: (value: number | null) =>
      value === null ? <span className="muted">待报价</span> : `¥${formatMoney(value)}`,
  },
  {
    title: '更新时间',
    dataIndex: 'updatedAt',
    key: 'updatedAt',
    render: (value: string) => <span className="tabular">{formatTime(value)}</span>,
  },
]

/**
 * 业务员工作台：客户 / 车辆 / 订单列表 + 多选批量操作。
 * 仅业务员角色可进入（见 router/RequireRole）。数据与批量接口均未导入。
 */
export function WorkbenchPage() {
  const { user } = useAppStore()
  const notImported = useNotImported()
  const [selectedKeys, setSelectedKeys] = useState<Key[]>([])

  return (
    <div className="page">
      <div className="page-scroll">
        <PageHeader
          eyebrow="业务员"
          title="业务员工作台"
          subtitle={`当前身份：${user?.userName ?? '—'} · 业务员 ID 待登录接口下发。批量操作与用户单笔流程共用同一套后端状态。`}
          actions={
            <Tag bordered={false} style={{ background: 'var(--color-muted)', color: 'var(--color-muted-foreground)' }}>
              批量接口未导入
            </Tag>
          }
        />

        <div className="metric-row">
          {METRICS.map((metric) => (
            <div key={metric.label} className="metric">
              <div className="metric-label">{metric.label}</div>
              <div className="metric-value">—</div>
              <div className="metric-note">{metric.note}</div>
            </div>
          ))}
        </div>

        <div className="batch-bar">
          <div className="batch-count">
            已选 <strong>{selectedKeys.length}</strong> 条 / 共 {SAMPLE_WORKBENCH_ROWS.length} 条
            <span className="muted"> · 列表为静态占位</span>
          </div>
          <div className="table-actions">
            {BATCH_ACTIONS.map((action) => (
              <Button
                key={action.key}
                size="small"
                type={action.key === 'quote' ? 'primary' : 'default'}
                disabled={selectedKeys.length === 0}
                onClick={() => notImported('agent-workbench')}
              >
                {action.label}
              </Button>
            ))}
          </div>
        </div>

        <section className="panel">
          <div className="panel-head">
            <span className="panel-title">客户 / 车辆 / 订单</span>
            <span className="muted" style={{ fontSize: 12 }}>
              支持多选后批量提交允许的操作
            </span>
          </div>
          <div className="panel-body" style={{ padding: 0 }}>
            <Table<WorkbenchRow>
              rowKey="orderId"
              columns={columns}
              dataSource={SAMPLE_WORKBENCH_ROWS}
              pagination={false}
              size="middle"
              scroll={{ x: 780 }}
              rowSelection={{
                selectedRowKeys: selectedKeys,
                onChange: (keys) => setSelectedKeys(keys),
              }}
            />
          </div>
        </section>

        <PlaceholderBlock id="agent-workbench" />
      </div>
    </div>
  )
}
