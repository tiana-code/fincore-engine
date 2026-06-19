// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import type { ReactElement } from 'react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { DecisionLogResponse } from '@/api/types'
import { ThemeProvider } from '@/components/ThemeProvider'
import { Decisions } from '@/routes/Decisions'

const SAMPLE: DecisionLogResponse[] = [
    {
        id: 'log_0001',
        evaluatedAt: '2026-06-19T10:00:00Z',
        ruleVersionId: 'ver-0001',
        inputHash: 'hash-abc',
        matched: true,
        outcomeLabel: 'APPROVE',
    },
    {
        id: 'log_0002',
        evaluatedAt: '2026-06-19T10:05:00Z',
        ruleVersionId: 'ver-0001',
        inputHash: 'hash-abc',
        matched: false,
        outcomeLabel: null,
    },
]

function ok(body: unknown) {
    return { ok: true, status: 200, json: async () => body } as Response
}

function renderDecisions() {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    const ui: ReactElement = (
        <QueryClientProvider client={client}>
            <MemoryRouter initialEntries={['/decisions']}>
                <ThemeProvider>
                    <Decisions />
                </ThemeProvider>
            </MemoryRouter>
        </QueryClientProvider>
    )
    return render(ui)
}

function search(value: string) {
    fireEvent.change(screen.getByLabelText('Query value'), { target: { value } })
    fireEvent.click(screen.getByRole('button', { name: 'Search' }))
}

afterEach(() => {
    vi.restoreAllMocks()
})

describe('Decisions', () => {
    it('shows an idle prompt and does not fetch before a search', () => {
        const fetchMock = vi.spyOn(globalThis, 'fetch')
        renderDecisions()
        expect(
            screen.getByText('Enter an input hash or rule version id to search the decision log.'),
        ).toBeInTheDocument()
        expect(fetchMock).not.toHaveBeenCalled()
    })

    it('queries by input hash and renders a row per log', async () => {
        const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(ok(SAMPLE))
        renderDecisions()
        search('hash-abc')
        expect(await screen.findByText('log_0001')).toBeInTheDocument()
        expect(screen.getAllByRole('row')).toHaveLength(SAMPLE.length + 1)
        expect(String(fetchMock.mock.calls.at(-1)?.[0])).toContain('inputHash=hash-abc')
    })

    it('renders the matched state for each log', async () => {
        vi.spyOn(globalThis, 'fetch').mockResolvedValue(ok(SAMPLE))
        renderDecisions()
        search('hash-abc')
        expect(await screen.findByText('MATCHED')).toBeInTheDocument()
        expect(screen.getByText('NO MATCH')).toBeInTheDocument()
    })

    it('queries by rule version id when that field is selected', async () => {
        const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(ok(SAMPLE))
        renderDecisions()
        fireEvent.click(screen.getByRole('tab', { name: 'Rule version id' }))
        search('ver-0001')
        await waitFor(() =>
            expect(String(fetchMock.mock.calls.at(-1)?.[0])).toContain('ruleVersionId=ver-0001'),
        )
    })

    it('shows an empty state when the query returns no logs', async () => {
        vi.spyOn(globalThis, 'fetch').mockResolvedValue(ok([]))
        renderDecisions()
        search('hash-none')
        expect(await screen.findByText('No decision logs for this query.')).toBeInTheDocument()
    })

    it('shows an error state with a working Retry', async () => {
        const fetchMock = vi.spyOn(globalThis, 'fetch').mockRejectedValue(new Error('network down'))
        renderDecisions()
        search('hash-abc')
        expect(await screen.findByText('Could not load decision logs.')).toBeInTheDocument()
        const before = fetchMock.mock.calls.length
        fireEvent.click(screen.getByRole('button', { name: 'Retry' }))
        await waitFor(() => expect(fetchMock.mock.calls.length).toBeGreaterThan(before))
    })

    it('marks the Decisions sidebar item as the current page', () => {
        renderDecisions()
        expect(screen.getByRole('link', { name: 'Decisions' })).toHaveAttribute(
            'aria-current',
            'page',
        )
    })
})
