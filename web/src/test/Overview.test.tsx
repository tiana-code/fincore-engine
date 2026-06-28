// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import type { ReactElement } from 'react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ThemeProvider } from '@/components/ThemeProvider'
import type { OverviewData } from '@/mock/types'
import { Overview } from '@/routes/Overview'

vi.mock('@/mock/overview', () => ({
    getOverview: (): OverviewData => ({
        status: {
            title: 'Sandbox running locally',
            version: 'v9',
            build: 'abc123',
            uptime: '1m',
            user: 'Tester',
            tenant: 'demo-tenant',
        },
        services: [{ name: 'postgres', version: '17', ok: true }],
        kpis: [
            {
                label: 'Transactions · sample',
                value: '42',
                sub: [{ text: '+1%', tone: 'credit' }],
                spark: [1, 2, 3],
            },
        ],
        activity: [
            {
                type: 'transaction.posted',
                detail: 'tx_demo',
                ts: '2s ago',
                icon: 'arrows',
                tone: 'neutral',
            },
        ],
        apiExamples: [
            {
                title: 'Create a transaction',
                endpoint: 'POST /v1/transactions',
                icon: 'arrows',
                curl: 'curl ...',
            },
        ],
        systemLinks: [
            { title: 'Swagger UI', sub: 'docs', url: 'localhost:8080/swagger', icon: 'code' },
        ],
    }),
}))

function ok(body: unknown) {
    return { ok: true, status: 200, json: async () => body } as Response
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
    it('falls back to sample KPIs and the offline notice when the API is unreachable', async () => {
        vi.spyOn(globalThis, 'fetch').mockRejectedValue(new Error('network down'))
        renderOverview()
        expect(await screen.findByText('Sandbox API not reachable.')).toBeInTheDocument()
        expect(screen.getByText('Transactions · sample')).toBeInTheDocument()
        // Activity feed and API playground stay illustrative regardless of mode.
        expect(screen.getByText('transaction.posted')).toBeInTheDocument()
        expect(screen.getByText('Create a transaction')).toBeInTheDocument()
    })

    it('renders live KPIs from the API counts when the sandbox is reachable', async () => {
        vi.spyOn(globalThis, 'fetch').mockImplementation((input) => {
            const url = String(input)
            if (url.includes('/v1/transactions')) {
                return Promise.resolve(
                    ok({ items: [], page: 0, size: 1, totalElements: 1247, totalPages: 1247 }),
                )
            }
            if (url.includes('/v1/payments')) {
                return Promise.resolve(
                    ok({ items: [], page: 0, size: 1, totalElements: 8, totalPages: 8 }),
                )
            }
            if (url.includes('/v1/accounts')) {
                return Promise.resolve(
                    ok({ items: [], page: 0, size: 1, totalElements: 53, totalPages: 53 }),
                )
            }
            if (url.includes('/v1/compliance/cases')) {
                return Promise.resolve(
                    ok([
                        { id: 'case_1', reference: 'c1', status: 'OPEN' },
                        { id: 'case_2', reference: 'c2', status: 'OPEN' },
                        { id: 'case_3', reference: 'c3', status: 'OPEN' },
                    ]),
                )
            }
            return Promise.reject(new Error(`unexpected url ${url}`))
        })
        renderOverview()
        expect(await screen.findByText('Live metrics.')).toBeInTheDocument()
        expect(screen.getByText('Transactions · total')).toBeInTheDocument()
        expect(screen.getByText('1,247')).toBeInTheDocument()
        expect(screen.getByText('Compliance · open')).toBeInTheDocument()
        // Sample KPI label must be gone once live data has loaded.
        expect(screen.queryByText('Transactions · sample')).not.toBeInTheDocument()
    })
})
