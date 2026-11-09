/** 金额：¥12,280.00 */
export function formatMoney(value: number | null | undefined): string {
  if (value === null || value === undefined || Number.isNaN(value)) return '—'
  return new Intl.NumberFormat('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value)
}

/** 保额：12.2 万 / 2,000 元 */
export function formatCoverage(value: number | null | undefined): string {
  if (value === null || value === undefined) return '—'
  if (value >= 10000) {
    const wan = Number((value / 10000).toFixed(1))
    return `${wan.toLocaleString('zh-CN')} 万`
  }
  return `${value.toLocaleString('zh-CN')} 元`
}

/** 时间：11-09 10:12 */
export function formatTime(iso: string | undefined): string {
  if (!iso) return '—'
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return '—'
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

/** 证件号脱敏：只保留前 4 位与后 4 位 */
export function maskIdNo(value: string | undefined): string {
  if (!value) return '—'
  if (value.length <= 8) return `${value.slice(0, 1)}****`
  return `${value.slice(0, 4)}${'*'.repeat(Math.max(6, value.length - 8))}${value.slice(-4)}`
}
