// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { formatNumber } from '@/lib/format'
import type { KpiDatum } from '@/mock/types'
import type { OverviewMetrics } from './useOverviewMetrics'

// Maps the truthful API counts onto the headline cards. No sparklines: a single
// count has no time series to draw.
export function liveKpis(metrics: OverviewMetrics): KpiDatum[] {
    return [
        {
            label: 'Transactions · total',
            value: formatNumber(metrics.transactions),
            sub: [{ text: 'posted to the ledger', tone: 'neutral' }],
        },
        {
            label: 'Payments · total',
            value: formatNumber(metrics.payments),
            sub: [{ text: 'across all statuses', tone: 'neutral' }],
        },
        {
            label: 'Accounts · total',
            value: formatNumber(metrics.accounts),
            sub: [{ text: 'open in the sandbox', tone: 'neutral' }],
        },
        {
            label: 'Compliance · open',
            value: formatNumber(metrics.openCases),
            sub: [
                metrics.openCases === 0
                    ? { text: 'no open cases', tone: 'credit' }
                    : { text: 'cases awaiting review', tone: 'amber' },
            ],
        },
    ]
}
