// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.fincore.decision.domain.DecimalValue
import com.fincore.decision.domain.EvaluationInput
import com.fincore.decision.domain.StringValue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class InputHasherTest {
    private val hasher = InputHasher(ObjectMapper())

    @Test
    fun `should produce a 64 char lowercase hex digest`() {
        val hash = hasher.hash(EvaluationInput(mapOf("a" to StringValue("x"))))

        hash shouldMatch Regex("^[0-9a-f]{64}$")
    }

    @Test
    fun `should hash the same logical input identically regardless of key order`() {
        val first = EvaluationInput(mapOf("amount" to DecimalValue(BigDecimal("100")), "country" to StringValue("A")))
        val second = EvaluationInput(mapOf("country" to StringValue("A"), "amount" to DecimalValue(BigDecimal("100"))))

        hasher.hash(first) shouldBe hasher.hash(second)
    }

    @Test
    fun `should hash equal decimals with different scale identically`() {
        val first = EvaluationInput(mapOf("amount" to DecimalValue(BigDecimal("2.0"))))
        val second = EvaluationInput(mapOf("amount" to DecimalValue(BigDecimal("2.00"))))

        hasher.hash(first) shouldBe hasher.hash(second)
    }

    @Test
    fun `should hash different inputs differently`() {
        val first = EvaluationInput(mapOf("amount" to DecimalValue(BigDecimal("100"))))
        val second = EvaluationInput(mapOf("amount" to DecimalValue(BigDecimal("200"))))

        (hasher.hash(first) == hasher.hash(second)) shouldBe false
    }
}
