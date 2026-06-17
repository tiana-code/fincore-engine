// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.synth

import com.fincore.decision.domain.EvaluationInput
import com.fincore.decision.domain.ValueKind

/**
 * A natural-language request to synthesize a decision rule.
 *
 * @property intent free-text description of the rule to generate; must not be blank.
 * @property availableAttributes optional vocabulary the rule may reference, grounding generation.
 * @property examples optional input/outcome pairs that illustrate the desired behaviour.
 */
data class SynthesisRequest(
    val intent: String,
    val availableAttributes: List<AttributeDescriptor> = emptyList(),
    val examples: List<SynthesisExample> = emptyList(),
) {
    init {
        require(intent.isNotBlank()) { "intent must not be blank" }
    }
}

/** An attribute the synthesizer may reference, named and typed in the engine's [ValueKind] vocabulary. */
data class AttributeDescriptor(
    val name: String,
    val kind: ValueKind,
)

/** An illustrative input and its expected outcome label (null when the input should not match any rule). */
data class SynthesisExample(
    val input: EvaluationInput,
    val expectedOutcomeLabel: String? = null,
)
