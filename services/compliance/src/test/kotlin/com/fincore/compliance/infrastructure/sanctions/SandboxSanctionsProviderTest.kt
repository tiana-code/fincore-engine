// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.infrastructure.sanctions

import com.fincore.compliance.application.sanctions.SanctionsScreeningRequest
import com.fincore.compliance.application.sanctions.SanctionsScreeningResult
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class SandboxSanctionsProviderTest {
    private val provider = SandboxSanctionsProvider()

    @Test
    fun `should clear when no attribute carries the match marker`() {
        val request = SanctionsScreeningRequest("subject-1", setOf("attr-a", "attr-b"), 1)

        provider.screen(request) shouldBe SanctionsScreeningResult.Clear
    }

    @Test
    fun `should report a potential match when enough attributes match`() {
        val request = SanctionsScreeningRequest("subject-1", setOf("match-a", "match-b"), 2)

        val result = provider.screen(request).shouldBeInstanceOf<SanctionsScreeningResult.PotentialMatch>()
        result.matchedAttributes.toSet() shouldBe setOf("match-a", "match-b")
    }

    @Test
    fun `should clear when matched attributes are below the required count`() {
        val request = SanctionsScreeningRequest("subject-1", setOf("match-a", "attr-b"), 2)

        provider.screen(request) shouldBe SanctionsScreeningResult.Clear
    }

    @Test
    fun `should return insufficient data when the subject reference is marked`() {
        val request = SanctionsScreeningRequest("insufficient-subject", setOf("attr-a"), 1)

        provider.screen(request).shouldBeInstanceOf<SanctionsScreeningResult.InsufficientData>()
    }

    @Test
    fun `should be deterministic for the same request`() {
        val request = SanctionsScreeningRequest("subject-1", setOf("match-a", "match-b"), 1)

        provider.screen(request) shouldBe provider.screen(request)
    }
}
