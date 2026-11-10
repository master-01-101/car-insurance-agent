import type { ReactNode } from 'react'

interface PageHeaderProps {
  eyebrow?: string
  title: string
  subtitle?: string
  actions?: ReactNode
}

export function PageHeader({ eyebrow, title, subtitle, actions }: PageHeaderProps) {
  return (
    <div className="page-head">
      <div style={{ minWidth: 0 }}>
        {eyebrow && <div className="eyebrow">{eyebrow}</div>}
        <h1 className="page-title">{title}</h1>
        {subtitle && <p className="page-sub">{subtitle}</p>}
      </div>
      {actions && <div className="page-actions">{actions}</div>}
    </div>
  )
}
