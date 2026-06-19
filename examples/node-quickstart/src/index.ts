// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { FincoreClient } from '@fincore/sdk-typescript'

const baseUrl = process.env.FINCORE_API_URL ?? 'http://localhost:8080'
const token = process.env.FINCORE_TOKEN

async function main(): Promise<void> {
    const client = new FincoreClient({ baseUrl, token })

    const page = await client.listAccounts()
    console.log(`accounts: ${page.totalElements} total (page ${page.page + 1} of ${page.totalPages})`)
    for (const account of page.items) {
        console.log(`  ${account.id}  ${account.type.padEnd(11)} ${account.currency}  ${account.name}`)
    }

    const first = page.items[0]
    if (first) {
        const account = await client.getAccount(first.id)
        console.log(`fetched ${account.id}: ${account.name} (${account.status})`)
    }
}

main().catch((error: unknown) => {
    console.error(error)
    process.exitCode = 1
})
