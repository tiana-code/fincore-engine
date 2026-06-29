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

export type TransactionStatus = 'POSTED' | 'REVERSED'

export interface TransactionResponse {
    id: string
    reference: string
    status: TransactionStatus
    postedAt: string
}

export type EntryDirection = 'DEBIT' | 'CREDIT'

export interface EntryResponse {
    accountId: string
    direction: EntryDirection
    amount: string
    currency: string
}

export interface TransactionDetailResponse {
    id: string
    reference: string
    description: string | null
    status: TransactionStatus
    reversesId: string | null
    postedAt: string
    entries: EntryResponse[]
}

export interface AccountEntryResponse {
    id: string
    transactionId: string
    direction: EntryDirection
    amount: string
    currency: string
    postedAt: string
}

export interface EntryPageResponse {
    items: AccountEntryResponse[]
    nextCursor: string | null
}

export interface BalanceResponse {
    accountId: string
    currency: string
    amount: string
    lastPostedAt: string | null
}

export type CaseStatus = 'OPEN' | 'CLAIMED' | 'ESCALATED' | 'RESOLVED'

export interface CaseResponse {
    id: string
    reference: string
    status: CaseStatus
}

export interface DecisionLogResponse {
    id: string
    evaluatedAt: string
    ruleVersionId: string
    inputHash: string
    matched: boolean
    outcomeLabel: string | null
}

export type PaymentStatus =
    | 'INITIATED'
    | 'SCREENING'
    | 'SUBMITTED'
    | 'SETTLED'
    | 'FAILED'
    | 'CANCELLED'

export interface PaymentResponse {
    id: string
    reference: string
    amount: string
    currency: string
    status: PaymentStatus
}

export type LedgerActivityType = 'transaction.posted' | 'transaction.reversed' | 'account.created'

export interface LedgerActivityEvent {
    type: LedgerActivityType
    resourceId: string
    label: string
    amount: string | null
    currency: string | null
    occurredAt: string
}

export interface LedgerOverviewResponse {
    activity: LedgerActivityEvent[]
    transactionsLast24h: number[]
}
