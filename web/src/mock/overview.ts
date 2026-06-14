// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { genSeries } from '@/lib/series'
import type { OverviewData } from './types'

// Synthetic sandbox data. Swap getOverview() for a TanStack Query hook against the REST API per screen.
const OVERVIEW: OverviewData = {
  status: {
    title: 'Sandbox running locally',
    version: 'v0.4.0',
    build: 'c4f9e2',
    uptime: '14h 21m',
    user: 'Lena',
    tenant: 'mercator-eu-prod',
  },
  services: [
    { name: 'postgres', version: '17.0', ok: true },
    { name: 'redpanda', version: '24.1', ok: true },
    { name: 'keycloak', version: '26.6', ok: true },
    { name: 'redis', version: '7.4', ok: true },
  ],
  kpis: [
    {
      label: 'Transactions · today',
      value: '1,247',
      sub: [
        { text: '+18%', tone: 'credit' },
        { text: 'vs yesterday', tone: 'neutral' },
      ],
      spark: genSeries(24, 100, 0.06, 0.015),
    },
    {
      label: 'Payments · in flight',
      value: '8',
      sub: [{ text: '4 SEPA · 3 INTERNAL · 1 SWIFT', tone: 'neutral' }],
      spark: genSeries(24, 10, 0.18, 0.001),
    },
    {
      label: 'Compliance · open',
      value: '3',
      sub: [
        { text: '1 P0', tone: 'debit' },
        { text: '2 P1', tone: 'amber' },
      ],
      spark: genSeries(24, 5, 0.15, 0),
      sparkColor: 'var(--amber)',
    },
    {
      label: 'Decision latency · p50',
      value: '4.2ms',
      sub: [
        { text: '-0.3ms', tone: 'credit' },
        { text: '· 5,841 evals', tone: 'neutral' },
      ],
      spark: genSeries(24, 4, 0.08, -0.001),
    },
  ],
  activity: [
    { type: 'transaction.posted', detail: 'tx_01HXVK4T8P · 1,240.00 EUR · acc_01HXT4M9 -> acc_01HXT4P1', ts: '2s ago', icon: 'arrows', tone: 'neutral' },
    { type: 'payment.initiated', detail: 'pmt_01HXVW9PMR · 1,200.00 EUR · SEPA SCT', ts: '14s ago', icon: 'send', tone: 'accent' },
    { type: 'decision.evaluated', detail: 'dec_01HXVK1MFT · aml_velocity@v3 · REVIEW · 62.4ms', ts: '38s ago', icon: 'scale', tone: 'amber' },
    { type: 'account.created', detail: 'acc_01HXVW2KFN · Olamide Adekunle · USER_WALLET · EUR', ts: '1m ago', icon: 'wallet', tone: 'credit' },
    { type: 'compliance.case.opened', detail: 'case_01HXVB7M · AML hit · P0', ts: '2m ago', icon: 'shield', tone: 'debit' },
    { type: 'decision.evaluated', detail: 'dec_01HXVJ7W · sanctions_screen@2.1 · DENY · 4.6ms', ts: '3m ago', icon: 'scale', tone: 'debit' },
    { type: 'transaction.posted', detail: 'tx_01HXVH9N · 4,200.00 EUR · payroll-batch-052', ts: '4m ago', icon: 'arrows', tone: 'neutral' },
    { type: 'payment.settled', detail: 'pmt_01HXSL3M · 1,200.00 EUR · Casa Rosa Ortega', ts: '6m ago', icon: 'check', tone: 'credit' },
    { type: 'account.frozen', detail: 'acc_01HXT4P9 · velocity rule trip', ts: '8m ago', icon: 'key', tone: 'amber' },
    { type: 'kyc.refresh.completed', detail: 'kyc_01HXVQ3M · 4 docs verified', ts: '12m ago', icon: 'shield', tone: 'credit' },
  ],
  apiExamples: [
    { title: 'Create a transaction', endpoint: 'POST /v1/transactions', icon: 'arrows', curl: 'curl -X POST $URL/v1/transactions \\\n  -H "Idempotency-Key: ..."' },
    { title: 'Initiate a payment', endpoint: 'POST /v1/payments', icon: 'send', curl: 'curl -X POST $URL/v1/payments \\\n  -H "Idempotency-Key: ..."' },
    { title: 'Evaluate a decision rule', endpoint: 'POST /v1/decisions:evaluate', icon: 'scale', curl: 'curl -X POST $URL/v1/decisions:evaluate \\\n  -d \'{"ruleset":"..."}\'' },
    { title: 'Open a compliance case', endpoint: 'POST /v1/compliance/cases', icon: 'shield', curl: 'curl -X POST $URL/v1/compliance/cases' },
  ],
  systemLinks: [
    { title: 'Swagger UI', sub: 'API docs · try requests', url: 'localhost:8080/swagger', icon: 'code' },
    { title: 'Grafana dashboards', sub: 'Metrics · traces · logs', url: 'localhost:3000', icon: 'activity' },
    { title: 'Keycloak admin', sub: 'Realms · users · clients', url: 'localhost:8081/admin', icon: 'key' },
  ],
}

export function getOverview(): OverviewData {
  return OVERVIEW
}
