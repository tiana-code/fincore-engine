// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { useMemo } from 'react'
import { ThemeToggle } from '@/components/ThemeToggle'
import { decodeJwtClaims } from '@/lib/jwt'

export type OverviewMode = 'live' | 'loading' | 'offline'

const CHIP: Record<OverviewMode, { color: string; label: string }> = {
    live: { color: 'var(--accent)', label: 'API live' },
    loading: { color: 'var(--text-3)', label: 'connecting' },
    offline: { color: 'var(--amber)', label: 'API offline' },
}

export function OverviewTopBar({ mode }: { mode: OverviewMode }) {
    const claims = useMemo(() => decodeJwtClaims(import.meta.env.VITE_DEV_BEARER_TOKEN), [])
    const chip = CHIP[mode]
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
                <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                    <span style={{ fontSize: 17, fontWeight: 500 }}>Sandbox</span>
                    <span style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 11 }}>
                        <span
                            style={{
                                width: 7,
                                height: 7,
                                borderRadius: '50%',
                                background: chip.color,
                            }}
                        />
                        <span className="mono" style={{ color: 'var(--text-2)' }}>
                            {chip.label}
                        </span>
                    </span>
                </div>
                {claims?.username && (
                    <div style={{ fontSize: 11.5, color: 'var(--text-3)', marginTop: 2 }}>
                        Welcome back, {claims.username}
                        {claims.tenant && (
                            <>
                                {' · tenant '}
                                <span className="mono">{claims.tenant}</span>
                            </>
                        )}
                    </div>
                )}
            </div>
            <div style={{ flex: 1 }} />
            <ThemeToggle />
        </div>
    )
}
