import { useCallback } from 'react'
import { App } from 'antd'
import { ExclamationCircleOutlined } from '@ant-design/icons'
import type { PlaceholderId } from '@/api/placeholders'
import { PlaceholderDetail } from './PlaceholderBlock'

/**
 * 点击尚未导入的功能时统一弹出说明弹窗。
 * 用法：const notImported = useNotImported(); onClick={() => notImported('quote')}
 */
export function useNotImported(): (id: PlaceholderId) => void {
  const { modal } = App.useApp()

  return useCallback(
    (id: PlaceholderId) => {
      modal.info({
        centered: true,
        width: 560,
        okText: '知道了',
        icon: null,
        title: (
          <span style={{ display: 'inline-flex', alignItems: 'center', gap: 8 }}>
            <ExclamationCircleOutlined style={{ color: 'var(--color-warning)' }} />
            此地方未导入
          </span>
        ),
        content: <PlaceholderDetail id={id} />,
      })
    },
    [modal],
  )
}
