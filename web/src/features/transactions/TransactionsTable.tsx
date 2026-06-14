// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import type { TransactionResponse } from '@/api/types'
import { formatInstant } from '@/lib/money'
import { TxStatusPill } from './Pills'

export function TransactionsTable({ transactions }: { transactions: TransactionResponse[] }) {
    return (
        <table className="tbl">
            <thead>
                <tr>
                    <th>Transaction ID</th>
                    <th>Reference</th>
                    <th>Status</th>
                    <th>Posted At</th>
                </tr>
            </thead>
            <tbody>
                {transactions.map((transaction) => (
                    <tr key={transaction.id}>
                        <td
                            className="mono"
                            title={transaction.id}
                            style={{ color: 'var(--text-2)' }}
                        >
                            {transaction.id}
                        </td>
                        <td>{transaction.reference}</td>
                        <td>
                            <TxStatusPill status={transaction.status} />
                        </td>
                        <td className="mono" style={{ color: 'var(--text-2)' }}>
                            {formatInstant(transaction.postedAt)}
                        </td>
                    </tr>
                ))}
            </tbody>
        </table>
    )
}
