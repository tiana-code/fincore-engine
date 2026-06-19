// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import type { ReactElement } from 'react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { PageResponse, PaymentResponse } from '@/api/types'
import { ThemeProvider } from '@/components/ThemeProvider'
import { Payments } from '@/routes/Payments'

function page(
    items: PaymentResponse[],
    over: Partial<PageResponse<PaymentResponse>> = {},
): PageResponse<PaymentResponse> {
    return { items, page: 0, size: 20, totalElements: items.length, totalPages: 1, ...over }
}

const SAMPLE: PaymentResponse[] = [
    { id: 'pay_0001', reference: 'order-1', amount: 100.0, currency: 'USD', status: 'INITIATED' },
    { id: 'pay_0002', reference: 'order-2', amount: 250.5, currency: 'EUR', status: 'SETTLED' },
]

function ok(body: unknown) {
    return { ok: true, status: 200, json: async () => body } as Response
}

function renderPayments() {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    const ui: ReactElement = (
        <QueryClientProvider client={client}>
            <MemoryRouter initialEntries={['/payments']}>
                <ThemeProvider>
                    <Payments />
                </ThemeProvider>
            </MemoryRouter>
        </QueryClientProvider>
    )
    return render(ui)
}

afterEach(() => {
    vi.restoreAllMocks()
})

describe('Payments', () => {
    it('shows a loading state while the request is in flight', () => {
        vi.spyOn(globalThis, 'fetch').mockReturnValue(new Promise<Response>(() => {}))
        renderPayments()
        expect(screen.getByText('Loading payments...')).toBeInTheDocument()
        expect(screen.queryByRole('table')).not.toBeInTheDocument()
    })

    it('renders a row per payment with only the real contract fields', async () => {
        vi.spyOn(globalThis, 'fetch').mockResolvedValue(ok(page(SAMPLE, { totalElements: 2 })))
        renderPayments()
        expect(await screen.findByText('order-1')).toBeInTheDocument()
        expect(screen.getAllByRole('row')).toHaveLength(SAMPLE.length + 1)
        expect(screen.getByText('pay_0001')).toBeInTheDocument()
        expect(screen.getByText('SETTLED')).toBeInTheDocument()
        expect(screen.queryByText('Provider')).not.toBeInTheDocument()
    })

    it('shows an empty state and disables Next when there are no payments', async () => {
        vi.spyOn(globalThis, 'fetch').mockResolvedValue(
            ok(page([], { totalElements: 0, totalPages: 0 })),
        )
        renderPayments()
        expect(await screen.findByText('No payments yet.')).toBeInTheDocument()
        expect(screen.getByRole('button', { name: 'Next page' })).toBeDisabled()
    })

    it('shows an error state with a working Retry', async () => {
        const fetchMock = vi.spyOn(globalThis, 'fetch').mockRejectedValue(new Error('network down'))
        renderPayments()
        expect(await screen.findByText('Could not load payments.')).toBeInTheDocument()
        const before = fetchMock.mock.calls.length
        fireEvent.click(screen.getByRole('button', { name: 'Retry' }))
        await waitFor(() => expect(fetchMock.mock.calls.length).toBeGreaterThan(before))
    })

    it('paginates: Prev disabled on first page, Next refetches with the next page', async () => {
        const fetchMock = vi
            .spyOn(globalThis, 'fetch')
            .mockResolvedValue(ok(page(SAMPLE, { totalElements: 45, totalPages: 3 })))
        renderPayments()
        await screen.findByText('order-1')
        expect(screen.getByRole('button', { name: 'Previous page' })).toBeDisabled()

        fireEvent.click(screen.getByRole('button', { name: 'Next page' }))
        await waitFor(() => expect(String(fetchMock.mock.calls.at(-1)?.[0])).toContain('page=1'))
    })

    it('marks the Payments sidebar item as the current page', async () => {
        vi.spyOn(globalThis, 'fetch').mockResolvedValue(ok(page(SAMPLE, { totalElements: 2 })))
        renderPayments()
        await screen.findByText('order-1')
        expect(screen.getByRole('link', { name: 'Payments' })).toHaveAttribute(
            'aria-current',
            'page',
        )
    })
})
