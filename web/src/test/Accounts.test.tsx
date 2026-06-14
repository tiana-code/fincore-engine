// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import type { ReactElement } from 'react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { AccountResponse, PageResponse } from '@/api/types'
import { ThemeProvider } from '@/components/ThemeProvider'
import { Accounts } from '@/routes/Accounts'

function page(
    items: AccountResponse[],
    over: Partial<PageResponse<AccountResponse>> = {},
): PageResponse<AccountResponse> {
    return { items, page: 0, size: 20, totalElements: items.length, totalPages: 1, ...over }
}

const SAMPLE: AccountResponse[] = [
    { id: 'acc_0001', name: 'Operating Cash', type: 'ASSET', currency: 'USD', status: 'ACTIVE' },
    {
        id: 'acc_0002',
        name: 'Customer Wallet',
        type: 'USER_WALLET',
        currency: 'EUR',
        status: 'FROZEN',
    },
    { id: 'acc_0003', name: 'Fee Income', type: 'REVENUE', currency: 'GBP', status: 'CLOSED' },
]

function ok(body: unknown) {
    return { ok: true, status: 200, json: async () => body } as Response
}

function renderAccounts() {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    const ui: ReactElement = (
        <QueryClientProvider client={client}>
            <MemoryRouter initialEntries={['/accounts']}>
                <ThemeProvider>
                    <Accounts />
                </ThemeProvider>
            </MemoryRouter>
        </QueryClientProvider>
    )
    return render(ui)
}

afterEach(() => {
    vi.restoreAllMocks()
})

describe('Accounts', () => {
    it('shows a loading state while the request is in flight', () => {
        vi.spyOn(globalThis, 'fetch').mockReturnValue(new Promise<Response>(() => {}))
        renderAccounts()
        expect(screen.getByText('Loading accounts...')).toBeInTheDocument()
        expect(screen.queryByRole('table')).not.toBeInTheDocument()
    })

    it('renders a row per account with only the real contract fields', async () => {
        vi.spyOn(globalThis, 'fetch').mockResolvedValue(ok(page(SAMPLE, { totalElements: 3 })))
        renderAccounts()
        expect(await screen.findByText('Operating Cash')).toBeInTheDocument()
        expect(screen.getAllByRole('row')).toHaveLength(SAMPLE.length + 1)
        expect(screen.getByText('acc_0001')).toBeInTheDocument()
        expect(screen.getByText('ACTIVE')).toBeInTheDocument()
        expect(screen.getByText('USER_WALLET')).toBeInTheDocument()
        expect(screen.queryByText('Balance')).not.toBeInTheDocument()
        expect(screen.queryByText('Owner')).not.toBeInTheDocument()
        expect(screen.queryByText('Created')).not.toBeInTheDocument()
    })

    it('shows an empty state and disables Next when there are no accounts', async () => {
        vi.spyOn(globalThis, 'fetch').mockResolvedValue(
            ok(page([], { totalElements: 0, totalPages: 0 })),
        )
        renderAccounts()
        expect(await screen.findByText('No accounts yet.')).toBeInTheDocument()
        expect(screen.getByRole('button', { name: 'Next page' })).toBeDisabled()
    })

    it('shows an error state with a working Retry', async () => {
        const fetchMock = vi.spyOn(globalThis, 'fetch').mockRejectedValue(new Error('network down'))
        renderAccounts()
        expect(await screen.findByText('Could not load accounts.')).toBeInTheDocument()
        const before = fetchMock.mock.calls.length
        fireEvent.click(screen.getByRole('button', { name: 'Retry' }))
        await waitFor(() => expect(fetchMock.mock.calls.length).toBeGreaterThan(before))
    })

    it('paginates: Prev disabled on first page, Next refetches with the next page', async () => {
        const fetchMock = vi
            .spyOn(globalThis, 'fetch')
            .mockResolvedValue(ok(page(SAMPLE, { totalElements: 45, totalPages: 3 })))
        renderAccounts()
        await screen.findByText('Operating Cash')
        expect(screen.getByRole('button', { name: 'Previous page' })).toBeDisabled()

        fireEvent.click(screen.getByRole('button', { name: 'Next page' }))
        await waitFor(() => expect(String(fetchMock.mock.calls.at(-1)?.[0])).toContain('page=1'))
    })

    it('marks the Accounts sidebar item as the current page', async () => {
        vi.spyOn(globalThis, 'fetch').mockResolvedValue(ok(page(SAMPLE, { totalElements: 3 })))
        renderAccounts()
        await screen.findByText('Operating Cash')
        expect(screen.getByRole('link', { name: 'Accounts' })).toHaveAttribute(
            'aria-current',
            'page',
        )
    })
})
