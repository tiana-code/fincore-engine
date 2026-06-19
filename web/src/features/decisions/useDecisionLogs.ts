// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '@/api/client'
import type { DecisionLogResponse } from '@/api/types'

export type DecisionLogField = 'inputHash' | 'ruleVersionId'

export interface DecisionLogFilter {
    field: DecisionLogField
    value: string
}

export function useDecisionLogs(filter: DecisionLogFilter | null) {
    return useQuery({
        queryKey: ['decision-logs', filter],
        queryFn: () => {
            const { field, value } = filter!
            return apiFetch<DecisionLogResponse[]>(
                `/v1/decision/logs?${field}=${encodeURIComponent(value)}`,
            )
        },
        enabled: filter !== null,
    })
}
