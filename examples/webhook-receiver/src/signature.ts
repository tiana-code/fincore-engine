// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { createHmac, timingSafeEqual } from 'node:crypto'

// Mirrors services/payments WebhookSignatureVerifier: lowercase hex of HMAC-SHA256 over the raw body,
// constant-time compare, fail-closed on a blank secret.
export function computeSignature(secret: string, payload: string): string {
    return createHmac('sha256', secret).update(payload, 'utf8').digest('hex')
}

export function verifySignature(secret: string, payload: string, providedHex: string): boolean {
    if (secret.trim().length === 0) {
        return false
    }
    const expected = Buffer.from(computeSignature(secret, payload), 'utf8')
    const provided = Buffer.from(providedHex.trim().toLowerCase(), 'utf8')
    if (expected.length !== provided.length) {
        return false
    }
    return timingSafeEqual(expected, provided)
}
