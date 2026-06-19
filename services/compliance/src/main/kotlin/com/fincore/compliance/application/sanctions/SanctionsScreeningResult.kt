// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.sanctions

/**
 * Outcome of a [SanctionsProvider] screening: a business decision, distinct from a technical failure (a thrown
 * [SanctionsProviderException]).
 *
 * [matchedAttributes] and [missing] carry generic attribute keys, never PII or raw subject values.
 * [InsufficientData] is a first-class outcome: the screening could not be decided for want of the listed attributes.
 */
sealed interface SanctionsScreeningResult {
    data object Clear : SanctionsScreeningResult

    /**
     * A potential hit. [score] is a generic provider-reported match confidence in [0, 1] (higher means a stronger
     * match); it is not an m-of-n ratio and not a business risk band.
     */
    data class PotentialMatch(
        val matchedAttributes: List<String>,
        val score: Double,
    ) : SanctionsScreeningResult {
        init {
            require(matchedAttributes.isNotEmpty()) { "matchedAttributes must not be empty" }
            require(score in MIN_SCORE..MAX_SCORE) { "score must be within [$MIN_SCORE, $MAX_SCORE]" }
        }

        private companion object {
            const val MIN_SCORE = 0.0
            const val MAX_SCORE = 1.0
        }
    }

    data class InsufficientData(
        val missing: List<String>,
    ) : SanctionsScreeningResult {
        init {
            require(missing.isNotEmpty()) { "missing must not be empty" }
        }
    }
}
