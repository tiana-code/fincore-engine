// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { describe, expect, it } from 'vitest'
import { formatAmount, formatInstant } from '@/lib/money'

describe('formatAmount', () => {
    it('groups thousands and keeps the fraction verbatim', () => {
        expect(formatAmount('1234567.89')).toBe('1,234,567.89')
    })

    it('preserves a high-scale fraction without rounding', () => {
        expect(formatAmount('100.123456789012345678')).toBe('100.123456789012345678')
    })

    it('keeps trailing zeros', () => {
        expect(formatAmount('100.00')).toBe('100.00')
    })

    it('handles negative amounts', () => {
        expect(formatAmount('-12345.6')).toBe('-12,345.6')
    })

    it('handles integers without a fraction', () => {
        expect(formatAmount('1000')).toBe('1,000')
    })
})

describe('formatInstant', () => {
    it('renders an instant as a UTC timestamp', () => {
        expect(formatInstant('2026-06-13T10:00:00Z')).toBe('2026-06-13 10:00:00 UTC')
    })

    it('drops sub-second precision', () => {
        expect(formatInstant('2026-06-13T10:00:00.123456Z')).toBe('2026-06-13 10:00:00 UTC')
    })
})
