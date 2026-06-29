// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { Icon } from '@/components/Icon'
import type { ApiExample } from '@/mock/types'

export function TryTheApi({ examples }: { examples: ApiExample[] }) {
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
                <span style={{ fontSize: 13, fontWeight: 500 }}>Try the API</span>
                <span className="mono" style={{ fontSize: 10.5, color: 'var(--text-3)' }}>
                    · in-page playground
                </span>
            </div>
            <div className="grid grid-cols-1 gap-3 p-[18px] sm:grid-cols-2" style={{ flex: 1 }}>
                {examples.map((example) => (
                    <div
                        key={example.title}
                        style={{
                            padding: 14,
                            border: '1px solid var(--border)',
                            borderRadius: 6,
                            background: 'var(--bg-2)',
                            display: 'flex',
                            flexDirection: 'column',
                            gap: 10,
                        }}
                    >
                        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                            <span
                                style={{
                                    width: 28,
                                    height: 28,
                                    borderRadius: 6,
                                    background: 'var(--surface)',
                                    border: '1px solid var(--border)',
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    color: 'var(--accent)',
                                }}
                            >
                                <Icon name={example.icon} size={14} />
                            </span>
                            <div style={{ flex: 1 }}>
                                <div style={{ fontSize: 12.5, fontWeight: 500 }}>
                                    {example.title}
                                </div>
                                <div
                                    className="mono"
                                    style={{ fontSize: 10.5, color: 'var(--text-3)', marginTop: 2 }}
                                >
                                    {example.endpoint}
                                </div>
                            </div>
                        </div>
                        <pre
                            className="mono"
                            style={{
                                margin: 0,
                                padding: 10,
                                background: 'var(--bg)',
                                border: '1px solid var(--border)',
                                borderRadius: 4,
                                fontSize: 10.5,
                                color: 'var(--text-2)',
                                lineHeight: 1.5,
                                whiteSpace: 'pre-wrap',
                            }}
                        >
                            {example.curl}
                        </pre>
                    </div>
                ))}
            </div>
        </div>
    )
}
