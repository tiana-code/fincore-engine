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

export interface Account {
    id: string
    name: string
    type: AccountType
    currency: string
    status: AccountStatus
}

export interface Page<T> {
    items: T[]
    page: number
    size: number
    totalElements: number
    totalPages: number
}

export interface Balance {
    accountId: string
    currency: string
    amount: string
    lastPostedAt: string | null
}

export type TransactionStatus = 'POSTED' | 'REVERSED'

export interface Transaction {
    id: string
    reference: string
    status: TransactionStatus
    postedAt: string
}

export type EntryDirection = 'DEBIT' | 'CREDIT'

export interface Entry {
    accountId: string
    direction: EntryDirection
    amount: string
    currency: string
}

export interface TransactionDetail {
    id: string
    reference: string
    description: string | null
    status: TransactionStatus
    reversesId: string | null
    postedAt: string
    entries: Entry[]
}

export type PaymentStatus = 'INITIATED' | 'SCREENING' | 'SUBMITTED' | 'SETTLED' | 'FAILED' | 'CANCELLED'

export interface Payment {
    id: string
    reference: string
    amount: string
    currency: string
    status: PaymentStatus
}

export type CaseStatus = 'OPEN' | 'CLAIMED' | 'ESCALATED' | 'RESOLVED'

export interface ComplianceCase {
    id: string
    reference: string
    status: CaseStatus
}

export type KycStatus = 'INITIATED' | 'SCREENING' | 'APPROVED' | 'REJECTED'

export interface KycSession {
    id: string
    subjectReference: string
    status: KycStatus
}
