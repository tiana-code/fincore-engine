// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import type { Account, Balance, Page, Transaction, TransactionDetail } from './types.js'

const DEFAULT_PAGE_SIZE = 20

export class FincoreError extends Error {
    constructor(readonly status: number) {
        super(`request failed with status ${status}`)
        this.name = 'FincoreError'
    }
}

export type FetchFn = (url: string, init?: RequestInit) => Promise<Response>

export interface FincoreClientOptions {
    baseUrl: string
    token?: string
    fetch?: FetchFn
}

export class FincoreClient {
    private readonly baseUrl: string
    private readonly token?: string
    private readonly fetchFn: FetchFn

    constructor(options: FincoreClientOptions) {
        this.baseUrl = options.baseUrl.replace(/\/+$/, '')
        this.token = options.token
        this.fetchFn = options.fetch ?? fetch
    }

    listAccounts(page = 0, size: number = DEFAULT_PAGE_SIZE): Promise<Page<Account>> {
        return this.get<Page<Account>>(`/v1/accounts?page=${page}&size=${size}`)
    }

    getAccount(id: string): Promise<Account> {
        return this.get<Account>(`/v1/accounts/${encodeURIComponent(id)}`)
    }

    getBalance(accountId: string): Promise<Balance> {
        return this.get<Balance>(`/v1/accounts/${encodeURIComponent(accountId)}/balance`)
    }

    listTransactions(page = 0, size: number = DEFAULT_PAGE_SIZE): Promise<Page<Transaction>> {
        return this.get<Page<Transaction>>(`/v1/transactions?page=${page}&size=${size}`)
    }

    getTransaction(id: string): Promise<TransactionDetail> {
        return this.get<TransactionDetail>(`/v1/transactions/${encodeURIComponent(id)}`)
    }

    private async get<T>(path: string): Promise<T> {
        const headers: Record<string, string> = { Accept: 'application/json' }
        if (this.token !== undefined) {
            headers.Authorization = `Bearer ${this.token}`
        }
        const response = await this.fetchFn(`${this.baseUrl}${path}`, { headers })
        if (!response.ok) {
            throw new FincoreError(response.status)
        }
        return (await response.json()) as T
    }
}
