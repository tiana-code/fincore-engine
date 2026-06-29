// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { Icon } from '@/components/Icon'
import { toneColor } from '@/lib/tone'
import type { ActivityEvent } from '@/mock/types'

export type ActivityState = 'ready' | 'loading' | 'offline'

const MESSAGE: Record<Exclude<ActivityState, 'ready'>, string> = {
    loading: 'Loading recent activity...',
    offline: 'Sandbox API not reachable.',
}

export function ActivityFeed({
    activity,
    state,
}: {
    activity: ActivityEvent[]
    state: ActivityState
}) {
    const placeholder =
        state !== 'ready'
            ? MESSAGE[state]
            : activity.length === 0
              ? 'No recent ledger activity.'
              : null
    return (
        <div className="card" style={{ display: 'flex', flexDirection: 'column' }}>
            <div
                style={{
                    padding: '14px 18px',
                    borderBottom: '1px solid var(--border)',
                    display: 'flex',
                    alignItems: 'center',
                    gap: 8,
                }}
            >
                <span style={{ fontSize: 13, fontWeight: 500 }}>Recent ledger activity</span>
            </div>
            <div style={{ padding: '6px 18px 14px' }}>
                {placeholder ? (
                    <div style={{ padding: '18px 0', fontSize: 12, color: 'var(--text-3)' }}>
                        {placeholder}
                    </div>
                ) : (
                    activity.map((event, i) => (
                        <div
                            key={event.detail}
                            style={{
                                display: 'grid',
                                gridTemplateColumns: '22px 1fr 80px',
                                alignItems: 'center',
                                gap: 12,
                                padding: '10px 0',
                                borderBottom:
                                    i < activity.length - 1 ? '1px solid var(--border)' : 'none',
                            }}
                        >
                            <span style={{ color: toneColor(event.tone), display: 'inline-flex' }}>
                                <Icon name={event.icon} size={14} />
                            </span>
                            <div style={{ minWidth: 0 }}>
                                <div
                                    className="mono"
                                    style={{ fontSize: 11.5, color: toneColor(event.tone) }}
                                >
                                    {event.type}
                                </div>
                                <div
                                    className="mono"
                                    style={{
                                        fontSize: 11,
                                        color: 'var(--text-2)',
                                        marginTop: 2,
                                        overflow: 'hidden',
                                        textOverflow: 'ellipsis',
                                        whiteSpace: 'nowrap',
                                    }}
                                >
                                    {event.detail}
                                </div>
                            </div>
                            <span
                                className="mono"
                                style={{
                                    fontSize: 10.5,
                                    color: 'var(--text-3)',
                                    textAlign: 'right',
                                }}
                            >
                                {event.ts}
                            </span>
                        </div>
                    ))
                )}
            </div>
        </div>
    )
}
