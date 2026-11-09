import {
  getPlaceholder,
  type PlaceholderId,
} from '@/api/placeholders'

/**
 * 占位符：标记「前端 UI 已就位、后端 / Agent 能力未导入」的区域。
 * 所有文案来自 src/api/placeholders.ts，避免各页面不一致。
 */

interface BlockProps {
  id: PlaceholderId
  /** 只显示标题与说明，用于空间紧张的位置 */
  compact?: boolean
  showEndpoints?: boolean
  className?: string
}

export function PlaceholderBlock({
  id,
  compact = false,
  showEndpoints = true,
  className,
}: BlockProps) {
  const spec = getPlaceholder(id)

  return (
    <div className={['ph-block', className].filter(Boolean).join(' ')} role="note">
      <div className="ph-head">
        <span className="ph-badge">此地方未导入</span>
        <span className="ph-owner">待{spec.owner}</span>
      </div>
      <p className="ph-title">{spec.title}</p>
      {!compact && <p className="ph-desc">{spec.desc}</p>}
      {!compact && showEndpoints && spec.endpoints.length > 0 && (
        <ul className="ph-endpoints" aria-label="预期接口">
          {spec.endpoints.map((endpoint) => (
            <li key={endpoint}>{endpoint}</li>
          ))}
        </ul>
      )}
    </div>
  )
}

interface InlineProps {
  id: PlaceholderId
  /** 覆盖默认文案 */
  text?: string
}

export function PlaceholderInline({ id, text }: InlineProps) {
  const spec = getPlaceholder(id)
  return (
    <div className="ph-inline" role="note">
      <span className="ph-badge">此地方未导入</span>
      <span>{text ?? spec.title}</span>
    </div>
  )
}

/** 弹窗内的完整占位说明 */
export function PlaceholderDetail({ id }: { id: PlaceholderId }) {
  const spec = getPlaceholder(id)

  return (
    <div style={{ fontSize: 13, lineHeight: 1.7 }}>
      <p style={{ fontWeight: 600, color: 'var(--color-primary)' }}>{spec.title}</p>
      <p style={{ marginTop: 6, color: 'var(--color-muted-foreground)' }}>{spec.desc}</p>

      {spec.endpoints.length > 0 && (
        <>
          <div className="eyebrow" style={{ marginTop: 14 }}>
            预期接口（待确认）
          </div>
          <ul className="ph-endpoints" style={{ marginTop: 6 }}>
            {spec.endpoints.map((endpoint) => (
              <li key={endpoint}>{endpoint}</li>
            ))}
          </ul>
        </>
      )}

      <div
        style={{
          marginTop: 14,
          paddingTop: 10,
          borderTop: '1px dashed var(--border-subtle)',
          fontSize: 12,
          color: 'var(--color-muted-foreground)',
        }}
      >
        <div>补齐方：{spec.owner}</div>
        <div>界面位置：{spec.ui}</div>
        <div style={{ marginTop: 4 }}>
          占位标识：<code>{spec.id}</code>
        </div>
      </div>
    </div>
  )
}
