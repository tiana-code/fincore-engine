// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { DEFAULT_PAGE_SIZE, HttpTransport, type FincoreClientOptions } from './http.js'
import type { Page, Payment } from './types.js'

export class PaymentsClient {
    private readonly http: HttpTransport

    constructor(options: FincoreClientOptions) {
        this.http = new HttpTransport(options)
    }

    listPayments(page = 0, size: number = DEFAULT_PAGE_SIZE): Promise<Page<Payment>> {
        return this.http.get<Page<Payment>>(`/v1/payments?page=${page}&size=${size}`)
    }

    getPayment(id: string): Promise<Payment> {
        return this.http.get<Payment>(`/v1/payments/${encodeURIComponent(id)}`)
    }
}
