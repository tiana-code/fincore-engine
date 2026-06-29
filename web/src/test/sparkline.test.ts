// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

import { describe, expect, it } from 'vitest'
import { buildSparkline } from '@/components/Sparkline'

describe('buildSparkline', () => {
    it('builds a line path that starts with a move and a closed fill path', () => {
        const { line, fill } = buildSparkline([1, 2, 3], 80, 28)
        expect(line.startsWith('M')).toBe(true)
        expect(fill.endsWith('Z')).toBe(true)
    })
})
