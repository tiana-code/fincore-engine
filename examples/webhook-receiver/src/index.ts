// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { createReceiver } from './server.js'

const DEFAULT_PORT = 9099

const secret = process.env.FINCORE_WEBHOOK_SECRET ?? ''
const port = Number(process.env.PORT ?? DEFAULT_PORT)

if (secret.trim().length === 0) {
    console.warn('FINCORE_WEBHOOK_SECRET is not set; every delivery will be rejected (fail-closed)')
}

createReceiver(secret).listen(port, () => {
    console.log(`webhook receiver listening on http://localhost:${port}`)
})
