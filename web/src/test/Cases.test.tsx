// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import type { ReactElement } from 'react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { CaseResponse } from '@/api/types'
import { ThemeProvider } from '@/components/ThemeProvider'
import { Cases } from '@/routes/Cases'

const SAMPLE: CaseResponse[] = [
    { id: 'case_0001', reference: 'case-ref-1', status: 'OPEN' },
    { id: 'case_0002', reference: 'case-ref-2', status: 'OPEN' },
]

function ok(body: unknown) {
    return { ok: true, status: 200, json: async () => body } as Response
}

function renderCases() {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    const ui: ReactElement = (
        <QueryClientProvider client={client}>
            <MemoryRouter initialEntries={['/compliance/cases']}>
                <ThemeProvider>
                    <Cases />
                </ThemeProvider>
            </MemoryRouter>
        </QueryClientProvider>
    )
    return render(ui)
}

afterEach(() => {
    vi.restoreAllMocks()
})

describe('Cases', () => {
    it('shows a loading state while the request is in flight', () => {
        vi.spyOn(globalThis, 'fetch').mockReturnValue(new Promise<Response>(() => {}))
        renderCases()
        expect(screen.getByText('Loading cases...')).toBeInTheDocument()
        expect(screen.queryByRole('table')).not.toBeInTheDocument()
    })

    it('renders a row per case with only the real contract fields', async () => {
        vi.spyOn(globalThis, 'fetch').mockResolvedValue(ok(SAMPLE))
        renderCases()
        expect(await screen.findByText('case-ref-1')).toBeInTheDocument()
        expect(screen.getAllByRole('row')).toHaveLength(SAMPLE.length + 1)
        expect(screen.getByText('case_0001')).toBeInTheDocument()
        expect(screen.queryByText('Assignee')).not.toBeInTheDocument()
    })

    it('defaults to the OPEN status filter', async () => {
        const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(ok(SAMPLE))
        renderCases()
        await screen.findByText('case-ref-1')
        expect(String(fetchMock.mock.calls.at(-1)?.[0])).toContain('status=OPEN')
    })

    it('refetches with the selected status when a tab is clicked', async () => {
        const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(ok(SAMPLE))
        renderCases()
        await screen.findByText('case-ref-1')

        fireEvent.click(screen.getByRole('tab', { name: 'RESOLVED' }))
        await waitFor(() =>
            expect(String(fetchMock.mock.calls.at(-1)?.[0])).toContain('status=RESOLVED'),
        )
    })

    it('shows an empty state when there are no cases in the status', async () => {
        vi.spyOn(globalThis, 'fetch').mockResolvedValue(ok([]))
        renderCases()
        expect(await screen.findByText('No cases in this status.')).toBeInTheDocument()
    })

    it('shows an error state with a working Retry', async () => {
        const fetchMock = vi.spyOn(globalThis, 'fetch').mockRejectedValue(new Error('network down'))
        renderCases()
        expect(await screen.findByText('Could not load cases.')).toBeInTheDocument()
        const before = fetchMock.mock.calls.length
        fireEvent.click(screen.getByRole('button', { name: 'Retry' }))
        await waitFor(() => expect(fetchMock.mock.calls.length).toBeGreaterThan(before))
    })

    it('marks the Compliance sidebar item as the current page', async () => {
        vi.spyOn(globalThis, 'fetch').mockResolvedValue(ok(SAMPLE))
        renderCases()
        await screen.findByText('case-ref-1')
        expect(screen.getByRole('link', { name: 'Compliance' })).toHaveAttribute(
            'aria-current',
            'page',
        )
    })
})
