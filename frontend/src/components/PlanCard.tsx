import { Button, Space, Tag } from 'antd'
import type { PlanPayload } from '@/types'
import { formatCoverage, formatMoney } from '@/utils/format'
import { useNotImported } from './useNotImported'

/**
 * 保险方案卡片。
 * 数据必须来自后端 / Agent（planApi.recommend / revise），
 * 前端不计算保费，也不判断方案是否被接受。
 */
export function PlanCard({ plan }: { plan: PlanPayload }) {
  const notImported = useNotImported()

  return (
    <div className="plan-card" aria-label="保险方案卡片">
      <div className="plan-head">
        <div>
          <div className="plan-title">推荐方案 · {plan.version}</div>
          <div className="plan-sub">
            {plan.insuranceCompany} · {plan.items.length} 个险种
          </div>
        </div>
        <Space size={6} wrap>
          <Tag bordered={false} style={{ background: 'var(--color-muted)', color: 'var(--color-muted-foreground)' }}>
            静态占位
          </Tag>
          <Tag color="blue" bordered={false}>
            Agent 生成
          </Tag>
        </Space>
      </div>

      <table className="plan-table">
        <thead>
          <tr>
            <th scope="col">险种</th>
            <th scope="col" style={{ textAlign: 'right' }}>
              保额
            </th>
            <th scope="col" style={{ textAlign: 'right' }}>
              保费
            </th>
            <th scope="col" style={{ width: 72 }}>
              属性
            </th>
          </tr>
        </thead>
        <tbody>
          {plan.items.map((item) => (
            <tr key={item.itemId}>
              <td>{item.coverageName}</td>
              <td className="num">{formatCoverage(item.coverageAmount)}</td>
              <td className="num">¥{formatMoney(item.premium)}</td>
              <td>
                <span style={{ fontSize: 11, color: 'var(--color-muted-foreground)' }}>
                  {item.required ? '必选' : '可选'}
                </span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <div className="plan-foot">
        <div className="plan-total">
          保费合计
          <strong>¥{formatMoney(plan.totalPremium)}</strong>
        </div>
        <Space size={8}>
          <Button size="small" onClick={() => notImported('plan-revise')}>
            调整方案
          </Button>
          <Button size="small" type="primary" onClick={() => notImported('plan-revise')}>
            确认方案
          </Button>
        </Space>
      </div>

      <div className="plan-note">
        金额与版本号以服务端返回为准；可直接用自然语言修改，例如「平安、交强险、三者调到 200 万」。
        改方案接口未导入，当前卡片为静态占位。
      </div>
    </div>
  )
}
