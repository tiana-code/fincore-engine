// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.kyc

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

private class FixedKycProvider(
    private val result: KycCheckResult,
) : KycProvider {
    override fun check(request: KycCheckRequest): KycCheckResult = result
}

class KycProviderContractTest {
    private val request = KycCheckRequest("subject-1")

    @Test
    fun `should expose a single check method when inspected`() {
        KycProvider::class.java.isInterface shouldBe true

        val method =
            KycProvider::class.java.declaredMethods
                .filter { !it.isBridge && !it.isSynthetic }
                .single { it.name == "check" }

        method.returnType shouldBe KycCheckResult::class.java
        method.parameterTypes.toList() shouldBe listOf(KycCheckRequest::class.java)
    }

    @Test
    fun `should return an approved result when the implementation approves`() {
        FixedKycProvider(KycCheckResult.Approved("ref-1"))
            .check(request)
            .shouldBeInstanceOf<KycCheckResult.Approved>()
    }

    @Test
    fun `should return a rejected result when the implementation rejects`() {
        FixedKycProvider(KycCheckResult.Rejected("declined"))
            .check(request)
            .shouldBeInstanceOf<KycCheckResult.Rejected>()
    }

    @Test
    fun `should return a pending result when the implementation defers`() {
        FixedKycProvider(KycCheckResult.Pending("ref-2"))
            .check(request)
            .shouldBeInstanceOf<KycCheckResult.Pending>()
    }

    @Test
    fun `should carry the missing attributes when data is insufficient`() {
        val result = FixedKycProvider(KycCheckResult.InsufficientData(listOf("attr-a"))).check(request)

        result.shouldBeInstanceOf<KycCheckResult.InsufficientData>().missing shouldBe listOf("attr-a")
    }

    @Test
    fun `should reject an empty missing list`() {
        shouldThrow<IllegalArgumentException> { KycCheckResult.InsufficientData(emptyList()) }
    }

    @Test
    fun `should reject a blank subject reference`() {
        shouldThrow<IllegalArgumentException> { KycCheckRequest(" ") }
    }

    @Test
    fun `should reject a subject reference over the length limit`() {
        shouldThrow<IllegalArgumentException> { KycCheckRequest("x".repeat(141)) }
    }
}
