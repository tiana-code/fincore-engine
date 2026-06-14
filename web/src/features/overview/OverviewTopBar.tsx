// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { ThemeToggle } from '@/components/ThemeToggle'
import type { SandboxStatus, ServiceHealth } from '@/mock/types'

interface Props {
  status: SandboxStatus
  services: ServiceHealth[]
}

export function OverviewTopBar({ status, services }: Props) {
  return (
    <div
      style={{
        padding: '14px 24px',
        borderBottom: '1px solid var(--border)',
        background: 'var(--bg-2)',
        display: 'flex',
        alignItems: 'center',
        gap: 14,
        flexWrap: 'wrap',
      }}
    >
      <div>
        <div style={{ display: 'flex', alignItems: 'baseline', gap: 10 }}>
          <span style={{ fontSize: 17, fontWeight: 500 }}>{status.title}</span>
          <span className="mono" style={{ fontSize: 11, color: 'var(--text-3)' }}>
            {status.version} · build {status.build} · uptime {status.uptime}
          </span>
        </div>
        <div style={{ fontSize: 11.5, color: 'var(--text-3)', marginTop: 2 }}>
          Welcome back, {status.user} · tenant <span className="mono">{status.tenant}</span>
        </div>
      </div>
      <div style={{ flex: 1 }} />
      {services.map((s) => (
        <div key={s.name} style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 11 }}>
          <span style={{ width: 7, height: 7, borderRadius: '50%', background: s.ok ? 'var(--accent)' : 'var(--debit)' }} />
          <span className="mono" style={{ color: 'var(--text-2)' }}>{s.name}</span>
          <span className="mono" style={{ color: 'var(--text-3)', fontSize: 10 }}>{s.version}</span>
        </div>
      ))}
      <ThemeToggle />
    </div>
  )
}
