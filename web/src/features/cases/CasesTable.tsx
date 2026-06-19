// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import type { CaseResponse } from '@/api/types'
import { CaseStatusPill } from './Pills'

export function CasesTable({ cases }: { cases: CaseResponse[] }) {
    return (
        <table className="tbl">
            <thead>
                <tr>
                    <th>Case ID</th>
                    <th>Reference</th>
                    <th>Status</th>
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
                    </tr>
                ))}
            </tbody>
        </table>
    )
}
