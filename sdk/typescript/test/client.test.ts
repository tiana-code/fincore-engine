// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { describe, expect, it, vi } from 'vitest'
import { FincoreClient, FincoreError } from '../src/index.js'

const account = { id: 'acc_1', name: 'Cash', type: 'ASSET', currency: 'USD', status: 'ACTIVE' }
const pageBody = { items: [account], page: 0, size: 20, totalElements: 1, totalPages: 1 }

function jsonResponse(body: unknown, status = 200): Response {
    return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

function mockFetch(body: unknown, status = 200) {
    return vi.fn(async (_url: string, _init?: RequestInit): Promise<Response> => jsonResponse(body, status))
}

function headersOf(call: [string, RequestInit?] | undefined): Record<string, string> {
    return (call?.[1]?.headers ?? {}) as Record<string, string>
}

describe('FincoreClient', () => {
    it('lists accounts and decodes the page', async () => {
        const fetchFn = mockFetch(pageBody)
        const client = new FincoreClient({ baseUrl: 'http://host', fetch: fetchFn })
        const page = await client.listAccounts()
        expect(page.totalElements).toBe(1)
        expect(page.items[0]?.id).toBe('acc_1')
        expect(fetchFn.mock.calls[0]?.[0]).toBe('http://host/v1/accounts?page=0&size=20')
    })

    it('sends the bearer token only when set', async () => {
        const withToken = mockFetch(pageBody)
        await new FincoreClient({ baseUrl: 'http://host', token: 'secret', fetch: withToken }).listAccounts()
        expect(headersOf(withToken.mock.calls[0]).Authorization).toBe('Bearer secret')

        const noToken = mockFetch(pageBody)
        await new FincoreClient({ baseUrl: 'http://host', fetch: noToken }).listAccounts()
        expect(headersOf(noToken.mock.calls[0]).Authorization).toBeUndefined()
    })

    it('encodes the account id in the path', async () => {
        const fetchFn = mockFetch(account)
        await new FincoreClient({ baseUrl: 'http://host', fetch: fetchFn }).getAccount('a/b')
        expect(fetchFn.mock.calls[0]?.[0]).toBe('http://host/v1/accounts/a%2Fb')
    })

    it('gets an account balance with a string amount', async () => {
        const balance = { accountId: 'acc_1', currency: 'USD', amount: '100.00', lastPostedAt: null }
        const fetchFn = mockFetch(balance)
        const result = await new FincoreClient({ baseUrl: 'http://host', fetch: fetchFn }).getBalance('acc_1')
        expect(result.amount).toBe('100.00')
        expect(fetchFn.mock.calls[0]?.[0]).toBe('http://host/v1/accounts/acc_1/balance')
    })

    it('lists transactions and decodes the page', async () => {
        const tx = { id: 'tx_1', reference: 'r1', status: 'POSTED', postedAt: '2026-06-19T00:00:00Z' }
        const body = { items: [tx], page: 0, size: 20, totalElements: 1, totalPages: 1 }
        const page = await new FincoreClient({ baseUrl: 'http://host', fetch: mockFetch(body) }).listTransactions()
        expect(page.items[0]?.status).toBe('POSTED')
    })

    it('gets a transaction with its entries', async () => {
        const detail = {
            id: 'tx_1',
            reference: 'r1',
            description: null,
            status: 'POSTED',
            reversesId: null,
            postedAt: '2026-06-19T00:00:00Z',
            entries: [{ accountId: 'acc_1', direction: 'DEBIT', amount: '100.00', currency: 'USD' }],
        }
        const result = await new FincoreClient({ baseUrl: 'http://host', fetch: mockFetch(detail) }).getTransaction('tx_1')
        expect(result.entries[0]?.direction).toBe('DEBIT')
        expect(result.entries[0]?.amount).toBe('100.00')
    })

    it('throws FincoreError with the status on a non-2xx response', async () => {
        const client = new FincoreClient({ baseUrl: 'http://host', fetch: mockFetch({ detail: 'not found' }, 404) })
        await expect(client.getAccount('missing')).rejects.toBeInstanceOf(FincoreError)
        await expect(client.getAccount('missing')).rejects.toMatchObject({ status: 404 })
    })
})
