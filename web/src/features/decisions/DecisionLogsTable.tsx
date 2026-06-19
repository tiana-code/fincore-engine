// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import type { DecisionLogResponse } from '@/api/types'
import { formatInstant } from '@/lib/money'
import { MatchedPill } from './Pills'

export function DecisionLogsTable({ logs }: { logs: DecisionLogResponse[] }) {
    return (
        <table className="tbl">
            <thead>
                <tr>
                    <th>Log ID</th>
                    <th>Evaluated At</th>
                    <th>Rule Version</th>
                    <th>Matched</th>
                    <th>Outcome</th>
                </tr>
            </thead>
            <tbody>
                {logs.map((log) => (
                    <tr key={log.id}>
                        <td className="mono" title={log.id} style={{ color: 'var(--text-accent)' }}>
                            {log.id}
                        </td>
                        <td className="mono" style={{ color: 'var(--text-2)' }}>
                            {formatInstant(log.evaluatedAt)}
                        </td>
                        <td
                            className="mono"
                            title={log.ruleVersionId}
                            style={{ color: 'var(--text-2)' }}
                        >
                            {log.ruleVersionId}
                        </td>
                        <td>
                            <MatchedPill matched={log.matched} />
                        </td>
                        <td>{log.outcomeLabel ?? '-'}</td>
                    </tr>
                ))}
            </tbody>
        </table>
    )
}
