// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { Link } from 'react-router-dom'
import type { AccountResponse } from '@/api/types'
import { StatusPill, TypePill } from './Pills'

export function AccountsTable({ accounts }: { accounts: AccountResponse[] }) {
    return (
        <table className="tbl">
            <thead>
                <tr>
                    <th>Account ID</th>
                    <th>Name</th>
                    <th>Type</th>
                    <th>Currency</th>
                    <th>Status</th>
                </tr>
            </thead>
            <tbody>
                {accounts.map((account) => (
                    <tr key={account.id}>
                        <td>
                            <Link
                                to={`/accounts/${account.id}`}
                                className="mono"
                                title={account.id}
                                style={{ color: 'var(--text-accent)', textDecoration: 'none' }}
                            >
                                {account.id}
                            </Link>
                        </td>
                        <td>{account.name}</td>
                        <td>
                            <TypePill type={account.type} />
                        </td>
                        <td className="mono" style={{ color: 'var(--text-2)' }}>
                            {account.currency}
                        </td>
                        <td>
                            <StatusPill status={account.status} />
                        </td>
                    </tr>
                ))}
            </tbody>
        </table>
    )
}
