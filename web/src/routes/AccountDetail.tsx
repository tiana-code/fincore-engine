// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { Link, useParams } from 'react-router-dom'
import { ApiError } from '@/api/client'
import { Icon } from '@/components/Icon'
import { Shell } from '@/components/Shell'
import { StateMessage } from '@/components/StateMessage'
import { StatusPill, TypePill } from '@/features/accounts/Pills'
import { useAccount, useBalance } from '@/features/accounts/useAccount'
import { useAccountEntries } from '@/features/accounts/useAccountEntries'
import { formatAmount, formatInstant } from '@/lib/money'

export function AccountDetail() {
    const { id = '' } = useParams()
    const { data: account, isPending, isError, error, refetch } = useAccount(id)

    return (
        <Shell activeNav="Accounts">
            <div
                style={{
                    padding: '20px 24px',
                    display: 'flex',
                    flexDirection: 'column',
                    gap: 16,
                    flex: 1,
                    minHeight: 0,
                }}
            >
                <Link
                    to="/accounts"
                    style={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: 6,
                        fontSize: 12,
                        color: 'var(--text-2)',
                        textDecoration: 'none',
                    }}
                >
                    <Icon name="chevLeft" size={12} />
                    Accounts
                </Link>

                {isPending ? (
                    <StateMessage icon="activity" text="Loading account..." />
                ) : isError ? (
                    error instanceof ApiError && error.status === 404 ? (
                        <StateMessage icon="inbox" text="Account not found." />
                    ) : (
                        <StateMessage icon="alert" text="Could not load account.">
                            <button type="button" className="btn" onClick={() => refetch()}>
                                Retry
                            </button>
                        </StateMessage>
                    )
                ) : (
                    <>
                        <div
                            className="card"
                            style={{
                                padding: 18,
                                display: 'flex',
                                alignItems: 'center',
                                gap: 12,
                                flexWrap: 'wrap',
                            }}
                        >
                            <span
                                style={{ fontSize: 19, fontWeight: 500, letterSpacing: '-0.01em' }}
                            >
                                {account.name}
                            </span>
                            <StatusPill status={account.status} />
                            <TypePill type={account.type} />
                            <span
                                className="mono"
                                style={{ fontSize: 11.5, color: 'var(--text-3)' }}
                            >
                                {account.id} · {account.currency}
                            </span>
                        </div>
                        <BalanceCard id={id} />
                        <AccountEntries id={id} />
                    </>
                )}
            </div>
        </Shell>
    )
}

function BalanceCard({ id }: { id: string }) {
    const { data: balance, isPending, isError, refetch } = useBalance(id)

    return (
        <div className="card" style={{ padding: 18, maxWidth: 360 }}>
            <div
                style={{
                    fontSize: 11,
                    color: 'var(--text-3)',
                    textTransform: 'uppercase',
                    letterSpacing: '0.06em',
                }}
            >
                Current balance
            </div>
            {isPending ? (
                <div style={{ marginTop: 8, fontSize: 13, color: 'var(--text-3)' }}>
                    Loading balance...
                </div>
            ) : isError ? (
                <div
                    style={{
                        marginTop: 8,
                        display: 'flex',
                        alignItems: 'center',
                        gap: 10,
                        fontSize: 13,
                        color: 'var(--text-3)',
                    }}
                >
                    <span>Could not load balance.</span>
                    <button type="button" className="btn btn-sm" onClick={() => refetch()}>
                        Retry
                    </button>
                </div>
            ) : (
                <>
                    <div
                        className="mono tnum"
                        style={{
                            fontSize: 32,
                            fontWeight: 500,
                            letterSpacing: '-0.02em',
                            marginTop: 8,
                        }}
                    >
                        <span>{formatAmount(balance.amount)}</span>{' '}
                        <span style={{ fontSize: 14, color: 'var(--text-3)', fontWeight: 400 }}>
                            {balance.currency}
                        </span>
                    </div>
                    <div style={{ fontSize: 11.5, color: 'var(--text-2)', marginTop: 6 }}>
                        {balance.lastPostedAt ? (
                            <>
                                last posted{' '}
                                <span className="mono">{formatInstant(balance.lastPostedAt)}</span>
                            </>
                        ) : (
                            'No postings yet.'
                        )}
                    </div>
                </>
            )}
        </div>
    )
}

function AccountEntries({ id }: { id: string }) {
    const { data, isPending, isError, refetch, hasNextPage, fetchNextPage, isFetchingNextPage } =
        useAccountEntries(id)

    if (isPending) return <StateMessage icon="activity" text="Loading entries..." />
    if (isError)
        return (
            <StateMessage icon="alert" text="Could not load entries.">
                <button type="button" className="btn" onClick={() => refetch()}>
                    Retry
                </button>
            </StateMessage>
        )

    const entries = data.pages.flatMap((page) => page.items)
    if (entries.length === 0) return <StateMessage icon="inbox" text="No entries yet." />

    return (
        <div className="card" style={{ overflow: 'hidden' }}>
            <table className="tbl">
                <thead>
                    <tr>
                        <th>Entry ID</th>
                        <th>Transaction ID</th>
                        <th>Direction</th>
                        <th style={{ textAlign: 'right' }}>Amount</th>
                        <th>Currency</th>
                        <th>Posted At</th>
                    </tr>
                </thead>
                <tbody>
                    {entries.map((entry) => (
                        <tr key={entry.id}>
                            <td className="mono" style={{ color: 'var(--text-2)' }}>
                                {entry.id}
                            </td>
                            <td>
                                <Link
                                    to={`/transactions/${entry.transactionId}`}
                                    className="mono"
                                    title={entry.transactionId}
                                    style={{ color: 'var(--text-accent)', textDecoration: 'none' }}
                                >
                                    {entry.transactionId}
                                </Link>
                            </td>
                            <td>{entry.direction}</td>
                            <td className="mono tnum" style={{ textAlign: 'right' }}>
                                {formatAmount(entry.amount)}
                            </td>
                            <td className="mono" style={{ color: 'var(--text-2)' }}>
                                {entry.currency}
                            </td>
                            <td className="mono" style={{ color: 'var(--text-2)' }}>
                                {formatInstant(entry.postedAt)}
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
            {hasNextPage && (
                <div style={{ padding: 12, textAlign: 'center' }}>
                    <button
                        type="button"
                        className="btn btn-sm"
                        disabled={isFetchingNextPage}
                        onClick={() => fetchNextPage()}
                    >
                        Load more
                    </button>
                </div>
            )}
        </div>
    )
}
