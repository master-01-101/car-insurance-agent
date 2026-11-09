import { useParams } from 'react-router-dom'
import { Button, Tag } from 'antd'
import { PageHeader } from '@/components/PageHeader'
import { PlaceholderBlock } from '@/components/PlaceholderBlock'
import { useNotImported } from '@/components/useNotImported'
import { SAMPLE_POLICY } from '@/data/samples'
import { formatCoverage, formatMoney } from '@/utils/format'

function Field({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <div className="policy-field-label">{label}</div>
      <div className="policy-field-value">{value}</div>
    </div>
  )
}

/**
 * 保单明细页（第七阶段）。
 * 只有后端确认支付成功后才允许展示「已出单」，因此本页仅为文档骨架占位。
 */
export function PolicyPage() {
  const { policyId } = useParams<{ policyId: string }>()
  const notImported = useNotImported()
  const policy = SAMPLE_POLICY

  return (
    <div className="page">
      <div className="page-scroll">
        <PageHeader
          eyebrow="第七阶段 · 保单"
          title="保单明细"
          subtitle="保单由后端在支付成功后生成；未确认支付成功前，页面不会出现已支付或已出单状态。"
          actions={
            <>
              <Button size="small" onClick={() => notImported('payment')}>
                支付状态
              </Button>
              <Button size="small" onClick={() => notImported('policy')}>
                下载 PDF
              </Button>
            </>
          }
        />

        <div className="panel policy-sheet">
          <div className="policy-head">
            <div>
              <div className="eyebrow">保险单</div>
              <div style={{ fontSize: 17, fontWeight: 600, letterSpacing: '-0.02em', marginTop: 2 }}>
                机动车交通事故责任强制保险 + 商业保险（示例）
              </div>
              <div className="policy-no">
                保单号 {policy.policyNo}
                {policyId ? ` · 路由参数 ${policyId}` : ''}
              </div>
            </div>
            <Tag
              bordered={false}
              style={{ background: 'var(--color-muted)', color: 'var(--color-muted-foreground)' }}
            >
              静态占位
            </Tag>
          </div>

          <div className="policy-body">
            <section>
              <div className="policy-section-title">投保人与被保人</div>
              <div className="policy-grid">
                <Field label="投保人" value={policy.applicant.name} />
                <Field label="证件类型" value={policy.applicant.idType ?? '—'} />
                <Field label="证件号码（脱敏）" value={policy.applicant.idMasked ?? '—'} />
                <Field label="被保人" value={policy.insured.name} />
                <Field label="证件类型" value={policy.insured.idType ?? '—'} />
                <Field label="证件号码（脱敏）" value={policy.insured.idMasked ?? '—'} />
              </div>
            </section>

            <section>
              <div className="policy-section-title">车辆信息</div>
              <div className="policy-grid">
                <Field label="号牌号码" value={policy.vehicle.plateNumber ?? '—'} />
                <Field label="车辆识别代号" value={policy.vehicle.vin ?? '—'} />
                <Field label="发动机号码" value={policy.vehicle.engineNo ?? '—'} />
                <Field label="所有人" value={policy.vehicle.ownerName ?? '—'} />
                <Field label="注册日期" value={policy.vehicle.registerDate ?? '—'} />
                <Field label="使用性质" value={policy.vehicle.usageNature ?? '—'} />
              </div>
            </section>

            <section>
              <div className="policy-section-title">险种与花费</div>
              <table className="plan-table" style={{ border: '1px solid var(--border-subtle)', borderRadius: 12 }}>
                <thead>
                  <tr>
                    <th scope="col">险种</th>
                    <th scope="col" style={{ textAlign: 'right' }}>
                      保额
                    </th>
                    <th scope="col" style={{ textAlign: 'right' }}>
                      保费
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {policy.items.map((item) => (
                    <tr key={item.itemId}>
                      <td>{item.coverageName}</td>
                      <td className="num">{formatCoverage(item.coverageAmount)}</td>
                      <td className="num">¥{formatMoney(item.premium)}</td>
                    </tr>
                  ))}
                  <tr>
                    <td style={{ fontWeight: 600 }}>合计</td>
                    <td className="num">—</td>
                    <td className="num" style={{ fontWeight: 600 }}>
                      ¥{formatMoney(policy.totalPremium)}
                    </td>
                  </tr>
                </tbody>
              </table>
            </section>

            <section>
              <div className="policy-section-title">保障期间</div>
              <div className="policy-grid">
                <Field label="生效日期" value={policy.effectiveFrom} />
                <Field label="截止日期" value={policy.effectiveTo} />
                <Field label="保费合计" value={`¥${formatMoney(policy.totalPremium)}`} />
              </div>
            </section>

            <section>
              <div className="policy-section-title">后续维护条款</div>
              <ul style={{ margin: 0, paddingLeft: 18, fontSize: 12.5, lineHeight: 1.9, color: 'var(--color-muted-foreground)' }}>
                {(policy.terms ?? []).map((term) => (
                  <li key={term}>{term}</li>
                ))}
              </ul>
            </section>
          </div>
        </div>

        <PlaceholderBlock id="policy" />
      </div>
    </div>
  )
}
