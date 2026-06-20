// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

export const DEFAULT_PAGE_SIZE = 20

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

export class HttpTransport {
    private readonly baseUrl: string
    private readonly token?: string
    private readonly fetchFn: FetchFn

    constructor(options: FincoreClientOptions) {
        this.baseUrl = options.baseUrl.replace(/\/+$/, '')
        this.token = options.token
        this.fetchFn = options.fetch ?? fetch
    }

    async get<T>(path: string): Promise<T> {
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
