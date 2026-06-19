// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { useState } from 'react'
import type { CaseStatus } from '@/api/types'
import { Shell } from '@/components/Shell'
import { StateMessage } from '@/components/StateMessage'
import { CasesTable } from '@/features/cases/CasesTable'
import { useCases } from '@/features/cases/useCases'

const STATUSES: CaseStatus[] = ['OPEN', 'CLAIMED', 'ESCALATED', 'RESOLVED']

export function Cases() {
    const [status, setStatus] = useState<CaseStatus>('OPEN')
    const { data, isPending, isError, refetch } = useCases(status)

    return (
        <Shell activeNav="Compliance">
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
                <div role="tablist" aria-label="Case status" style={{ display: 'flex', gap: 6 }}>
                    {STATUSES.map((option) => (
                        <button
                            key={option}
                            type="button"
                            role="tab"
                            aria-selected={option === status}
                            className={`btn${option === status ? ' btn-active' : ''}`}
                            onClick={() => setStatus(option)}
                        >
                            {option}
                        </button>
                    ))}
                </div>

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
                            <StateMessage icon="activity" text="Loading cases..." />
                        ) : isError ? (
                            <StateMessage icon="alert" text="Could not load cases.">
                                <button type="button" className="btn" onClick={() => refetch()}>
                                    Retry
                                </button>
                            </StateMessage>
                        ) : data.length === 0 ? (
                            <StateMessage icon="inbox" text="No cases in this status." />
                        ) : (
                            <CasesTable cases={data} />
                        )}
                    </div>
                </div>
            </div>
        </Shell>
    )
}
