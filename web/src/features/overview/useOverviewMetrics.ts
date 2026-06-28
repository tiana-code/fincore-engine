// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '@/api/client'
import type {
    AccountResponse,
    CaseResponse,
    PageResponse,
    PaymentResponse,
    TransactionResponse,
} from '@/api/types'

export interface OverviewMetrics {
    transactions: number
    payments: number
    accounts: number
    openCases: number
}

// Headline counts the sandbox API can answer truthfully today. Paged endpoints
// give totals through totalElements (size=1 keeps the payload tiny); open cases
// come from the status-filtered list. Latency or "today" windows have no public
// endpoint, so they are intentionally not surfaced here.
export function useOverviewMetrics(): {
    isLoading: boolean
    isError: boolean
    metrics: OverviewMetrics | null
} {
    const transactions = useQuery({
        queryKey: ['overview', 'transactions'],
        queryFn: () =>
            apiFetch<PageResponse<TransactionResponse>>('/v1/transactions?page=0&size=1'),
    })
    const payments = useQuery({
        queryKey: ['overview', 'payments'],
        queryFn: () => apiFetch<PageResponse<PaymentResponse>>('/v1/payments?page=0&size=1'),
    })
    const accounts = useQuery({
        queryKey: ['overview', 'accounts'],
        queryFn: () => apiFetch<PageResponse<AccountResponse>>('/v1/accounts?page=0&size=1'),
    })
    const openCases = useQuery({
        queryKey: ['overview', 'cases', 'OPEN'],
        queryFn: () => apiFetch<CaseResponse[]>('/v1/compliance/cases?status=OPEN'),
    })

    const queries = [transactions, payments, accounts, openCases]
    const metrics: OverviewMetrics | null =
        transactions.data && payments.data && accounts.data && openCases.data
            ? {
                  transactions: transactions.data.totalElements,
                  payments: payments.data.totalElements,
                  accounts: accounts.data.totalElements,
                  openCases: openCases.data.length,
              }
            : null

    return {
        isLoading: queries.some((query) => query.isLoading),
        isError: queries.some((query) => query.isError),
        metrics,
    }
}
