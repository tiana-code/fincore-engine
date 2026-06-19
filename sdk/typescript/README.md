# @fincore/sdk-typescript

A typed `fetch` client over the FinCore Engine REST API. Isomorphic (works in Node 18+ and browsers via the global `fetch`), with no runtime dependencies.

```ts
import { FincoreClient } from '@fincore/sdk-typescript'

const client = new FincoreClient({ baseUrl: 'http://localhost:8080', token: '<jwt>' })

const page = await client.listAccounts()
const account = await client.getAccount(page.items[0].id)
```

A non-2xx response throws `FincoreError` carrying the HTTP `status`. The bearer token is sent only when provided.

The current surface covers the ledger account reads; further operations are tracked as follow-ups. This package is not published to a registry.

SPDX-License-Identifier: BUSL-1.1
