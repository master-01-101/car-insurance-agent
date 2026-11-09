import { useState } from 'react'
import { Button, Drawer } from 'antd'
import {
  CameraOutlined,
  IdcardOutlined,
  ProfileOutlined,
  SafetyCertificateOutlined,
  UserSwitchOutlined,
} from '@ant-design/icons'
import { ChatBubble } from '@/components/ChatBubble'
import { ChatComposer } from '@/components/ChatComposer'
import { FlowSteps } from '@/components/FlowSteps'
import { PlaceholderBlock } from '@/components/PlaceholderBlock'
import { UploadTile } from '@/components/UploadTile'
import { useNotImported } from '@/components/useNotImported'
import { getStageIndex, getStageLabel, STAGE_STATUS_PLACEHOLDER } from '@/data/flow'
import { OCR_FIELDS, SAMPLE_VEHICLE, SAMPLE_MESSAGES } from '@/data/samples'
import { useAppStore } from '@/store/AppStore'

function VehicleRow({ label, value }: { label: string; value?: string }) {
  return (
    <div className="kv-row">
      <span className="kv-label">{label}</span>
      {value ? (
        <span className="kv-value">{value}</span>
      ) : (
        <span className="kv-value kv-value--empty">待 OCR 回填</span>
      )}
    </div>
  )
}

/** 右侧栏内容：移动端会复用同一份内容放进抽屉 */
function ChatRailContent() {
  const { activeStage } = useAppStore()
  const notImported = useNotImported()

  return (
    <>
      <div className="rail-section">
        <div className="section-title">办理进度</div>
        <FlowSteps statusMap={STAGE_STATUS_PLACEHOLDER} />
        <div className="side-note">
          当前阶段：{getStageLabel(activeStage)}。阶段状态为静态占位，
          实际由会话接口返回的 currentStage 驱动。
        </div>
      </div>

      <div className="rail-section">
        <div className="section-title">车辆资料</div>
        <div className="kv">
          <VehicleRow label="车牌号" value={SAMPLE_VEHICLE.plateNumber} />
          {OCR_FIELDS.filter((field) => field.key !== 'plateNumber').map((field) => (
            <VehicleRow
              key={field.key}
              label={field.label}
              value={SAMPLE_VEHICLE[field.key] as string | undefined}
            />
          ))}
        </div>

        <div className="upload-grid">
          <UploadTile
            title="行驶证正页"
            hint="JPG / PNG，单张 ≤10MB"
            icon={<CameraOutlined />}
            placeholderId="license-upload"
          />
          <UploadTile
            title="行驶证副页"
            hint="JPG / PNG，单张 ≤10MB"
            icon={<CameraOutlined />}
            placeholderId="license-upload"
          />
        </div>

        <PlaceholderBlock id="ocr-license" compact />
      </div>

      <div className="rail-section">
        <div className="section-title">身份证影像</div>
        <div className="upload-grid">
          <UploadTile
            title="身份证正面"
            hint="人像面，需清晰可辨"
            icon={<IdcardOutlined />}
            placeholderId="idcard-upload"
          />
          <UploadTile
            title="身份证反面"
            hint="国徽面，需清晰可辨"
            icon={<IdcardOutlined />}
            placeholderId="idcard-upload"
          />
        </div>
        <PlaceholderBlock id="idcard-upload" compact />
      </div>

      <div className="rail-section">
        <div className="section-title">业务动作</div>
        <div className="table-actions">
          <Button size="small" onClick={() => notImported('quote')}>
            发起报价
          </Button>
          <Button size="small" onClick={() => notImported('underwriting')}>
            提交核保
          </Button>
          <Button size="small" onClick={() => notImported('payment')}>
            去支付
          </Button>
          <Button size="small" onClick={() => notImported('policy')}>
            查看保单
          </Button>
        </div>
        <div className="side-note" style={{ margin: 0 }}>
          报价、核保、支付、出单四个动作都要等后端返回业务状态，
          前端不自行判断成功与否。
        </div>
      </div>

      <div className="rail-section">
        <div className="section-title">术语知识库</div>
        <PlaceholderBlock id="knowledge-base" compact showEndpoints={false} />
      </div>
    </>
  )
}

/**
 * 聊天页：左侧会话列表在应用外壳里，这里是主对话区 + 右侧流程/资料栏。
 * 所有数据来自静态占位；真实消息、上传、方案都未导入。
 */
export function ChatPage() {
  const { activeConversation, activeStage } = useAppStore()
  const notImported = useNotImported()
  const [railOpen, setRailOpen] = useState(false)

  const plate = activeConversation?.plateNumber ?? '未选择会话'
  const stageIndex = getStageIndex(activeStage)

  return (
    <div className="chat-page">
      <section className="panel chat-main" aria-label="对话区">
        <div className="chat-head">
          <div className="chat-head-left">
            <span className="plate">{plate}</span>
            <div>
              <div className="chat-title">车险办理 · {getStageLabel(activeStage)}</div>
              <div className="chat-meta">
                阶段 {stageIndex} / 7 · 会话 ID {activeConversation?.conversationId ?? '—'}
              </div>
            </div>
          </div>

          <div className="chat-head-actions">
            <Button
              size="small"
              className="rail-toggle"
              icon={<ProfileOutlined />}
              onClick={() => setRailOpen(true)}
            >
              流程与资料
            </Button>
            <Button size="small" icon={<UserSwitchOutlined />} onClick={() => notImported('handoff')}>
              转人工
            </Button>
            <Button
              size="small"
              type="primary"
              icon={<SafetyCertificateOutlined />}
              onClick={() => notImported('quote')}
            >
              发起报价
            </Button>
          </div>
        </div>

        <div className="chat-thread">
          <PlaceholderBlock id="chat-history" />

          {SAMPLE_MESSAGES.map((message) => (
            <ChatBubble key={message.messageId} message={message} />
          ))}

          <div className="chat-typing">
            <span className="typing-dots" aria-hidden="true">
              <span />
              <span />
              <span />
            </span>
            <span>Agent 回复未导入 · 此处在等待流式输出</span>
          </div>
        </div>

        <ChatComposer />
      </section>

      <aside className="chat-rail panel" aria-label="流程与资料">
        <ChatRailContent />
      </aside>

      <Drawer
        open={railOpen}
        onClose={() => setRailOpen(false)}
        placement="right"
        width={340}
        title="流程与资料"
        styles={{ body: { padding: 0, background: 'var(--surface-solid)' } }}
      >
        <ChatRailContent />
      </Drawer>
    </div>
  )
}
