import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { FileImageOutlined, RobotOutlined, ToolOutlined, UserOutlined } from '@ant-design/icons'
import type { ChatMessage, PlanPayload, StatusPayload } from '@/types'
import { formatTime } from '@/utils/format'
import { PlanCard } from './PlanCard'
import { StatusCard } from './StatusCard'

const ROLE_LABEL: Record<ChatMessage['role'], string> = {
  user: '我',
  assistant: '车险助手',
  tool: '工具调用',
}

const ROLE_ICON: Record<ChatMessage['role'], ReactNode> = {
  user: <UserOutlined />,
  assistant: <RobotOutlined />,
  tool: <ToolOutlined />,
}

/**
 * 聊天气泡：支持 text / image / plan / status / handoff 五种消息类型
 * （结构见《前端.md》第五节的聊天消息建议结构）。
 */
export function ChatBubble({ message }: { message: ChatMessage }) {
  const isPlan = message.messageType === 'plan'
  const payload = message.payload

  const planPayload: PlanPayload | null =
    isPlan && payload && 'items' in payload ? payload : null
  const statusPayload: StatusPayload | null =
    message.messageType === 'status' && payload && 'tone' in payload ? payload : null

  return (
    <div className={`msg msg--${message.role}${isPlan ? ' msg-wide' : ''}`}>
      <div className="msg-avatar" aria-hidden="true">
        {ROLE_ICON[message.role]}
      </div>

      <div className="msg-body">
        <div className="msg-meta">
          <span className="msg-name">{ROLE_LABEL[message.role]}</span>
          <span className="tabular">{formatTime(message.createdAt)}</span>
          <span className="muted">静态占位</span>
        </div>

        {message.content && <div className="bubble">{message.content}</div>}

        {message.messageType === 'image' && (
          <div className="upload-tile" style={{ cursor: 'default', width: 220 }}>
            <div className="upload-thumb">
              <FileImageOutlined />
            </div>
            <div className="upload-name">图片消息</div>
            <div className="upload-hint">影像上传接口未导入，缩略图无法渲染。</div>
          </div>
        )}

        {statusPayload && (
          <StatusCard
            tone={statusPayload.tone}
            title={statusPayload.statusLabel}
            desc={statusPayload.detail}
          />
        )}

        {message.messageType === 'handoff' && (
          <StatusCard
            tone="warning"
            title="已发起转人工"
            desc="人工接管接口未导入：排队位次、坐席接入、人工回复都无法同步。"
          >
            <div style={{ marginTop: 8 }}>
              <Link to="/handoff" style={{ fontSize: 12, fontWeight: 600 }}>
                查看人工接管进度 →
              </Link>
            </div>
          </StatusCard>
        )}

        {planPayload && <PlanCard plan={planPayload} />}
      </div>
    </div>
  )
}
