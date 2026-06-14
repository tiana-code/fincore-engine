// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { Icon } from '@/components/Icon'
import { toneColor } from '@/lib/tone'
import type { ActivityEvent } from '@/mock/types'

export function ActivityFeed({ activity }: { activity: ActivityEvent[] }) {
  return (
    <div className="card" style={{ display: 'flex', flexDirection: 'column' }}>
      <div style={{ padding: '14px 18px', borderBottom: '1px solid var(--border)', display: 'flex', alignItems: 'center', gap: 8 }}>
        <span style={{ fontSize: 13, fontWeight: 500 }}>Recent activity</span>
        <span style={{ width: 6, height: 6, borderRadius: '50%', background: 'var(--accent)', boxShadow: '0 0 0 4px rgba(20,184,166,0.15)' }} />
        <span className="mono" style={{ fontSize: 10.5, color: 'var(--text-3)' }}>live · last 12m</span>
        <div style={{ flex: 1 }} />
        <span style={{ fontSize: 11, color: 'var(--text-2)' }}>View all</span>
      </div>
      <div style={{ padding: '6px 18px 14px' }}>
        {activity.map((event, i) => (
          <div
            key={`${event.type}-${i}`}
            style={{
              display: 'grid',
              gridTemplateColumns: '22px 1fr 80px 14px',
              alignItems: 'center',
              gap: 12,
              padding: '10px 0',
              borderBottom: i < activity.length - 1 ? '1px solid var(--border)' : 'none',
            }}
          >
            <span style={{ color: toneColor(event.tone), display: 'inline-flex' }}>
              <Icon name={event.icon} size={14} />
            </span>
            <div style={{ minWidth: 0 }}>
              <div className="mono" style={{ fontSize: 11.5, color: toneColor(event.tone) }}>{event.type}</div>
              <div
                className="mono"
                style={{ fontSize: 11, color: 'var(--text-2)', marginTop: 2, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}
              >
                {event.detail}
              </div>
            </div>
            <span className="mono" style={{ fontSize: 10.5, color: 'var(--text-3)', textAlign: 'right' }}>{event.ts}</span>
            <Icon name="chevRight" size={12} />
          </div>
        ))}
      </div>
    </div>
  )
}
