// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

export type AccountType =
    | 'ASSET'
    | 'LIABILITY'
    | 'EQUITY'
    | 'REVENUE'
    | 'EXPENSE'
    | 'USER_WALLET'
    | 'FEE'
    | 'RESERVE'
    | 'SUSPENSE'

export type AccountStatus = 'ACTIVE' | 'FROZEN' | 'CLOSED'

export interface AccountResponse {
    id: string
    name: string
    type: AccountType
    currency: string
    status: AccountStatus
}

export interface PageResponse<T> {
    items: T[]
    page: number
    size: number
    totalElements: number
    totalPages: number
}

export interface BalanceResponse {
    accountId: string
    currency: string
    amount: string
    lastPostedAt: string | null
}
