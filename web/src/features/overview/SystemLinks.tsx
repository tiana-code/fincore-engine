// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { Icon } from '@/components/Icon'
import type { SystemLink } from '@/mock/types'

export function SystemLinks({ links }: { links: SystemLink[] }) {
  return (
    <div className="grid grid-cols-1 gap-[18px] md:grid-cols-3">
      {links.map((link) => (
        <div key={link.title} className="card" style={{ padding: 16, display: 'flex', alignItems: 'center', gap: 14 }}>
          <span
            style={{
              width: 36,
              height: 36,
              borderRadius: 8,
              background: 'var(--bg-2)',
              border: '1px solid var(--border)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: 'var(--accent)',
            }}
          >
            <Icon name={link.icon} size={16} />
          </span>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontSize: 13, fontWeight: 500 }}>{link.title}</div>
            <div className="mono" style={{ fontSize: 10.5, color: 'var(--text-3)', marginTop: 2 }}>{link.url}</div>
            <div style={{ fontSize: 11, color: 'var(--text-2)', marginTop: 4 }}>{link.sub}</div>
          </div>
          <Icon name="chevRight" size={14} />
        </div>
      ))}
    </div>
  )
}
