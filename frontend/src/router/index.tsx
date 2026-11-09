import { createBrowserRouter } from 'react-router-dom'
import { AppLayout } from '@/layouts/AppLayout'
import { RequireAuth, RequireRole } from './guards'
import { ChatPage } from '@/pages/ChatPage'
import { EntryPage } from '@/pages/EntryPage'
import { HandoffPage } from '@/pages/HandoffPage'
import { LoginPage } from '@/pages/LoginPage'
import { NotFoundPage } from '@/pages/NotFoundPage'
import { OrdersPage } from '@/pages/OrdersPage'
import { PolicyPage } from '@/pages/PolicyPage'
import { WorkbenchPage } from '@/pages/WorkbenchPage'

/**
 * 路由表
 * /login            登录（无外壳）
 * /                 车险入口
 * /chat             办理会话（聊天）
 * /orders           订单
 * /policy           保单（/policy/:policyId 预留）
 * /workbench        业务员工作台（仅业务员）
 * /handoff          人工接管
 */
export const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  {
    element: <RequireAuth />,
    children: [
      {
        element: <AppLayout />,
        children: [
          { index: true, element: <EntryPage /> },
          { path: 'chat', element: <ChatPage /> },
          { path: 'orders', element: <OrdersPage /> },
          { path: 'policy', element: <PolicyPage /> },
          { path: 'policy/:policyId', element: <PolicyPage /> },
          { path: 'handoff', element: <HandoffPage /> },
          {
            path: 'workbench',
            element: <RequireRole role="agent" />,
            children: [{ index: true, element: <WorkbenchPage /> }],
          },
          { path: '*', element: <NotFoundPage /> },
        ],
      },
    ],
  },
])
