// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import type { CaseResponse } from '@/api/types'
import { CaseStatusPill } from './Pills'
import { type CaseAction, actionsFor } from './useCaseAction'

interface CasesTableProps {
    cases: CaseResponse[]
    onAction: (id: string, action: CaseAction) => void
    pending: boolean
}

export function CasesTable({ cases, onAction, pending }: CasesTableProps) {
    return (
        <table className="tbl">
            <thead>
                <tr>
                    <th>Case ID</th>
                    <th>Reference</th>
                    <th>Status</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
                {cases.map((kase) => (
                    <tr key={kase.id}>
                        <td
                            className="mono"
                            title={kase.id}
                            style={{ color: 'var(--text-accent)' }}
                        >
                            {kase.id}
                        </td>
                        <td>{kase.reference}</td>
                        <td>
                            <CaseStatusPill status={kase.status} />
                        </td>
                        <td>
                            <div style={{ display: 'flex', gap: 6 }}>
                                {actionsFor(kase.status).map((action) => (
                                    <button
                                        key={action}
                                        type="button"
                                        className="btn btn-sm"
                                        disabled={pending}
                                        onClick={() => onAction(kase.id, action)}
                                    >
                                        {action}
                                    </button>
                                ))}
                            </div>
                        </td>
                    </tr>
                ))}
            </tbody>
        </table>
    )
}
