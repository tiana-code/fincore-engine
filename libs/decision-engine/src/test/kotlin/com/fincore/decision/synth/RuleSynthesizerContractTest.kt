// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.synth

import com.fincore.decision.domain.DecimalValue
import com.fincore.decision.domain.DecisionDslException
import com.fincore.decision.domain.DecisionRule
import com.fincore.decision.domain.EvaluationInput
import com.fincore.decision.domain.ValueKind
import com.fincore.decision.parser.RuleParser
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

private const val VALID_DSL =
    """{"condition":{"attr":"age","op":"gte","value":18},"outcome":{"label":"APPROVE"}}"""
private const val MALFORMED_DSL = """{"outcome":{"label":"APPROVE"}}"""

private class FixedSynthesizer(
    private val result: SynthesisResult,
) : RuleSynthesizer {
    override fun synthesize(request: SynthesisRequest): SynthesisResult = result
}

private class FailingSynthesizer : RuleSynthesizer {
    override fun synthesize(request: SynthesisRequest): SynthesisResult = throw RuleSynthesisException("no candidate")
}

class RuleSynthesizerContractTest {
    private val parser = RuleParser()
    private val request = SynthesisRequest(intent = "approve adults")

    @Test
    fun `should expose a single synthesize method when inspected`() {
        RuleSynthesizer::class.java.isInterface shouldBe true

        val method = RuleSynthesizer::class.java.declaredMethods.single { it.name == "synthesize" }

        method.returnType shouldBe SynthesisResult::class.java
        method.parameterTypes.toList() shouldBe listOf(SynthesisRequest::class.java)
    }

    @Test
    fun `should produce parser-valid dsl when a result is fed to the parser`() {
        val synthesizer = FixedSynthesizer(SynthesisResult(dsl = VALID_DSL, confidence = 0.9))

        val parsed = parser.parse(synthesizer.synthesize(request).dsl)

        parsed.shouldBeInstanceOf<DecisionRule>()
    }

    @Test
    fun `should let the parser reject a malformed candidate from a synthesizer`() {
        val synthesizer = FixedSynthesizer(SynthesisResult(dsl = MALFORMED_DSL))

        shouldThrow<DecisionDslException> { parser.parse(synthesizer.synthesize(request).dsl) }
    }

    @Test
    fun `should surface a synthesis exception from a failing synthesizer`() {
        shouldThrow<RuleSynthesisException> { FailingSynthesizer().synthesize(request) }
    }

    @Test
    fun `should carry available attributes and examples through the request`() {
        val grounded =
            SynthesisRequest(
                intent = "approve adults",
                availableAttributes = listOf(AttributeDescriptor("age", ValueKind.DECIMAL)),
                examples =
                    listOf(
                        SynthesisExample(
                            input = EvaluationInput(mapOf("age" to DecimalValue(20.toBigDecimal()))),
                            expectedOutcomeLabel = "APPROVE",
                        ),
                    ),
            )

        grounded.availableAttributes.single().kind shouldBe ValueKind.DECIMAL
        grounded.examples.single().expectedOutcomeLabel shouldBe "APPROVE"
    }
}
