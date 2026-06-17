// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.synth

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SynthesisTypesTest {
    @Test
    fun `should reject a blank intent when building a request`() {
        shouldThrow<IllegalArgumentException> { SynthesisRequest(intent = "  ") }
    }

    @Test
    fun `should default grounding to empty lists when building a request`() {
        val request = SynthesisRequest(intent = "approve adults")

        request.availableAttributes shouldBe emptyList()
        request.examples shouldBe emptyList()
    }

    @Test
    fun `should reject a blank dsl when building a result`() {
        shouldThrow<IllegalArgumentException> { SynthesisResult(dsl = "   ") }
    }

    @Test
    fun `should reject confidence above one when building a result`() {
        shouldThrow<IllegalArgumentException> { SynthesisResult(dsl = "stub", confidence = 1.5) }
    }

    @Test
    fun `should reject confidence below zero when building a result`() {
        shouldThrow<IllegalArgumentException> { SynthesisResult(dsl = "stub", confidence = -0.1) }
    }

    @Test
    fun `should reject a NaN confidence when building a result`() {
        shouldThrow<IllegalArgumentException> { SynthesisResult(dsl = "stub", confidence = Double.NaN) }
    }

    @Test
    fun `should accept a null or in-range confidence when building a result`() {
        SynthesisResult(dsl = "stub").confidence shouldBe null
        SynthesisResult(dsl = "stub", confidence = 0.0).confidence shouldBe 0.0
        SynthesisResult(dsl = "stub", confidence = 1.0).confidence shouldBe 1.0
    }
}
