// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '@/api/client'
import type { PageResponse, PaymentResponse } from '@/api/types'

export function usePayments(page: number, size: number) {
    return useQuery({
        queryKey: ['payments', page, size],
        queryFn: () =>
            apiFetch<PageResponse<PaymentResponse>>(`/v1/payments?page=${page}&size=${size}`),
    })
}
