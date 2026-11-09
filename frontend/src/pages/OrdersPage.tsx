import { Button, Table, Tag } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { PageHeader } from '@/components/PageHeader'
import { PlaceholderBlock } from '@/components/PlaceholderBlock'
import { OrderStatusTag } from '@/components/StatusTag'
import { useNotImported } from '@/components/useNotImported'
import { SAMPLE_WORKBENCH_ROWS } from '@/data/samples'
import type { WorkbenchRow } from '@/types'
import { formatMoney, formatTime } from '@/utils/format'

const columns: ColumnsType<WorkbenchRow> = [
  {
    title: '订单号',
    dataIndex: 'orderId',
    key: 'orderId',
    render: (value: string) => <span className="mono">{value}</span>,
  },
  { title: '车牌号', dataIndex: 'plateNumber', key: 'plateNumber' },
  { title: '客户', dataIndex: 'customerName', key: 'customerName' },
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

/** 订单列表：数据来自后端订单接口（未导入），当前表格为静态占位 */
export function OrdersPage() {
  const notImported = useNotImported()

  return (
    <div className="page">
      <div className="page-scroll">
        <PageHeader
          eyebrow="订单"
          title="我的订单"
          subtitle="报价、核保、支付、出单的状态都以后端返回为准，前端不做推断。"
          actions={
            <Button size="small" onClick={() => notImported('quote')}>
              刷新
            </Button>
          }
        />

        <PlaceholderBlock id="quote" compact showEndpoints={false} />

        <section className="panel">
          <div className="panel-head">
            <span className="panel-title">订单列表</span>
            <Tag
              bordered={false}
              style={{ background: 'var(--color-muted)', color: 'var(--color-muted-foreground)' }}
            >
              静态占位
            </Tag>
          </div>
          <div className="panel-body" style={{ padding: 0 }}>
            <Table<WorkbenchRow>
              rowKey="orderId"
              columns={[
                ...columns,
                {
                  title: '操作',
                  key: 'actions',
                  width: 110,
                  render: (_, record) => (
                    <div className="table-actions">
                      <Button size="small" type="link" onClick={() => notImported('quote')}>
                        详情
                      </Button>
                      <Button
                        size="small"
                        type="link"
                        disabled={record.status === 'issued'}
                        onClick={() => notImported('payment')}
                      >
                        支付
                      </Button>
                    </div>
                  ),
                },
              ]}
              dataSource={SAMPLE_WORKBENCH_ROWS}
              pagination={false}
              size="middle"
              scroll={{ x: 720 }}
            />
          </div>
        </section>

        <div className="batch-bar">
          <div className="batch-count">
            订单接口未导入，列表不会刷新，也不会随会话状态变化。
          </div>
          <Button size="small" onClick={() => notImported('agent-workbench')}>
            批量操作说明
          </Button>
        </div>
      </div>
    </div>
  )
}
