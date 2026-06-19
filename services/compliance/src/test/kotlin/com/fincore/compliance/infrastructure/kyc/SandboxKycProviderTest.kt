// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.infrastructure.kyc

import com.fincore.compliance.application.kyc.KycCheckRequest
import com.fincore.compliance.application.kyc.KycCheckResult
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class SandboxKycProviderTest {
    private val provider = SandboxKycProvider()

    @Test
    fun `should approve a plain subject reference`() {
        provider.check(KycCheckRequest("subject-1")).shouldBeInstanceOf<KycCheckResult.Approved>()
    }

    @Test
    fun `should reject when the reference carries the reject marker`() {
        provider.check(KycCheckRequest("reject-subject")).shouldBeInstanceOf<KycCheckResult.Rejected>()
    }

    @Test
    fun `should return pending when the reference carries the pending marker`() {
        provider.check(KycCheckRequest("pending-subject")).shouldBeInstanceOf<KycCheckResult.Pending>()
    }

    @Test
    fun `should return insufficient data with a non-empty list when marked`() {
        val result = provider.check(KycCheckRequest("insufficient-subject"))

        result.shouldBeInstanceOf<KycCheckResult.InsufficientData>().missing shouldBe listOf("sandbox.insufficient")
    }

    @Test
    fun `should be deterministic for the same subject reference`() {
        val first = provider.check(KycCheckRequest("subject-1"))
        val second = provider.check(KycCheckRequest("subject-1"))

        first shouldBe second
    }
}
