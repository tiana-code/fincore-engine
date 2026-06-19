// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { useState } from 'react'
import { Shell } from '@/components/Shell'
import { StateMessage } from '@/components/StateMessage'
import { DecisionLogsTable } from '@/features/decisions/DecisionLogsTable'
import {
    type DecisionLogField,
    type DecisionLogFilter,
    useDecisionLogs,
} from '@/features/decisions/useDecisionLogs'

const FIELDS: { value: DecisionLogField; label: string }[] = [
    { value: 'inputHash', label: 'Input hash' },
    { value: 'ruleVersionId', label: 'Rule version id' },
]

export function Decisions() {
    const [field, setField] = useState<DecisionLogField>('inputHash')
    const [value, setValue] = useState('')
    const [filter, setFilter] = useState<DecisionLogFilter | null>(null)
    const { data, isPending, isError, refetch } = useDecisionLogs(filter)

    const submit = (event: React.FormEvent) => {
        event.preventDefault()
        const trimmed = value.trim()
        if (trimmed) setFilter({ field, value: trimmed })
    }

    return (
        <Shell activeNav="Decisions">
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
                <form onSubmit={submit} style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                    <div
                        role="tablist"
                        aria-label="Filter field"
                        style={{ display: 'flex', gap: 6 }}
                    >
                        {FIELDS.map((option) => (
                            <button
                                key={option.value}
                                type="button"
                                role="tab"
                                aria-selected={option.value === field}
                                className={`btn${option.value === field ? ' btn-active' : ''}`}
                                onClick={() => setField(option.value)}
                            >
                                {option.label}
                            </button>
                        ))}
                    </div>
                    <input
                        className="input mono"
                        aria-label="Query value"
                        value={value}
                        onChange={(event) => setValue(event.target.value)}
                        style={{ flex: 1 }}
                    />
                    <button type="submit" className="btn">
                        Search
                    </button>
                </form>

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
                        {filter === null ? (
                            <StateMessage
                                icon="inbox"
                                text="Enter an input hash or rule version id to search the decision log."
                            />
                        ) : isPending ? (
                            <StateMessage icon="activity" text="Loading decision logs..." />
                        ) : isError ? (
                            <StateMessage icon="alert" text="Could not load decision logs.">
                                <button type="button" className="btn" onClick={() => refetch()}>
                                    Retry
                                </button>
                            </StateMessage>
                        ) : data.length === 0 ? (
                            <StateMessage icon="inbox" text="No decision logs for this query." />
                        ) : (
                            <DecisionLogsTable logs={data} />
                        )}
                    </div>
                </div>
            </div>
        </Shell>
    )
}
