// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import type { PaymentResponse } from '@/api/types'
import { formatMoney } from '@/lib/format'
import { PaymentStatusPill } from './Pills'

export function PaymentsTable({ payments }: { payments: PaymentResponse[] }) {
    return (
        <table className="tbl">
            <thead>
                <tr>
                    <th>Payment ID</th>
                    <th>Reference</th>
                    <th>Amount</th>
                    <th>Status</th>
                </tr>
            </thead>
            <tbody>
                {payments.map((payment) => (
                    <tr key={payment.id}>
                        <td
                            className="mono"
                            title={payment.id}
                            style={{ color: 'var(--text-accent)' }}
                        >
                            {payment.id}
                        </td>
                        <td>{payment.reference}</td>
                        <td className="mono" style={{ color: 'var(--text-2)' }}>
                            {formatMoney(payment.amount, payment.currency)}
                        </td>
                        <td>
                            <PaymentStatusPill status={payment.status} />
                        </td>
                    </tr>
                ))}
            </tbody>
        </table>
    )
}
