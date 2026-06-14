// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { AccountResponse, BalanceResponse } from '@/api/types'
import { ThemeProvider } from '@/components/ThemeProvider'
import { AccountDetail } from '@/routes/AccountDetail'

const ACCOUNT: AccountResponse = {
    id: 'acc_0001',
    name: 'Operating Cash',
    type: 'ASSET',
    currency: 'EUR',
    status: 'ACTIVE',
}
const BALANCE: BalanceResponse = {
    accountId: 'acc_0001',
    currency: 'EUR',
    amount: '1234567.89',
    lastPostedAt: '2026-06-13T10:00:00Z',
}

function ok(body: unknown) {
    return { ok: true, status: 200, json: async () => body } as Response
}
function fail(status: number) {
    return { ok: false, status, json: async () => ({}) } as Response
}

function mockFetch(opts: { account?: () => Response; balance?: () => Response } = {}) {
    return vi.spyOn(globalThis, 'fetch').mockImplementation((input) => {
        const url = String(input)
        if (url.endsWith('/balance'))
            return Promise.resolve((opts.balance ?? (() => ok(BALANCE)))())
        return Promise.resolve((opts.account ?? (() => ok(ACCOUNT)))())
    })
}

function renderDetail(id = 'acc_0001') {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    return render(
        <QueryClientProvider client={client}>
            <MemoryRouter initialEntries={[`/accounts/${id}`]}>
                <ThemeProvider>
                    <Routes>
                        <Route path="/accounts/:id" element={<AccountDetail />} />
                    </Routes>
                </ThemeProvider>
            </MemoryRouter>
        </QueryClientProvider>,
    )
}

afterEach(() => {
    vi.restoreAllMocks()
})

describe('AccountDetail', () => {
    it('renders the account header and a losslessly formatted balance', async () => {
        mockFetch()
        renderDetail()
        expect(await screen.findByText('Operating Cash')).toBeInTheDocument()
        expect(screen.getByText('ACTIVE')).toBeInTheDocument()
        expect(screen.getByText('ASSET')).toBeInTheDocument()
        expect(await screen.findByText('1,234,567.89')).toBeInTheDocument()
        expect(screen.getByText(/last posted/)).toBeInTheDocument()
        expect(screen.getByText('2026-06-13 10:00:00 UTC')).toBeInTheDocument()
    })

    it('shows a not-found state on 404', async () => {
        mockFetch({ account: () => fail(404) })
        renderDetail()
        expect(await screen.findByText('Account not found.')).toBeInTheDocument()
    })

    it('shows an error state with Retry on a server error', async () => {
        const fetchMock = mockFetch({ account: () => fail(500) })
        renderDetail()
        expect(await screen.findByText('Could not load account.')).toBeInTheDocument()
        const before = fetchMock.mock.calls.length
        fireEvent.click(screen.getByRole('button', { name: 'Retry' }))
        await waitFor(() => expect(fetchMock.mock.calls.length).toBeGreaterThan(before))
    })

    it('shows a balance error independently when the account loads', async () => {
        mockFetch({ balance: () => fail(500) })
        renderDetail()
        expect(await screen.findByText('Operating Cash')).toBeInTheDocument()
        expect(await screen.findByText('Could not load balance.')).toBeInTheDocument()
    })

    it('shows a no-postings hint when lastPostedAt is null', async () => {
        mockFetch({ balance: () => ok({ ...BALANCE, lastPostedAt: null }) })
        renderDetail()
        expect(await screen.findByText('No postings yet.')).toBeInTheDocument()
    })
})
