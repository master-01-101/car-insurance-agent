import type { ReactNode } from 'react'
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { Avatar, Button, Drawer, Tag, Tooltip } from 'antd'
import {
  AppstoreOutlined,
  CustomerServiceOutlined,
  FileTextOutlined,
  LogoutOutlined,
  MenuOutlined,
  MessageOutlined,
  SafetyCertificateOutlined,
  TeamOutlined,
} from '@ant-design/icons'
import { BrandMark } from '@/components/BrandMark'
import { ConversationList } from '@/components/ConversationList'
import { roleLabel, useAppStore } from '@/store/AppStore'

interface NavItem {
  to: string
  label: string
  icon: ReactNode
  end?: boolean
  agentOnly?: boolean
  tail?: string
}

const NAV_ITEMS: NavItem[] = [
  { to: '/', label: '车险入口', icon: <AppstoreOutlined />, end: true },
  { to: '/chat', label: '办理会话', icon: <MessageOutlined /> },
  { to: '/orders', label: '订单', icon: <FileTextOutlined /> },
  { to: '/policy', label: '保单', icon: <SafetyCertificateOutlined /> },
  { to: '/workbench', label: '业务员工作台', icon: <TeamOutlined />, agentOnly: true, tail: '批量' },
  { to: '/handoff', label: '人工接管', icon: <CustomerServiceOutlined /> },
]

const ROUTE_TITLES: Record<string, string> = {
  '/': '车险入口',
  '/chat': '办理会话',
  '/orders': '订单',
  '/policy': '保单',
  '/workbench': '业务员工作台',
  '/handoff': '人工接管',
}

function resolveTitle(pathname: string): string {
  if (pathname.startsWith('/policy')) return '保单'
  return ROUTE_TITLES[pathname] ?? '车险出单智能体'
}

function SidebarContent() {
  const { user, isAgent, logout } = useAppStore()
  const navigate = useNavigate()
  const items = NAV_ITEMS.filter((item) => !item.agentOnly || isAgent)

  return (
    <>
      <Link to="/" className="brand" aria-label="返回车险入口">
        <span className="brand-mark">
          <BrandMark size={19} />
        </span>
        <span className="brand-text">
          <div className="brand-name">车险出单智能体</div>
          <div className="brand-sub">Frontend UI Skeleton</div>
        </span>
      </Link>

      <nav className="nav" aria-label="主导航">
        {items.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.end}
            className={({ isActive }) => `nav-item${isActive ? ' nav-item--active' : ''}`}
          >
            <span className="nav-icon" aria-hidden="true">
              {item.icon}
            </span>
            <span className="nav-label">{item.label}</span>
            {item.tail && <span className="nav-tail">{item.tail}</span>}
          </NavLink>
        ))}
      </nav>

      <ConversationList />

      <div className="side-footer">
        <div className="identity">
          <Avatar
            size={28}
            style={{
              background: isAgent ? 'var(--color-secondary)' : 'var(--color-accent)',
              fontSize: 12,
              flex: '0 0 28px',
            }}
          >
            {isAgent ? '业' : '用'}
          </Avatar>
          <div style={{ minWidth: 0 }}>
            <div className="identity-name">{user?.userName ?? '未登录'}</div>
            <div className="identity-role">
              {user ? roleLabel(user.role) : '—'} ·{' '}
              {user?.agentId ? `业务员 ${user.agentId}` : '业务员 ID 待下发'}
            </div>
          </div>
        </div>
        <Tooltip title="退出登录">
          <Button
            size="small"
            type="text"
            icon={<LogoutOutlined />}
            aria-label="退出登录"
            onClick={() => {
              logout()
              navigate('/login')
            }}
          />
        </Tooltip>
      </div>
    </>
  )
}

/**
 * 应用外壳：左侧导航 + 会话列表，顶部信息条，中间内容区。
 * 后端未接入，因此外壳只展示「UI 骨架 / 演示数据」两类标记。
 */
export function AppLayout() {
  const { isAgent, mobileNavOpen, setMobileNavOpen } = useAppStore()
  const location = useLocation()

  return (
    <div className="app-shell">
      <aside className="app-sidebar" aria-label="侧边栏">
        <SidebarContent />
      </aside>

      <Drawer
        open={mobileNavOpen}
        onClose={() => setMobileNavOpen(false)}
        placement="left"
        width={288}
        title={null}
        styles={{
          body: {
            padding: 0,
            display: 'flex',
            flexDirection: 'column',
            background: 'var(--surface-solid)',
          },
        }}
      >
        <SidebarContent />
      </Drawer>

      <div className="app-main">
        <header className="app-topbar">
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, minWidth: 0 }}>
            <Button
              className="mobile-only"
              size="small"
              type="text"
              icon={<MenuOutlined />}
              aria-label="打开导航"
              onClick={() => setMobileNavOpen(true)}
            />
            <div className="crumb">
              <span>车险出单智能体</span>
              <span aria-hidden="true">/</span>
              <span className="crumb-strong">{resolveTitle(location.pathname)}</span>
            </div>
          </div>

          <div className="topbar-right">
            <Tag
              bordered={false}
              style={{ background: 'var(--color-muted)', color: 'var(--color-muted-foreground)' }}
            >
              UI 骨架 · v0.1
            </Tag>
            <Tag color={isAgent ? 'blue' : 'default'} bordered={false}>
              {isAgent ? '业务员视图' : '用户视图'}
            </Tag>
          </div>
        </header>

        <main className="app-content">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
