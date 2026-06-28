// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { toneColor } from '@/lib/tone'
import type { KpiDatum } from '@/mock/types'
import { Sparkline } from './Sparkline'

export function Kpi({ datum }: { datum: KpiDatum }) {
    return (
        <div className="card" style={{ padding: 16, flex: 1, minWidth: 0 }}>
            <div
                style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'flex-start',
                    gap: 12,
                }}
            >
                <div style={{ minWidth: 0 }}>
                    <div
                        style={{
                            fontSize: 11,
                            color: 'var(--text-3)',
                            textTransform: 'uppercase',
                            letterSpacing: '0.06em',
                            marginBottom: 8,
                        }}
                    >
                        {datum.label}
                    </div>
                    <div
                        className="mono tnum"
                        style={{ fontSize: 22, fontWeight: 500, letterSpacing: '-0.02em' }}
                    >
                        {datum.value}
                    </div>
                    <div
                        style={{
                            fontSize: 11,
                            marginTop: 6,
                            display: 'flex',
                            alignItems: 'center',
                            gap: 6,
                        }}
                    >
                        {datum.sub.map((segment, i) => (
                            <span key={i} style={{ color: toneColor(segment.tone) }}>
                                {segment.text}
                            </span>
                        ))}
                    </div>
                </div>
                {datum.spark && (
                    <Sparkline data={datum.spark} color={datum.sparkColor ?? 'var(--accent)'} />
                )}
            </div>
        </div>
    )
}
