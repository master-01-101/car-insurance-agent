import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { RouterProvider } from 'react-router-dom'
import { App as AntApp, ConfigProvider } from 'antd'
import zhCN from 'antd/locale/zh_CN'

import 'antd/dist/reset.css'
import '@/styles/global.css'

import { antdTheme } from '@/theme/antdTheme'
import { AppStoreProvider } from '@/store/AppStore'
import { router } from '@/router'

const container = document.getElementById('root')
if (!container) {
  throw new Error('找不到 #root 挂载节点')
}

createRoot(container).render(
  <StrictMode>
    <ConfigProvider locale={zhCN} theme={antdTheme}>
      <AntApp>
        <AppStoreProvider>
          <RouterProvider router={router} />
        </AppStoreProvider>
      </AntApp>
    </ConfigProvider>
  </StrictMode>,
)
