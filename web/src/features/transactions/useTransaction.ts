// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '@/api/client'
import type { TransactionDetailResponse } from '@/api/types'

export function useTransaction(id: string) {
    return useQuery({
        queryKey: ['transaction', id],
        queryFn: () => apiFetch<TransactionDetailResponse>(`/v1/transactions/${id}`),
    })
}
