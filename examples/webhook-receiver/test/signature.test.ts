// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { describe, expect, it } from 'vitest'
import { computeSignature, verifySignature } from '../src/signature.js'

const secret = 'whsec-example'
const payload = '{"deliveryId":"whd_1","providerReference":"pr_1","outcome":"SETTLED"}'

describe('verifySignature', () => {
    it('accepts a signature computed with the shared secret', () => {
        expect(verifySignature(secret, payload, computeSignature(secret, payload))).toBe(true)
    })

    it('accepts an uppercase signature, since the server lowercases it', () => {
        expect(verifySignature(secret, payload, computeSignature(secret, payload).toUpperCase())).toBe(true)
    })

    it('rejects a tampered payload', () => {
        const signature = computeSignature(secret, payload)
        expect(verifySignature(secret, `${payload} `, signature)).toBe(false)
    })

    it('rejects a wrong-length signature', () => {
        expect(verifySignature(secret, payload, 'deadbeef')).toBe(false)
    })

    it('rejects everything when the secret is blank (fail-closed)', () => {
        expect(verifySignature('', payload, computeSignature('any', payload))).toBe(false)
    })
})
