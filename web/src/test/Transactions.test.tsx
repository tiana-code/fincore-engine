// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import type { ReactElement } from 'react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { PageResponse, TransactionResponse } from '@/api/types'
import { ThemeProvider } from '@/components/ThemeProvider'
import { Transactions } from '@/routes/Transactions'

function page(
    items: TransactionResponse[],
    over: Partial<PageResponse<TransactionResponse>> = {},
): PageResponse<TransactionResponse> {
    return { items, page: 0, size: 20, totalElements: items.length, totalPages: 1, ...over }
}

const SAMPLE: TransactionResponse[] = [
    {
        id: 'tx_0001',
        reference: 'payroll-0425',
        status: 'POSTED',
        postedAt: '2026-06-13T10:00:00Z',
    },
    {
        id: 'tx_0002',
        reference: 'refund-0188',
        status: 'REVERSED',
        postedAt: '2026-06-13T09:30:00Z',
    },
]

function ok(body: unknown) {
    return { ok: true, status: 200, json: async () => body } as Response
}

function renderTransactions() {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    const ui: ReactElement = (
        <QueryClientProvider client={client}>
            <MemoryRouter initialEntries={['/transactions']}>
                <ThemeProvider>
                    <Transactions />
                </ThemeProvider>
            </MemoryRouter>
        </QueryClientProvider>
    )
    return render(ui)
}

afterEach(() => {
    vi.restoreAllMocks()
})

describe('Transactions', () => {
    it('shows a loading state while the request is in flight', () => {
        vi.spyOn(globalThis, 'fetch').mockReturnValue(new Promise<Response>(() => {}))
        renderTransactions()
        expect(screen.getByText('Loading transactions...')).toBeInTheDocument()
        expect(screen.queryByRole('table')).not.toBeInTheDocument()
    })

    it('renders a row per transaction with only the real contract fields', async () => {
        vi.spyOn(globalThis, 'fetch').mockResolvedValue(ok(page(SAMPLE, { totalElements: 2 })))
        renderTransactions()
        expect(await screen.findByText('payroll-0425')).toBeInTheDocument()
        expect(screen.getAllByRole('row')).toHaveLength(SAMPLE.length + 1)
        expect(screen.getByText('tx_0001')).toBeInTheDocument()
        expect(screen.getByText('POSTED')).toBeInTheDocument()
        expect(screen.getByText('REVERSED')).toBeInTheDocument()
        expect(screen.queryByText('Amount')).not.toBeInTheDocument()
        expect(screen.queryByText('Currency')).not.toBeInTheDocument()
    })

    it('shows an empty state and disables Next when there are no transactions', async () => {
        vi.spyOn(globalThis, 'fetch').mockResolvedValue(
            ok(page([], { totalElements: 0, totalPages: 0 })),
        )
        renderTransactions()
        expect(await screen.findByText('No transactions yet.')).toBeInTheDocument()
        expect(screen.getByRole('button', { name: 'Next page' })).toBeDisabled()
    })

    it('shows an error state with a working Retry', async () => {
        const fetchMock = vi.spyOn(globalThis, 'fetch').mockRejectedValue(new Error('network down'))
        renderTransactions()
        expect(await screen.findByText('Could not load transactions.')).toBeInTheDocument()
        const before = fetchMock.mock.calls.length
        fireEvent.click(screen.getByRole('button', { name: 'Retry' }))
        await waitFor(() => expect(fetchMock.mock.calls.length).toBeGreaterThan(before))
    })

    it('paginates: Prev disabled on first page, Next refetches with the next page', async () => {
        const fetchMock = vi
            .spyOn(globalThis, 'fetch')
            .mockResolvedValue(ok(page(SAMPLE, { totalElements: 45, totalPages: 3 })))
        renderTransactions()
        await screen.findByText('payroll-0425')
        expect(screen.getByRole('button', { name: 'Previous page' })).toBeDisabled()

        fireEvent.click(screen.getByRole('button', { name: 'Next page' }))
        await waitFor(() => expect(String(fetchMock.mock.calls.at(-1)?.[0])).toContain('page=1'))
    })

    it('marks the Transactions sidebar item as the current page', async () => {
        vi.spyOn(globalThis, 'fetch').mockResolvedValue(ok(page(SAMPLE, { totalElements: 2 })))
        renderTransactions()
        await screen.findByText('payroll-0425')
        expect(screen.getByRole('link', { name: 'Transactions' })).toHaveAttribute(
            'aria-current',
            'page',
        )
    })
})
