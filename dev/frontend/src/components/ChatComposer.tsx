import { useState } from 'react'
import { Button, Space } from 'antd'
import { SendOutlined } from '@ant-design/icons'
import { useNotImported } from './useNotImported'
import type { PlaceholderId } from '@/api/placeholders'

const QUICK_ACTIONS: Array<{ label: string; id: PlaceholderId }> = [
  { label: '上传行驶证', id: 'license-upload' },
  { label: '上传身份证', id: 'idcard-upload' },
  { label: '修改方案', id: 'plan-revise' },
  { label: '我要报价', id: 'quote' },
  { label: '转人工', id: 'handoff' },
]

/**
 * 聊天输入区。
 * 发送动作需要 POST /api/conversations/{id}/messages（未导入），
 * 因此当前只在本地弹出占位说明，不保留任何伪造的回复。
 */
export function ChatComposer() {
  const notImported = useNotImported()
  const [value, setValue] = useState('')

  const handleSend = () => {
    notImported('chat-send')
  }

  return (
    <div className="composer">
      <div className="composer-chips" role="group" aria-label="快捷操作">
        {QUICK_ACTIONS.map((action) => (
          <button
            key={action.id}
            type="button"
            className="chip"
            onClick={() => notImported(action.id)}
          >
            {action.label}
          </button>
        ))}
      </div>

      <div className="composer-box">
        <textarea
          className="composer-input"
          value={value}
          onChange={(event) => setValue(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === 'Enter' && !event.shiftKey) {
              event.preventDefault()
              handleSend()
            }
          }}
          placeholder="描述你的需求，例如：把三者险提到 200 万；或上传行驶证照片"
          aria-label="聊天输入框"
          rows={2}
        />
        <div className="composer-actions">
          <span className="composer-hint">
            消息不会真正发送 · Enter 发送 / Shift + Enter 换行
          </span>
          <Space size={6}>
            <Button size="small" type="text" onClick={() => notImported('agent-stream')}>
              流式输出
            </Button>
            <Button
              size="small"
              type="primary"
              icon={<SendOutlined />}
              onClick={handleSend}
            >
              发送
            </Button>
          </Space>
        </div>
      </div>

      <div className="ph-inline" role="note">
        <span className="ph-badge">此地方未导入</span>
        <span>消息发送与 Agent 回复接口（成员 C）· 输入内容不会离开浏览器</span>
      </div>
    </div>
  )
}
