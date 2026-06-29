// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import type { ApiExample, SystemLink } from '@/mock/types'

// Static reference content (not data): example requests and links to local tooling.
export const API_EXAMPLES: ApiExample[] = [
    {
        title: 'Create a transaction',
        endpoint: 'POST /v1/transactions',
        icon: 'arrows',
        curl: 'curl -X POST $URL/v1/transactions \\\n  -H "Idempotency-Key: ..."',
    },
    {
        title: 'Initiate a payment',
        endpoint: 'POST /v1/payments',
        icon: 'send',
        curl: 'curl -X POST $URL/v1/payments \\\n  -H "Idempotency-Key: ..."',
    },
    {
        title: 'Evaluate a decision rule',
        endpoint: 'POST /v1/decisions:evaluate',
        icon: 'scale',
        curl: 'curl -X POST $URL/v1/decisions:evaluate \\\n  -d \'{"ruleset":"..."}\'',
    },
    {
        title: 'Open a compliance case',
        endpoint: 'POST /v1/compliance/cases',
        icon: 'shield',
        curl: 'curl -X POST $URL/v1/compliance/cases',
    },
]

export const SYSTEM_LINKS: SystemLink[] = [
    {
        title: 'Swagger UI',
        sub: 'API docs · try requests',
        url: 'localhost:8080/swagger',
        icon: 'code',
    },
    {
        title: 'Grafana dashboards',
        sub: 'Metrics · traces · logs',
        url: 'localhost:3000',
        icon: 'activity',
    },
    {
        title: 'Keycloak admin',
        sub: 'Realms · users · clients',
        url: 'localhost:8081/admin',
        icon: 'key',
    },
]
