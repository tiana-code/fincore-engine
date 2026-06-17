// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.synth

private const val MIN_CONFIDENCE = 0.0
private const val MAX_CONFIDENCE = 1.0

/**
 * A synthesized rule candidate.
 *
 * @property dsl the generated rule as engine DSL JSON; an UNTRUSTED candidate the caller must validate
 *   with [com.fincore.decision.parser.RuleParser] before use. Must not be blank.
 * @property confidence optional self-reported quality in 0.0..1.0; omitted when the implementation cannot
 *   estimate it.
 * @property rationale optional human-readable explanation of the generated rule.
 */
data class SynthesisResult(
    val dsl: String,
    val confidence: Double? = null,
    val rationale: String? = null,
) {
    init {
        require(dsl.isNotBlank()) { "dsl must not be blank" }
        confidence?.let {
            require(!it.isNaN() && it in MIN_CONFIDENCE..MAX_CONFIDENCE) { "confidence must be in 0.0..1.0" }
        }
    }
}
