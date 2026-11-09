import { useNavigate } from 'react-router-dom'
import { Button, Result } from 'antd'

export function NotFoundPage() {
  const navigate = useNavigate()

  return (
    <div className="login-shell">
      <Result
        status="404"
        title="页面不存在"
        subTitle="该路由未在本版本实现，或链接已失效。"
        extra={
          <Button type="primary" onClick={() => navigate('/')}>
            返回车险入口
          </Button>
        }
      />
    </div>
  )
}

export function ForbiddenPage() {
  const navigate = useNavigate()

  return (
    <div className="login-shell">
      <Result
        status="403"
        title="无访问权限"
        subTitle="业务员工作台仅对业务员角色开放。请退出后以业务员身份登录。"
        extra={
          <Button type="primary" onClick={() => navigate('/')}>
            返回车险入口
          </Button>
        }
      />
    </div>
  )
}
