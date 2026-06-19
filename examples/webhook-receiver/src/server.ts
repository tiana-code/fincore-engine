// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { createServer, type Server } from 'node:http'
import { verifySignature } from './signature.js'

const SIGNATURE_HEADER = 'x-webhook-signature'
const HTTP_OK = 200
const HTTP_UNAUTHORIZED = 401

function describe(rawBody: string): string {
    try {
        const event = JSON.parse(rawBody) as { deliveryId?: string; outcome?: string }
        return `deliveryId=${event.deliveryId ?? '?'} outcome=${event.outcome ?? '?'}`
    } catch {
        return `${rawBody.length} bytes (unparseable)`
    }
}

export function createReceiver(secret: string): Server {
    return createServer((req, res) => {
        const chunks: Buffer[] = []
        req.on('data', (chunk: Buffer) => chunks.push(chunk))
        req.on('end', () => {
            const rawBody = Buffer.concat(chunks).toString('utf8')
            const header = req.headers[SIGNATURE_HEADER]
            const provided = Array.isArray(header) ? (header[0] ?? '') : (header ?? '')
            const verified = verifySignature(secret, rawBody, provided)
            console.log(`${verified ? 'VERIFIED' : 'REJECTED'} ${describe(rawBody)}`)
            res.writeHead(verified ? HTTP_OK : HTTP_UNAUTHORIZED).end()
        })
    })
}
