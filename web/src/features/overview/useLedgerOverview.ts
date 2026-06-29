// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '@/api/client'
import type { LedgerOverviewResponse } from '@/api/types'

// Recent ledger activity + the 24h hourly posting count, from the ledger read model.
// Polls every 30s so the "API live" chip and the feed stay reasonably current.
export function useLedgerOverview() {
    return useQuery({
        queryKey: ['overview', 'ledger'],
        queryFn: () => apiFetch<LedgerOverviewResponse>('/v1/overview'),
        staleTime: 30_000,
        refetchInterval: 30_000,
    })
}
