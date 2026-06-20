// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { describe, expect, it, vi } from 'vitest'
import { ComplianceClient, FincoreError, PaymentsClient } from '../src/index.js'

const payment = { id: 'pay_1', reference: 'po-1', amount: 125.5, currency: 'USD', status: 'SETTLED' }
const paymentPage = { items: [payment], page: 0, size: 20, totalElements: 1, totalPages: 1 }
const complianceCase = { id: 'case_1', reference: 'c-1', status: 'OPEN' }
const kycSession = { id: 'kyc_1', subjectReference: 'subj-1', status: 'APPROVED' }

function jsonResponse(body: unknown, status = 200): Response {
    return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

function mockFetch(body: unknown, status = 200) {
    return vi.fn(async (_url: string, _init?: RequestInit): Promise<Response> => jsonResponse(body, status))
}

function headersOf(call: [string, RequestInit?] | undefined): Record<string, string> {
    return (call?.[1]?.headers ?? {}) as Record<string, string>
}

describe('PaymentsClient', () => {
    it('lists payments against the payments base url', async () => {
        const fetchFn = mockFetch(paymentPage)
        const page = await new PaymentsClient({ baseUrl: 'http://payments:8081', fetch: fetchFn }).listPayments()
        expect(page.items[0]?.id).toBe('pay_1')
        expect(fetchFn.mock.calls[0]?.[0]).toBe('http://payments:8081/v1/payments?page=0&size=20')
    })

    it('keeps the payment amount as a number', async () => {
        const fetchFn = mockFetch(payment)
        const result = await new PaymentsClient({ baseUrl: 'http://payments:8081', fetch: fetchFn }).getPayment('pay_1')
        expect(result.amount).toBe(125.5)
        expect(typeof result.amount).toBe('number')
    })

    it('sends the bearer token when set', async () => {
        const fetchFn = mockFetch(payment)
        await new PaymentsClient({ baseUrl: 'http://payments:8081', token: 'secret', fetch: fetchFn }).getPayment('pay_1')
        expect(headersOf(fetchFn.mock.calls[0]).Authorization).toBe('Bearer secret')
    })

    it('throws FincoreError on a non-2xx response', async () => {
        const fetchFn = mockFetch({ detail: 'not found' }, 404)
        const client = new PaymentsClient({ baseUrl: 'http://payments:8081', fetch: fetchFn })
        await expect(client.getPayment('missing')).rejects.toBeInstanceOf(FincoreError)
    })
})

describe('ComplianceClient', () => {
    it('lists cases filtered by status', async () => {
        const fetchFn = mockFetch([complianceCase])
        const cases = await new ComplianceClient({ baseUrl: 'http://compliance', fetch: fetchFn }).listCases('OPEN')
        expect(cases[0]?.status).toBe('OPEN')
        expect(fetchFn.mock.calls[0]?.[0]).toBe('http://compliance/v1/compliance/cases?status=OPEN')
    })

    it('gets a single case', async () => {
        const fetchFn = mockFetch(complianceCase)
        const result = await new ComplianceClient({ baseUrl: 'http://compliance', fetch: fetchFn }).getCase('case_1')
        expect(result.reference).toBe('c-1')
    })

    it('gets a kyc session', async () => {
        const fetchFn = mockFetch(kycSession)
        const result = await new ComplianceClient({ baseUrl: 'http://compliance', fetch: fetchFn }).getKycSession('kyc_1')
        expect(result.subjectReference).toBe('subj-1')
        expect(fetchFn.mock.calls[0]?.[0]).toBe('http://compliance/v1/kyc/sessions/kyc_1')
    })
})
