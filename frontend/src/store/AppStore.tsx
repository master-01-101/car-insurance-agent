import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import type { AuthUser, Conversation, FlowStageKey, UserRole } from '@/types'
import { SAMPLE_CONVERSATIONS } from '@/data/samples'

/**
 * 全局状态：登录身份、当前会话（按车牌号）、当前车辆/阶段、移动端导航。
 *
 * 说明：登录态目前只做本地演示（localStorage），真实 token 与角色判定
 * 由 POST /api/auth/login 下发，见 src/api/index.ts → authApi.login。
 */

const AUTH_STORAGE_KEY = 'chexian.auth.demo'

function readStoredUser(): AuthUser | null {
  try {
    const raw = localStorage.getItem(AUTH_STORAGE_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw) as AuthUser
    if (!parsed?.userId || (parsed.role !== 'user' && parsed.role !== 'agent')) {
      return null
    }
    return parsed
  } catch {
    return null
  }
}

interface StoreValue {
  user: AuthUser | null
  isAgent: boolean
  conversations: Conversation[]
  activeConversationId: string
  activeConversation: Conversation | null
  activeStage: FlowStageKey
  mobileNavOpen: boolean
  login: (role: UserRole, account: string) => AuthUser
  logout: () => void
  setActiveConversation: (conversationId: string) => void
  setMobileNavOpen: (open: boolean) => void
}

const StoreContext = createContext<StoreValue | null>(null)

export function AppStoreProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(() => readStoredUser())
  const [conversations] = useState<Conversation[]>(SAMPLE_CONVERSATIONS)
  const [activeConversationId, setActiveConversationId] = useState<string>(
    SAMPLE_CONVERSATIONS[0]?.conversationId ?? '',
  )
  const [mobileNavOpen, setMobileNavOpen] = useState(false)

  useEffect(() => {
    if (user) {
      localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(user))
    } else {
      localStorage.removeItem(AUTH_STORAGE_KEY)
    }
  }, [user])

  const login = useCallback((role: UserRole, account: string): AuthUser => {
    const nextUser: AuthUser = {
      userId: account.trim() || (role === 'agent' ? 'demo-agent' : 'demo-user'),
      userName: account.trim() || (role === 'agent' ? '演示业务员' : '演示用户'),
      role,
      agentId: undefined,
    }
    setUser(nextUser)
    return nextUser
  }, [])

  const logout = useCallback(() => {
    setUser(null)
    localStorage.removeItem(AUTH_STORAGE_KEY)
  }, [])

  const setActiveConversation = useCallback((conversationId: string) => {
    setActiveConversationId(conversationId)
    setMobileNavOpen(false)
  }, [])

  const activeConversation = useMemo(
    () =>
      conversations.find((item) => item.conversationId === activeConversationId) ?? null,
    [conversations, activeConversationId],
  )

  const value = useMemo<StoreValue>(
    () => ({
      user,
      isAgent: user?.role === 'agent',
      conversations,
      activeConversationId,
      activeConversation,
      activeStage: activeConversation?.currentStage ?? 'collect',
      mobileNavOpen,
      login,
      logout,
      setActiveConversation,
      setMobileNavOpen,
    }),
    [
      user,
      conversations,
      activeConversationId,
      activeConversation,
      mobileNavOpen,
      login,
      logout,
      setActiveConversation,
    ],
  )

  return <StoreContext.Provider value={value}>{children}</StoreContext.Provider>
}

export function useAppStore(): StoreValue {
  const value = useContext(StoreContext)
  if (!value) {
    throw new Error('useAppStore 必须在 <AppStoreProvider> 内使用')
  }
  return value
}

/** 角色中文名 */
export function roleLabel(role: UserRole): string {
  return role === 'agent' ? '业务员' : '普通用户'
}
