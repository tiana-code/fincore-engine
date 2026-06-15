// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { AccountEntryResponse, AccountResponse, BalanceResponse } from '@/api/types'
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
const ENTRY_A: AccountEntryResponse = {
    id: 'ent_0001',
    transactionId: 'tx_aaa',
    direction: 'DEBIT',
    amount: '100.00',
    currency: 'EUR',
    postedAt: '2026-06-13T09:00:00Z',
}
const ENTRY_B: AccountEntryResponse = {
    id: 'ent_0002',
    transactionId: 'tx_bbb',
    direction: 'CREDIT',
    amount: '250.50',
    currency: 'EUR',
    postedAt: '2026-06-12T08:00:00Z',
}
const EMPTY_ENTRIES = { items: [], nextCursor: null }

function ok(body: unknown) {
    return { ok: true, status: 200, json: async () => body } as Response
}
function fail(status: number) {
    return { ok: false, status, json: async () => ({}) } as Response
}

function mockFetch(
    opts: {
        account?: () => Response
        balance?: () => Response
        entries?: (url: string) => Response
    } = {},
) {
    return vi.spyOn(globalThis, 'fetch').mockImplementation((input) => {
        const url = String(input)
        if (url.includes('/entries'))
            return Promise.resolve((opts.entries ?? (() => ok(EMPTY_ENTRIES)))(url))
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

    it('renders entry rows with a transaction link and lossless amounts', async () => {
        mockFetch({ entries: () => ok({ items: [ENTRY_A, ENTRY_B], nextCursor: null }) })
        renderDetail()
        expect(await screen.findByText('ent_0001')).toBeInTheDocument()
        expect(screen.getByText('ent_0002')).toBeInTheDocument()
        expect(screen.getByText('100.00')).toBeInTheDocument()
        expect(screen.getByText('250.50')).toBeInTheDocument()
        const link = screen.getByRole('link', { name: 'tx_aaa' })
        expect(link).toHaveAttribute('href', '/transactions/tx_aaa')
        expect(screen.queryByRole('button', { name: 'Load more' })).not.toBeInTheDocument()
    })

    it('appends the next page and hides Load more when the cursor is exhausted', async () => {
        mockFetch({
            entries: (url) =>
                url.includes('cursor=c2')
                    ? ok({ items: [ENTRY_B], nextCursor: null })
                    : ok({ items: [ENTRY_A], nextCursor: 'c2' }),
        })
        renderDetail()
        expect(await screen.findByText('ent_0001')).toBeInTheDocument()
        fireEvent.click(screen.getByRole('button', { name: 'Load more' }))
        expect(await screen.findByText('ent_0002')).toBeInTheDocument()
        await waitFor(() =>
            expect(screen.queryByRole('button', { name: 'Load more' })).not.toBeInTheDocument(),
        )
    })

    it('shows an empty state when the account has no entries', async () => {
        mockFetch({ entries: () => ok(EMPTY_ENTRIES) })
        renderDetail()
        expect(await screen.findByText('No entries yet.')).toBeInTheDocument()
    })

    it('shows an entries error with Retry while keeping the header visible', async () => {
        mockFetch({ entries: () => fail(500) })
        renderDetail()
        expect(await screen.findByText('Operating Cash')).toBeInTheDocument()
        expect(await screen.findByText('Could not load entries.')).toBeInTheDocument()
        const retry = screen.getByRole('button', { name: 'Retry' })
        const fetchMock = globalThis.fetch as unknown as ReturnType<typeof vi.fn>
        const before = fetchMock.mock.calls.length
        fireEvent.click(retry)
        await waitFor(() => expect(fetchMock.mock.calls.length).toBeGreaterThan(before))
    })
})
