// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.infrastructure.sanctions

import com.fincore.compliance.application.sanctions.SanctionsProvider
import com.fincore.compliance.application.sanctions.SanctionsScreeningRequest
import com.fincore.compliance.application.sanctions.SanctionsScreeningResult
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

// In-tree sandbox provider, off by default; a deterministic m-of-n match driven by markers in the request attributes.
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
