// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '@/api/client'
import type { AccountResponse, BalanceResponse } from '@/api/types'

export function useAccount(id: string) {
    return useQuery({
        queryKey: ['account', id],
        queryFn: () => apiFetch<AccountResponse>(`/v1/accounts/${id}`),
    })
}

export function useBalance(id: string) {
    return useQuery({
        queryKey: ['account', id, 'balance'],
        queryFn: () => apiFetch<BalanceResponse>(`/v1/accounts/${id}/balance`),
    })
}
