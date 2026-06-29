// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import type { ReactElement } from 'react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { LedgerOverviewResponse } from '@/api/types'
import { ThemeProvider } from '@/components/ThemeProvider'
import { Overview } from '@/routes/Overview'

function ok(body: unknown) {
    return { ok: true, status: 200, json: async () => body } as Response
}

function page(totalElements: number) {
    return ok({ items: [], page: 0, size: 1, totalElements, totalPages: totalElements })
}

const OVERVIEW: LedgerOverviewResponse = {
    activity: [
        {
            type: 'transaction.posted',
            resourceId: 'tx_demo123',
            label: 'wallet top-up',
            amount: '500.00',
            currency: 'USD',
            occurredAt: '2026-06-28T10:00:00Z',
        },
    ],
    transactionsLast24h: [0, 0, 1, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1],
}

function mockApi(overview: LedgerOverviewResponse) {
    vi.spyOn(globalThis, 'fetch').mockImplementation((input) => {
        const url = String(input)
        if (url.includes('/v1/overview')) return Promise.resolve(ok(overview))
        if (url.includes('/v1/transactions')) return Promise.resolve(page(1247))
        if (url.includes('/v1/payments')) return Promise.resolve(page(8))
        if (url.includes('/v1/accounts')) return Promise.resolve(page(53))
        if (url.includes('/v1/compliance/cases')) {
            return Promise.resolve(ok([{ id: 'case_1', reference: 'c1', status: 'OPEN' }]))
        }
        return Promise.reject(new Error(`unexpected url ${url}`))
    })
}

function renderOverview() {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    const ui: ReactElement = (
        <QueryClientProvider client={client}>
            <MemoryRouter>
                <ThemeProvider>
                    <Overview />
                </ThemeProvider>
            </MemoryRouter>
        </QueryClientProvider>
    )
    return render(ui)
}

afterEach(() => {
    vi.restoreAllMocks()
})

describe('Overview', () => {
    it('renders live KPIs and the real activity feed when the sandbox is reachable', async () => {
        mockApi(OVERVIEW)
        renderOverview()

        expect(await screen.findByText('Live data.')).toBeInTheDocument()
        expect(screen.getByText('Transactions · total')).toBeInTheDocument()
        expect(screen.getByText('1,247')).toBeInTheDocument()
        expect(await screen.findByText('transaction.posted')).toBeInTheDocument()
        expect(screen.getByText(/tx_demo123/)).toBeInTheDocument()
    })

    it('shows the offline notice and dash placeholders when the API is unreachable', async () => {
        vi.spyOn(globalThis, 'fetch').mockRejectedValue(new Error('network down'))
        renderOverview()

        expect((await screen.findAllByText('Sandbox API not reachable.')).length).toBeGreaterThan(0)
        expect(screen.getByText('Transactions · total')).toBeInTheDocument()
        expect(screen.getAllByText('unavailable').length).toBeGreaterThan(0)
    })

    it('shows an empty-feed message when the ledger has no activity', async () => {
        mockApi({ activity: [], transactionsLast24h: new Array(24).fill(0) })
        renderOverview()

        expect(await screen.findByText('No recent ledger activity.')).toBeInTheDocument()
    })
})
