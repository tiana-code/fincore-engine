// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '@/api/client'
import type { AccountResponse, PageResponse } from '@/api/types'

export function useAccounts(page: number, size: number) {
    return useQuery({
        queryKey: ['accounts', page, size],
        queryFn: () =>
            apiFetch<PageResponse<AccountResponse>>(`/v1/accounts?page=${page}&size=${size}`),
    })
}
