// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { ThemeProvider } from '@/components/ThemeProvider'
import type { OverviewData } from '@/mock/types'
import { Overview } from '@/routes/Overview'

vi.mock('@/mock/overview', () => ({
  getOverview: (): OverviewData => ({
    status: { title: 'Sandbox running locally', version: 'v9', build: 'abc123', uptime: '1m', user: 'Tester', tenant: 'demo-tenant' },
    services: [{ name: 'postgres', version: '17', ok: true }],
    kpis: [{ label: 'Transactions · today', value: '42', sub: [{ text: '+1%', tone: 'credit' }], spark: [1, 2, 3] }],
    activity: [{ type: 'transaction.posted', detail: 'tx_demo', ts: '2s ago', icon: 'arrows', tone: 'neutral' }],
    apiExamples: [{ title: 'Create a transaction', endpoint: 'POST /v1/transactions', icon: 'arrows', curl: 'curl ...' }],
    systemLinks: [{ title: 'Swagger UI', sub: 'docs', url: 'localhost:8080/swagger', icon: 'code' }],
  }),
}))

describe('Overview', () => {
  it('renders the stubbed landing data without crashing', () => {
    render(
      <ThemeProvider>
        <Overview />
      </ThemeProvider>,
    )
    expect(screen.getByText('Transactions · today')).toBeInTheDocument()
    expect(screen.getByText('transaction.posted')).toBeInTheDocument()
    expect(screen.getByText('Create a transaction')).toBeInTheDocument()
  })
})
