// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { formatNumber } from '@/lib/format'
import type { KpiDatum } from '@/mock/types'
import type { OverviewMetrics } from './useOverviewMetrics'

const KPI_LABELS = [
    'Transactions · total',
    'Payments · total',
    'Accounts · total',
    'Compliance · open',
] as const

// Maps the truthful API counts onto the headline cards. The transactions card carries the
// real 24h hourly sparkline when one is available; the other counts have no time series.
export function liveKpis(metrics: OverviewMetrics, transactionsLast24h?: number[]): KpiDatum[] {
    const spark = transactionsLast24h?.some((count) => count > 0) ? transactionsLast24h : undefined
    return [
        {
            label: KPI_LABELS[0],
            value: formatNumber(metrics.transactions),
            sub: [{ text: 'posted to the ledger', tone: 'neutral' }],
            spark,
        },
        {
            label: KPI_LABELS[1],
            value: formatNumber(metrics.payments),
            sub: [{ text: 'across all statuses', tone: 'neutral' }],
        },
        {
            label: KPI_LABELS[2],
            value: formatNumber(metrics.accounts),
            sub: [{ text: 'open in the sandbox', tone: 'neutral' }],
        },
        {
            label: KPI_LABELS[3],
            value: formatNumber(metrics.openCases),
            sub: [
                metrics.openCases === 0
                    ? { text: 'no open cases', tone: 'credit' }
                    : { text: 'cases awaiting review', tone: 'amber' },
            ],
        },
    ]
}

// Dash placeholders for the loading/offline states so no numbers are fabricated.
export function placeholderKpis(note: string): KpiDatum[] {
    return KPI_LABELS.map((label) => ({
        label,
        value: '-',
        sub: [{ text: note, tone: 'neutral' }],
    }))
}
