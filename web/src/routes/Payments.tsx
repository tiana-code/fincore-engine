// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { useState } from 'react'
import { Shell } from '@/components/Shell'
import { StateMessage } from '@/components/StateMessage'
import { Pagination } from '@/features/accounts/Pagination'
import { PaymentForm } from '@/features/payments/PaymentForm'
import { PaymentsTable } from '@/features/payments/PaymentsTable'
import { useInitiatePayment } from '@/features/payments/useInitiatePayment'
import { usePayments } from '@/features/payments/usePayments'

const PAGE_SIZE = 20

export function Payments() {
    const [page, setPage] = useState(0)
    const [showForm, setShowForm] = useState(false)
    const { data, isPending, isError, refetch } = usePayments(page, PAGE_SIZE)
    const create = useInitiatePayment()

    return (
        <Shell activeNav="Payments">
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
                <div>
                    <button
                        type="button"
                        className="btn btn-primary"
                        onClick={() => {
                            create.reset()
                            setShowForm((open) => !open)
                        }}
                    >
                        {showForm ? 'Close' : 'New payment'}
                    </button>
                </div>
                {showForm && (
                    <PaymentForm
                        pending={create.isPending}
                        onCancel={() => {
                            create.reset()
                            setShowForm(false)
                        }}
                        onSubmit={(payment) =>
                            create.mutate(
                                { payment, idempotencyKey: crypto.randomUUID() },
                                { onSuccess: () => setShowForm(false) },
                            )
                        }
                    />
                )}
                {create.isError && (
                    <div role="alert" style={{ fontSize: 12, color: 'var(--text-err)' }}>
                        Could not create the payment. Try again.
                    </div>
                )}
                <div
                    className="card"
                    style={{
                        flex: 1,
                        minHeight: 0,
                        display: 'flex',
                        flexDirection: 'column',
                        overflow: 'hidden',
                    }}
                >
                    <div style={{ flex: 1, overflow: 'auto' }} className="fc-scroll">
                        {isPending ? (
                            <StateMessage icon="activity" text="Loading payments..." />
                        ) : isError ? (
                            <StateMessage icon="alert" text="Could not load payments.">
                                <button type="button" className="btn" onClick={() => refetch()}>
                                    Retry
                                </button>
                            </StateMessage>
                        ) : data.totalElements === 0 ? (
                            <StateMessage icon="inbox" text="No payments yet." />
                        ) : (
                            <PaymentsTable payments={data.items} />
                        )}
                    </div>
                    {!isPending && !isError && (
                        <Pagination
                            page={data.page}
                            totalPages={data.totalPages}
                            totalElements={data.totalElements}
                            onPrev={() => setPage((p) => Math.max(0, p - 1))}
                            onNext={() => setPage((p) => p + 1)}
                            noun="payments"
                        />
                    )}
                </div>
            </div>
        </Shell>
    )
}
