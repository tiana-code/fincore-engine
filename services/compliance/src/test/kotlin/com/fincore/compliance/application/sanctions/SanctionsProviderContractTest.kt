// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.sanctions

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

private class FixedSanctionsProvider(
    private val result: SanctionsScreeningResult,
) : SanctionsProvider {
    override fun screen(request: SanctionsScreeningRequest): SanctionsScreeningResult = result
}

class SanctionsProviderContractTest {
    private val request = SanctionsScreeningRequest("subject-1", setOf("attr-a", "attr-b"), 1)

    @Test
    fun `should expose a single screen method when inspected`() {
        SanctionsProvider::class.java.isInterface shouldBe true

        val method =
            SanctionsProvider::class.java.declaredMethods
                .filter { !it.isBridge && !it.isSynthetic }
                .single { it.name == "screen" }

        method.returnType shouldBe SanctionsScreeningResult::class.java
        method.parameterTypes.toList() shouldBe listOf(SanctionsScreeningRequest::class.java)
    }

    @Test
    fun `should return clear when the implementation finds no hit`() {
        FixedSanctionsProvider(SanctionsScreeningResult.Clear)
            .screen(request)
            .shouldBeInstanceOf<SanctionsScreeningResult.Clear>()
    }

    @Test
    fun `should carry matched attributes and score on a potential match`() {
        val result = FixedSanctionsProvider(SanctionsScreeningResult.PotentialMatch(listOf("attr-a"), 0.5)).screen(request)

        val match = result.shouldBeInstanceOf<SanctionsScreeningResult.PotentialMatch>()
        match.matchedAttributes shouldBe listOf("attr-a")
        match.score shouldBe 0.5
    }

    @Test
    fun `should carry the missing attributes when data is insufficient`() {
        val result = FixedSanctionsProvider(SanctionsScreeningResult.InsufficientData(listOf("attr-b"))).screen(request)

        result.shouldBeInstanceOf<SanctionsScreeningResult.InsufficientData>().missing shouldBe listOf("attr-b")
    }

    @Test
    fun `should reject a blank subject reference`() {
        shouldThrow<IllegalArgumentException> { SanctionsScreeningRequest(" ", setOf("attr-a"), 1) }
    }

    @Test
    fun `should reject empty attributes`() {
        shouldThrow<IllegalArgumentException> { SanctionsScreeningRequest("subject-1", emptySet(), 1) }
    }

    @Test
    fun `should reject required matches below one`() {
        shouldThrow<IllegalArgumentException> { SanctionsScreeningRequest("subject-1", setOf("attr-a"), 0) }
    }

    @Test
    fun `should reject required matches above the attribute count`() {
        shouldThrow<IllegalArgumentException> { SanctionsScreeningRequest("subject-1", setOf("attr-a"), 2) }
    }

    @Test
    fun `should reject a potential match with no matched attributes`() {
        shouldThrow<IllegalArgumentException> { SanctionsScreeningResult.PotentialMatch(emptyList(), 0.5) }
    }

    @Test
    fun `should reject a potential match score out of range`() {
        shouldThrow<IllegalArgumentException> { SanctionsScreeningResult.PotentialMatch(listOf("attr-a"), 1.5) }
    }

    @Test
    fun `should reject insufficient data with no missing attributes`() {
        shouldThrow<IllegalArgumentException> { SanctionsScreeningResult.InsufficientData(emptyList()) }
    }

    @Test
    fun `should reject a not-a-number score`() {
        shouldThrow<IllegalArgumentException> { SanctionsScreeningResult.PotentialMatch(listOf("attr-a"), Double.NaN) }
    }

    @Test
    fun `should propagate a provider exception on technical failure`() {
        val provider =
            object : SanctionsProvider {
                override fun screen(request: SanctionsScreeningRequest): SanctionsScreeningResult =
                    throw SanctionsProviderException("upstream unavailable")
            }

        shouldThrow<SanctionsProviderException> { provider.screen(request) }
    }
}
