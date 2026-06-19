// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.infrastructure.sanctions

import com.fincore.compliance.application.sanctions.SanctionsProvider
import com.fincore.compliance.application.sanctions.SanctionsScreeningRequest
import com.fincore.compliance.application.sanctions.SanctionsScreeningResult
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/**
 * Deterministic in-tree sandbox sanctions provider, off by default. Encodes no real sanctions list; the outcome is a
 * pure function of documented case-insensitive markers. An "insufficient" marker in the opaque subject reference yields
 * InsufficientData; otherwise the provided attribute keys carrying the "match" marker are the matched dimensions, and a
 * potential hit is reported when at least the requested number of them match. The score is a fixed sandbox confidence,
 * not the m-of-n ratio.
 */
@Component
@ConditionalOnProperty(
    prefix = "fincore.compliance.sanctions.sandbox",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = false,
)
class SandboxSanctionsProvider : SanctionsProvider {
    override fun screen(request: SanctionsScreeningRequest): SanctionsScreeningResult {
        if (request.subjectReference.contains(INSUFFICIENT_MARKER, ignoreCase = true)) {
            return SanctionsScreeningResult.InsufficientData(listOf("sandbox.insufficient"))
        }
        val matched = request.attributes.filter { it.contains(MATCH_MARKER, ignoreCase = true) }
        return if (matched.size >= request.requiredMatches) {
            SanctionsScreeningResult.PotentialMatch(matched, MATCH_SCORE)
        } else {
            SanctionsScreeningResult.Clear
        }
    }

    private companion object {
        const val MATCH_MARKER = "match"
        const val INSUFFICIENT_MARKER = "insufficient"
        const val MATCH_SCORE = 0.75
    }
}
