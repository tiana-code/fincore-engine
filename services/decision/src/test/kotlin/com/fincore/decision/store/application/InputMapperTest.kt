// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.application

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fincore.decision.domain.BoolValue
import com.fincore.decision.domain.DecimalValue
import com.fincore.decision.domain.StringValue
import com.fincore.decision.store.config.DecisionApiProperties
import com.fincore.decision.store.exception.InputNotMappableException
import com.fincore.decision.store.exception.InputTooLargeException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class InputMapperTest {
    private val json = ObjectMapper()

    private fun node(raw: String): JsonNode = json.readTree(raw)

    @Test
    fun `should map string number and boolean to typed attribute values`() {
        val input =
            InputMapper(DecisionApiProperties()).toEvaluationInput(
                mapOf("s" to node(""""x""""), "n" to node("1.5"), "b" to node("true")),
            )

        input.get("s").shouldBeInstanceOf<StringValue>().value shouldBe "x"
        input.get("n").shouldBeInstanceOf<DecimalValue>()
        input.get("b").shouldBeInstanceOf<BoolValue>().value shouldBe true
    }

    @Test
    fun `should reject a nested object value`() {
        shouldThrow<InputNotMappableException> {
            InputMapper(DecisionApiProperties()).toEvaluationInput(mapOf("o" to node("""{"x":1}""")))
        }
    }

    @Test
    fun `should reject more attributes than the cap`() {
        shouldThrow<InputTooLargeException> {
            InputMapper(DecisionApiProperties(maxInputAttributes = 1)).toEvaluationInput(mapOf("a" to node("1"), "b" to node("2")))
        }
    }

    @Test
    fun `should reject a value over the length cap`() {
        shouldThrow<InputTooLargeException> {
            InputMapper(DecisionApiProperties(maxInputValueChars = 2)).toEvaluationInput(mapOf("s" to node(""""abc"""")))
        }
    }
}
