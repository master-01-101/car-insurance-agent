import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { Button, Form, Input, Segmented, Tag } from 'antd'
import { CheckCircleOutlined, LockOutlined, UserOutlined } from '@ant-design/icons'
import { BrandMark } from '@/components/BrandMark'
import { useNotImported } from '@/components/useNotImported'
import { useAppStore } from '@/store/AppStore'
import type { UserRole } from '@/types'

interface LoginValues {
  account: string
  password?: string
}

/**
 * 登录页。
 * 真实鉴权（账号校验、token 下发、角色判定）未导入：
 * 这里的「登录」只在本地写入演示身份，不发送任何请求。
 */
export function LoginPage() {
  const [role, setRole] = useState<UserRole>('user')
  const [form] = Form.useForm<LoginValues>()
  const { login } = useAppStore()
  const navigate = useNavigate()
  const location = useLocation()
  const notImported = useNotImported()

  const from = (location.state as { from?: string } | null)?.from ?? '/'

  const handleFinish = (values: LoginValues) => {
    login(role, values.account ?? '')
    navigate(from.startsWith('/login') ? '/' : from, { replace: true })
  }

  return (
    <div className="login-shell">
      <div className="login-card">
        <section className="login-brand">
          <div className="login-mark">
            <BrandMark size={22} />
          </div>

          <div>
            <h1 className="login-title">车险出单智能体</h1>
            <p className="login-tagline">
              用对话完成车辆信息收集、方案推荐、报价核保与支付出单。
              本版本只交付前端 UI 骨架，后端与 Agent 能力一律以占位符标记。
            </p>
          </div>

          <ul className="login-points">
            <li>
              <CheckCircleOutlined />
              <span>新用户上传行驶证正副页自动 OCR；老用户凭车牌号或车架号续保</span>
            </li>
            <li>
              <CheckCircleOutlined />
              <span>方案支持自然语言修改，价格与状态始终以服务端返回为准</span>
            </li>
            <li>
              <CheckCircleOutlined />
              <span>会话标题取车牌号，第二个车牌自动开启新会话</span>
            </li>
          </ul>

          <div>
            <Tag
              bordered={false}
              style={{
                background: 'rgba(255,255,255,0.1)',
                color: '#e2e8f0',
                border: '1px solid rgba(255,255,255,0.16)',
              }}
            >
              静态占位 · 未接入后端
            </Tag>
          </div>
        </section>

        <section className="login-panel">
          <div>
            <h2 className="login-panel-title">登录</h2>
            <p className="login-panel-sub">选择身份进入对应工作台，业务员可使用批量操作。</p>
          </div>

          <Segmented
            block
            value={role}
            onChange={(value) => setRole(value as UserRole)}
            options={[
              { label: '普通用户', value: 'user' },
              { label: '业务员', value: 'agent' },
            ]}
          />

          <Form<LoginValues>
            form={form}
            layout="vertical"
            requiredMark={false}
            onFinish={handleFinish}
            initialValues={{ account: '', password: '' }}
          >
            <Form.Item
              name="account"
              label="账号"
              rules={[{ required: true, message: '请输入账号（演示环境任意填写）' }]}
            >
              <Input
                prefix={<UserOutlined />}
                placeholder="演示环境任意填写"
                autoComplete="username"
                size="large"
              />
            </Form.Item>

            <Form.Item name="password" label="密码">
              <Input.Password
                prefix={<LockOutlined />}
                placeholder="鉴权接口未导入，密码不会被校验"
                autoComplete="current-password"
                size="large"
              />
            </Form.Item>

            <Button type="primary" htmlType="submit" block size="large">
              登录
            </Button>
          </Form>

          <div className="ph-inline" role="note">
            <span className="ph-badge">此地方未导入</span>
            <span>登录鉴权与 token 下发（成员 B 后端）</span>
          </div>

          <div className="login-foot">
            <span>业务员 ID 由登录接口下发，当前为空</span>
            <Button
              type="link"
              size="small"
              style={{ padding: 0 }}
              onClick={() => notImported('auth-login')}
            >
              查看占位说明
            </Button>
          </div>
        </section>
      </div>
    </div>
  )
}
