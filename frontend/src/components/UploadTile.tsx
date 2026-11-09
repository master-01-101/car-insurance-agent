import type { ReactNode } from 'react'
import { Tag } from 'antd'
import type { PlaceholderId } from '@/api/placeholders'
import { useNotImported } from './useNotImported'

interface UploadTileProps {
  title: string
  hint: string
  icon: ReactNode
  placeholderId: PlaceholderId
}

/**
 * 影像上传位（行驶证正/副页、身份证正/反面、验车材料）。
 * 点击只弹出占位说明：上传服务未导入，不产生本地文件对象，也没有进度。
 */
export function UploadTile({ title, hint, icon, placeholderId }: UploadTileProps) {
  const notImported = useNotImported()

  return (
    <button
      type="button"
      className="upload-tile"
      onClick={() => notImported(placeholderId)}
      aria-label={`${title}：上传接口未导入`}
    >
      <div className="upload-thumb" aria-hidden="true">
        {icon}
      </div>
      <div className="upload-name">
        <span>{title}</span>
        <Tag bordered={false} style={{ background: 'var(--color-muted)', color: 'var(--color-muted-foreground)' }}>
          待上传
        </Tag>
      </div>
      <div className="upload-hint">{hint}</div>
    </button>
  )
}
