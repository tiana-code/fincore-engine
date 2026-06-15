// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { Link, useParams } from 'react-router-dom'
import { ApiError } from '@/api/client'
import type { EntryResponse } from '@/api/types'
import { Icon } from '@/components/Icon'
import { Shell } from '@/components/Shell'
import { StateMessage } from '@/components/StateMessage'
import { TxStatusPill } from '@/features/transactions/Pills'
import { useTransaction } from '@/features/transactions/useTransaction'
import { formatAmount, formatInstant } from '@/lib/money'

export function TransactionDetail() {
    const { id = '' } = useParams()
    const { data: transaction, isPending, isError, error, refetch } = useTransaction(id)

    return (
        <Shell activeNav="Transactions">
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
                    to="/transactions"
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
                    Transactions
                </Link>

                {isPending ? (
                    <StateMessage icon="activity" text="Loading transaction..." />
                ) : isError ? (
                    error instanceof ApiError && error.status === 404 ? (
                        <StateMessage icon="inbox" text="Transaction not found." />
                    ) : (
                        <StateMessage icon="alert" text="Could not load transaction.">
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
                                {transaction.reference}
                            </span>
                            <TxStatusPill status={transaction.status} />
                            <span
                                className="mono"
                                style={{ fontSize: 11.5, color: 'var(--text-3)' }}
                            >
                                {transaction.id} · {formatInstant(transaction.postedAt)}
                            </span>
                            {transaction.reversesId && (
                                <span style={{ fontSize: 11.5, color: 'var(--text-3)' }}>
                                    reverses{' '}
                                    <Link
                                        to={`/transactions/${transaction.reversesId}`}
                                        className="mono"
                                        style={{
                                            color: 'var(--text-accent)',
                                            textDecoration: 'none',
                                        }}
                                    >
                                        {transaction.reversesId}
                                    </Link>
                                </span>
                            )}
                        </div>
                        <EntriesTable entries={transaction.entries} />
                    </>
                )}
            </div>
        </Shell>
    )
}

function EntriesTable({ entries }: { entries: EntryResponse[] }) {
    return (
        <div className="card" style={{ overflow: 'hidden' }}>
            <table className="tbl">
                <thead>
                    <tr>
                        <th>Account ID</th>
                        <th>Direction</th>
                        <th style={{ textAlign: 'right' }}>Amount</th>
                        <th>Currency</th>
                    </tr>
                </thead>
                <tbody>
                    {entries.map((entry, index) => (
                        <tr key={`${entry.accountId}-${entry.direction}-${index}`}>
                            <td>
                                <Link
                                    to={`/accounts/${entry.accountId}`}
                                    className="mono"
                                    title={entry.accountId}
                                    style={{ color: 'var(--text-accent)', textDecoration: 'none' }}
                                >
                                    {entry.accountId}
                                </Link>
                            </td>
                            <td>{entry.direction}</td>
                            <td className="mono tnum" style={{ textAlign: 'right' }}>
                                {formatAmount(entry.amount)}
                            </td>
                            <td className="mono" style={{ color: 'var(--text-2)' }}>
                                {entry.currency}
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    )
}
