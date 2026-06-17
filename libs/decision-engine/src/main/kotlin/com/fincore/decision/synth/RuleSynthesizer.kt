// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.synth

/**
 * Plug-in port for synthesizing a decision rule from a natural-language intent.
 *
 * Implementations are provided out of tree (for example an LLM-backed adapter) and are NOT part of
 * this open-source engine. The contract is deliberately generative and makes no determinism promise,
 * unlike [com.fincore.decision.eval.RuleEvaluator].
 */
interface RuleSynthesizer {
    /**
     * Generate a candidate decision rule (engine DSL JSON) from [request].
     *
     * The returned [SynthesisResult.dsl] is an UNTRUSTED candidate: it is not guaranteed to be valid
     * or safe DSL. Callers MUST validate it with [com.fincore.decision.parser.RuleParser] (fail-closed)
     * and treat a thrown [com.fincore.decision.domain.DecisionDslException] as rejection before
     * persisting or evaluating it. This port performs no validation and never evaluates or stores the
     * candidate.
     *
     * @throws RuleSynthesisException if no candidate could be produced.
     */
    fun synthesize(request: SynthesisRequest): SynthesisResult
}
