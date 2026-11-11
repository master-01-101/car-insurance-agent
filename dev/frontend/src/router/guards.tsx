import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAppStore } from '@/store/AppStore'
import { ForbiddenPage } from '@/pages/NotFoundPage'
import type { UserRole } from '@/types'

/**
 * 路由守卫（当前基于本地演示身份）。
 * 真实鉴权导入后，这里应改为校验后端返回的 token 与角色。
 */
export function RequireAuth() {
  const { user } = useAppStore()
  const location = useLocation()

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }
  return <Outlet />
}

/** 角色守卫：例如业务员工作台只允许 role === 'agent' */
export function RequireRole({ role }: { role: UserRole }) {
  const { user } = useAppStore()

  if (!user) {
    return <Navigate to="/login" replace />
  }
  if (user.role !== role) {
    return <ForbiddenPage />
  }
  return <Outlet />
}
