import { Button, Tag } from 'antd'
import { PageHeader } from '@/components/PageHeader'
import { PlaceholderBlock } from '@/components/PlaceholderBlock'
import { useNotImported } from '@/components/useNotImported'
import { SAMPLE_HANDOFF_TICKET } from '@/data/samples'
import { formatTime } from '@/utils/format'

interface TimelineEntry {
  status: 'done' | 'active' | 'pending'
  title: string
  meta: string
  desc: string
}

const TIMELINE: TimelineEntry[] = [
  {
    status: 'done',
    title: '用户发起转人工',
    meta: '11-09 10:14',
    desc: '触发原因：OCR 车主姓名与投保人不一致。需求中「OCR 识别不正确需追问」会走转人工。',
  },
  {
    status: 'active',
    title: '进入排队',
    meta: '11-09 10:15',
    desc: '当前排位第 2 位（静态占位）。真实排位由人工接管平台返回。',
  },
  {
    status: 'pending',
    title: '人工坐席接入',
    meta: '等待中',
    desc: '坐席接入事件由接管平台推送，接口未导入，前端不会自行改成「处理中」。',
  },
  {
    status: 'pending',
    title: '人工回复同步到对话',
    meta: '等待中',
    desc: '人工回复需与 Agent 消息共用同一消息通道，否则切换会话会串消息。',
  },
]

const STATUS_LABEL: Record<string, string> = {
  none: '未转人工',
  requested: '已申请',
  queued: '排队中',
  serving: '人工处理中',
  closed: '已结束',
}

function TicketRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="kv-row">
      <span className="kv-label">{label}</span>
      <span className="kv-value">{value}</span>
    </div>
  )
}

/** 人工接管页：展示转人工状态与处理进度，接口未导入 */
export function HandoffPage() {
  const ticket = SAMPLE_HANDOFF_TICKET
  const notImported = useNotImported()

  return (
    <div className="page">
      <div className="page-scroll">
        <PageHeader
          eyebrow="人工接管"
          title="转人工处理进度"
          subtitle="AI 接管状态与人工处理状态必须一致：前端只渲染服务端返回的状态，不自行推断。"
          actions={
            <Tag bordered={false} style={{ background: 'var(--color-warning-soft)', color: 'var(--color-warning)' }}>
              {STATUS_LABEL[ticket.status]}
            </Tag>
          }
        />

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: 16 }}>
          <section className="panel">
            <div className="panel-head">
              <span className="panel-title">当前接管信息</span>
              <Tag
                bordered={false}
                style={{ background: 'var(--color-muted)', color: 'var(--color-muted-foreground)' }}
              >
                静态占位
              </Tag>
            </div>
            <div className="panel-body" style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
              <div className="kv">
                <TicketRow label="工单号" value={ticket.ticketId} />
                <TicketRow label="会话 ID" value={ticket.conversationId} />
                <TicketRow label="触发原因" value={ticket.reason} />
                <TicketRow label="排队位次" value={ticket.queuePosition ? `第 ${ticket.queuePosition} 位` : '—'} />
                <TicketRow label="接管坐席" value={ticket.assignedAgentName ?? '待分配'} />
                <TicketRow label="发起时间" value={formatTime(ticket.createdAt)} />
              </div>

              <div className="table-actions">
                <Button size="small" onClick={() => notImported('handoff')}>
                  刷新状态
                </Button>
                <Button size="small" onClick={() => notImported('handoff')}>
                  结束接管
                </Button>
                <Button size="small" type="text" onClick={() => notImported('chat-send')}>
                  转回 AI
                </Button>
              </div>
            </div>
          </section>

          <section className="panel">
            <div className="panel-head">
              <span className="panel-title">处理进度</span>
              <span className="muted" style={{ fontSize: 12 }}>
                事件由人工接管平台推送
              </span>
            </div>
            <div className="panel-body">
              <div className="timeline-list">
                {TIMELINE.map((entry) => (
                  <div key={entry.title} className={`timeline-item timeline-item--${entry.status}`}>
                    <span className="timeline-dot" aria-hidden="true" />
                    <div>
                      <div className="timeline-title">{entry.title}</div>
                      <div className="timeline-meta">{entry.meta}</div>
                      <div className="timeline-desc">{entry.desc}</div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </section>
        </div>

        <PlaceholderBlock id="handoff" />

        <div className="batch-bar">
          <div className="batch-count">
            人工接管平台地址、鉴权方式与消息推送协议需项目方提供；当前页面不会主动轮询。
          </div>
          <Button size="small" onClick={() => notImported('handoff')}>
            查看接口约定
          </Button>
        </div>
      </div>
    </div>
  )
}
