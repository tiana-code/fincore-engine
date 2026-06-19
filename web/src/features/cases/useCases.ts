// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '@/api/client'
import type { CaseResponse, CaseStatus } from '@/api/types'

export function useCases(status: CaseStatus) {
    return useQuery({
        queryKey: ['cases', status],
        queryFn: () => apiFetch<CaseResponse[]>(`/v1/compliance/cases?status=${status}`),
    })
}
