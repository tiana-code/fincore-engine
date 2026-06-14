// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '@/api/client'
import type { PageResponse, TransactionResponse } from '@/api/types'

export function useTransactions(page: number, size: number) {
    return useQuery({
        queryKey: ['transactions', page, size],
        queryFn: () =>
            apiFetch<PageResponse<TransactionResponse>>(
                `/v1/transactions?page=${page}&size=${size}`,
            ),
    })
}
