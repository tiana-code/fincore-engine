// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { Link } from 'react-router-dom'
import { Icon, type IconName } from './Icon'

interface NavItem {
    name: string
    icon: IconName
    path?: string
}

const NAV: NavItem[] = [
    { name: 'Overview', icon: 'home', path: '/' },
    { name: 'Accounts', icon: 'wallet', path: '/accounts' },
    { name: 'Transactions', icon: 'arrows', path: '/transactions' },
    { name: 'Payments', icon: 'send', path: '/payments' },
    { name: 'Decisions', icon: 'scale', path: '/decisions' },
    { name: 'Compliance', icon: 'shield', path: '/compliance/cases' },
    { name: 'Audit', icon: 'book' },
]

const SERVICES = ['postgres', 'redpanda', 'keycloak', 'redis']

export function Sidebar({ active = 'Overview' }: { active?: string }) {
    return (
        <div
            style={{
                width: 240,
                height: '100%',
                background: 'var(--bg-2)',
                borderRight: '1px solid var(--border)',
                display: 'flex',
                flexDirection: 'column',
                flex: '0 0 auto',
            }}
        >
            <div
                style={{
                    padding: '18px 18px 16px',
                    borderBottom: '1px solid var(--border)',
                    display: 'flex',
                    alignItems: 'center',
                    gap: 10,
                }}
            >
                <div
                    style={{
                        width: 26,
                        height: 26,
                        borderRadius: 6,
                        background: 'linear-gradient(135deg, var(--accent), var(--text-accent))',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        color: '#03201D',
                        fontWeight: 700,
                        fontSize: 13,
                        fontFamily: 'var(--mono)',
                    }}
                >
                    F
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', lineHeight: 1.15 }}>
                    <span style={{ fontWeight: 600, fontSize: 13 }}>FinCore</span>
                    <span className="mono" style={{ fontSize: 10, color: 'var(--text-3)' }}>
                        v0.4.0 · sandbox
                    </span>
                </div>
            </div>

            <div style={{ padding: '12px 12px 8px' }}>
                <div
                    style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: 8,
                        height: 28,
                        padding: '0 10px',
                        background: 'var(--bg)',
                        border: '1px solid var(--border)',
                        borderRadius: 4,
                        color: 'var(--text-3)',
                        fontSize: 12,
                    }}
                >
                    <Icon name="search" size={13} />
                    <span style={{ flex: 1 }}>Jump to...</span>
                    <span className="kbd">Cmd K</span>
                </div>
            </div>

            <nav
                style={{
                    padding: '4px 8px',
                    flex: 1,
                    display: 'flex',
                    flexDirection: 'column',
                    gap: 1,
                }}
            >
                {NAV.map((item) => {
                    const isActive = item.name === active
                    const style: React.CSSProperties = {
                        display: 'flex',
                        alignItems: 'center',
                        gap: 10,
                        height: 30,
                        padding: '0 10px',
                        borderRadius: 4,
                        background: isActive ? 'var(--accent-bg)' : 'transparent',
                        color: isActive ? 'var(--text-accent)' : 'var(--text-2)',
                        fontWeight: isActive ? 500 : 400,
                        fontSize: 12.5,
                        borderLeft: isActive ? '2px solid var(--accent)' : '2px solid transparent',
                        paddingLeft: 8,
                        textDecoration: 'none',
                    }
                    const content = (
                        <>
                            <Icon name={item.icon} size={14} />
                            <span>{item.name}</span>
                        </>
                    )
                    return item.path ? (
                        <Link
                            key={item.name}
                            to={item.path}
                            aria-current={isActive ? 'page' : undefined}
                            style={style}
                        >
                            {content}
                        </Link>
                    ) : (
                        <div
                            key={item.name}
                            aria-current={isActive ? 'page' : undefined}
                            style={style}
                        >
                            {content}
                        </div>
                    )
                })}
            </nav>

            <div style={{ padding: '10px 14px', borderTop: '1px solid var(--border)' }}>
                <div
                    style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        fontSize: 11,
                        color: 'var(--text-3)',
                        marginBottom: 8,
                    }}
                >
                    <span>System</span>
                    <span className="mono cr" style={{ fontSize: 10 }}>
                        operational
                    </span>
                </div>
                <div
                    style={{
                        display: 'grid',
                        gridTemplateColumns: '1fr 1fr',
                        gap: 4,
                        fontSize: 10.5,
                        color: 'var(--text-2)',
                        fontFamily: 'var(--mono)',
                    }}
                >
                    {SERVICES.map((s) => (
                        <div key={s} style={{ display: 'flex', alignItems: 'center', gap: 5 }}>
                            <span
                                style={{
                                    width: 6,
                                    height: 6,
                                    borderRadius: '50%',
                                    background: 'var(--accent)',
                                }}
                            />
                            <span>{s}</span>
                        </div>
                    ))}
                </div>
            </div>

            <div
                style={{
                    padding: '10px 14px',
                    borderTop: '1px solid var(--border)',
                    display: 'flex',
                    alignItems: 'center',
                    gap: 10,
                }}
            >
                <span
                    style={{
                        width: 26,
                        height: 26,
                        borderRadius: '50%',
                        background: 'oklch(0.42 0.06 170)',
                        color: 'oklch(0.86 0.07 170)',
                        display: 'inline-flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        fontSize: 11,
                        fontWeight: 600,
                        flex: '0 0 auto',
                    }}
                >
                    LP
                </span>
                <div style={{ flex: 1, minWidth: 0, lineHeight: 1.2 }}>
                    <div style={{ fontSize: 12, fontWeight: 500 }}>Lena Park</div>
                    <div
                        className="mono"
                        style={{
                            fontSize: 10,
                            color: 'var(--text-3)',
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                            whiteSpace: 'nowrap',
                        }}
                    >
                        mercator-eu-prod
                    </div>
                </div>
                <Icon name="chevDown" size={12} />
            </div>
        </div>
    )
}
