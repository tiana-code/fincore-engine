// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { DEFAULT_PAGE_SIZE, HttpTransport, type FincoreClientOptions } from './http.js'
import type { Account, Balance, Page, Transaction, TransactionDetail } from './types.js'

export class FincoreClient {
    private readonly http: HttpTransport

    constructor(options: FincoreClientOptions) {
        this.http = new HttpTransport(options)
    }

    listAccounts(page = 0, size: number = DEFAULT_PAGE_SIZE): Promise<Page<Account>> {
        return this.http.get<Page<Account>>(`/v1/accounts?page=${page}&size=${size}`)
    }

    getAccount(id: string): Promise<Account> {
        return this.http.get<Account>(`/v1/accounts/${encodeURIComponent(id)}`)
    }

    getBalance(accountId: string): Promise<Balance> {
        return this.http.get<Balance>(`/v1/accounts/${encodeURIComponent(accountId)}/balance`)
    }

    listTransactions(page = 0, size: number = DEFAULT_PAGE_SIZE): Promise<Page<Transaction>> {
        return this.http.get<Page<Transaction>>(`/v1/transactions?page=${page}&size=${size}`)
    }

    getTransaction(id: string): Promise<TransactionDetail> {
        return this.http.get<TransactionDetail>(`/v1/transactions/${encodeURIComponent(id)}`)
    }
}
