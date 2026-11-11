import { useNavigate } from 'react-router-dom'
import { PlusOutlined } from '@ant-design/icons'
import { Button, Tooltip } from 'antd'
import { useAppStore } from '@/store/AppStore'
import { getStageLabel } from '@/data/flow'
import { formatTime } from '@/utils/format'
import { useNotImported } from './useNotImported'

/**
 * 会话列表：标题即车牌号。
 * 数据来源 GET /api/conversations（未导入），当前为静态占位。
 */
export function ConversationList() {
  const { conversations, activeConversationId, setActiveConversation } = useAppStore()
  const navigate = useNavigate()
  const notImported = useNotImported()

  return (
    <div className="side-section">
      <div className="side-section-head">
        <span className="side-caption">会话 · 按车牌号</span>
        <Tooltip title="新建会话（接口未导入）">
          <Button
            size="small"
            type="text"
            icon={<PlusOutlined />}
            aria-label="新建会话"
            onClick={() => notImported('conversation-list')}
          />
        </Tooltip>
      </div>

      <div className="conv-list">
        {conversations.map((conversation) => {
          const active = conversation.conversationId === activeConversationId
          return (
            <button
              key={conversation.conversationId}
              type="button"
              className={`conv-item${active ? ' conv-item--active' : ''}`}
              aria-current={active ? 'page' : undefined}
              onClick={() => {
                setActiveConversation(conversation.conversationId)
                navigate('/chat')
              }}
            >
              <div className="conv-body">
                <div className="conv-plate">{conversation.plateNumber}</div>
                <div className="conv-meta">
                  <span
                    className="conv-dot"
                    style={{ background: active ? 'var(--color-accent)' : 'var(--border-strong)' }}
                  />
                  <span>{getStageLabel(conversation.currentStage)}</span>
                  <span aria-hidden="true">·</span>
                  <span className="tabular">{formatTime(conversation.updatedAt)}</span>
                </div>
              </div>
            </button>
          )
        })}
      </div>

      <div className="side-note">
        会话标题用车牌号命名，出现第二个车牌时自动新建会话。
        <br />
        会话接口未导入，列表为静态占位。
      </div>
    </div>
  )
}
