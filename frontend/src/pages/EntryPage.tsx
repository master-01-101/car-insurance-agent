import type { ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, Tag } from 'antd'
import { CarOutlined, HeartOutlined, HomeOutlined } from '@ant-design/icons'
import { PageHeader } from '@/components/PageHeader'
import { FlowOverview } from '@/components/FlowOverview'
import { PlaceholderBlock } from '@/components/PlaceholderBlock'
import { useNotImported } from '@/components/useNotImported'

interface InsuranceCard {
  key: string
  name: string
  desc: string
  icon: ReactNode
  enabled: boolean
}

const CARDS: InsuranceCard[] = [
  {
    key: 'auto',
    name: '车险',
    desc: '交强险 + 商业险的报价、核保与出单，支持用自然语言调整方案。',
    icon: <CarOutlined />,
    enabled: true,
  },
  {
    key: 'property',
    name: '财险',
    desc: '家财险、企业财产险等，当前版本暂未开放。',
    icon: <HomeOutlined />,
    enabled: false,
  },
  {
    key: 'life',
    name: '人寿险',
    desc: '定期寿险、重疾险等，当前版本暂未开放。',
    icon: <HeartOutlined />,
    enabled: false,
  },
]

/** 车险入口：只允许进入车险流程，其余险种显示为暂未开放 */
export function EntryPage() {
  const navigate = useNavigate()
  const notImported = useNotImported()

  return (
    <div className="page">
      <div className="page-scroll">
        <PageHeader
          eyebrow="第一步 · 选择险种"
          title="选择要办理的险种"
          subtitle="当前版本只开放车险流程。财险、人寿险在需求中仅作占位，不提供入口。"
          actions={
            <Tag bordered={false} style={{ background: 'var(--color-muted)', color: 'var(--color-muted-foreground)' }}>
              后端未接入
            </Tag>
          }
        />

        <div className="entry-grid">
          {CARDS.map((card) => (
            <div
              key={card.key}
              className={`entry-card ${card.enabled ? 'entry-card--active' : 'entry-card--disabled'}`}
            >
              <div className="entry-icon" aria-hidden="true">
                {card.icon}
              </div>
              <div className="entry-name">{card.name}</div>
              <div className="entry-desc">{card.desc}</div>
              <div className="entry-foot">
                <Tag
                  bordered={false}
                  style={
                    card.enabled
                      ? { background: 'var(--color-success-soft)', color: 'var(--color-success)' }
                      : { background: 'var(--color-muted)', color: 'var(--color-muted-foreground)' }
                  }
                >
                  {card.enabled ? '可用' : '暂未开放'}
                </Tag>
                <Button
                  size="small"
                  type={card.enabled ? 'primary' : 'default'}
                  disabled={!card.enabled}
                  onClick={() => navigate('/chat')}
                >
                  {card.enabled ? '开始办理' : '敬请期待'}
                </Button>
              </div>
            </div>
          ))}
        </div>

        <section className="panel">
          <div className="panel-head">
            <span className="panel-title">车险办理流程</span>
            <span className="muted" style={{ fontSize: 12 }}>
              7 个阶段 · 阶段状态由服务端驱动
            </span>
          </div>
          <div className="panel-body">
            <FlowOverview />
          </div>
        </section>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: 16 }}>
          <PlaceholderBlock id="vehicle-query" />
          <PlaceholderBlock id="knowledge-base" />
        </div>

        <div className="batch-bar">
          <div className="batch-count">
            想先用老客户数据看看？续保查询需要后端历史保单，当前不可用。
          </div>
          <Button size="small" onClick={() => notImported('vehicle-query')}>
            续保查询
          </Button>
        </div>
      </div>
    </div>
  )
}
