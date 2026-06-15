// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { TransactionDetailResponse } from '@/api/types'
import { ThemeProvider } from '@/components/ThemeProvider'
import { TransactionDetail } from '@/routes/TransactionDetail'

const DETAIL: TransactionDetailResponse = {
    id: 'tx_0001',
    reference: 'payroll-0425',
    description: 'monthly payroll',
    status: 'POSTED',
    reversesId: null,
    postedAt: '2026-06-13T10:00:00Z',
    entries: [
        { accountId: 'acc_0001', direction: 'DEBIT', amount: '1234567.89', currency: 'EUR' },
        { accountId: 'acc_0002', direction: 'CREDIT', amount: '-1234567.89', currency: 'EUR' },
    ],
}

function ok(body: unknown) {
    return { ok: true, status: 200, json: async () => body } as Response
}
function fail(status: number) {
    return { ok: false, status, json: async () => ({}) } as Response
}

function renderDetail(id = 'tx_0001') {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    return render(
        <QueryClientProvider client={client}>
            <MemoryRouter initialEntries={[`/transactions/${id}`]}>
                <ThemeProvider>
                    <Routes>
                        <Route path="/transactions/:id" element={<TransactionDetail />} />
                    </Routes>
                </ThemeProvider>
            </MemoryRouter>
        </QueryClientProvider>,
    )
}

afterEach(() => {
    vi.restoreAllMocks()
})

describe('TransactionDetail', () => {
    it('renders the header and entries with losslessly formatted amounts', async () => {
        vi.spyOn(globalThis, 'fetch').mockResolvedValue(ok(DETAIL))
        renderDetail()
        expect(await screen.findByText('payroll-0425')).toBeInTheDocument()
        expect(screen.getByText('POSTED')).toBeInTheDocument()
        expect(screen.getByText('1,234,567.89')).toBeInTheDocument()
        expect(screen.getByText('-1,234,567.89')).toBeInTheDocument()
        expect(screen.getByText('DEBIT')).toBeInTheDocument()
        expect(screen.getByText('CREDIT')).toBeInTheDocument()
        expect(screen.getByRole('link', { name: 'acc_0001' })).toHaveAttribute(
            'href',
            '/accounts/acc_0001',
        )
    })

    it('links to the reversed original when reversesId is present', async () => {
        vi.spyOn(globalThis, 'fetch').mockResolvedValue(
            ok({ ...DETAIL, status: 'POSTED', reversesId: 'tx_9999' }),
        )
        renderDetail()
        await screen.findByText('payroll-0425')
        expect(screen.getByRole('link', { name: 'tx_9999' })).toHaveAttribute(
            'href',
            '/transactions/tx_9999',
        )
    })

    it('shows a not-found state on 404', async () => {
        vi.spyOn(globalThis, 'fetch').mockResolvedValue(fail(404))
        renderDetail()
        expect(await screen.findByText('Transaction not found.')).toBeInTheDocument()
    })

    it('shows an error state with a working Retry', async () => {
        const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(fail(500))
        renderDetail()
        expect(await screen.findByText('Could not load transaction.')).toBeInTheDocument()
        const before = fetchMock.mock.calls.length
        fireEvent.click(screen.getByRole('button', { name: 'Retry' }))
        await waitFor(() => expect(fetchMock.mock.calls.length).toBeGreaterThan(before))
    })
})
