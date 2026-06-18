// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.infrastructure.bank

import com.fincore.core.Currency
import com.fincore.core.Money
import com.fincore.payments.application.bank.BankPaymentRequest
import com.fincore.payments.application.bank.BankSubmissionResult
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Duration

class SandboxBankProviderTest {
    private val provider = SandboxBankProvider()

    @Test
    fun `should accept with a deterministic provider reference when the reference has no reject marker`() {
        val result = provider.submit(request("order-1"))

        result shouldBe BankSubmissionResult.Accepted("sbx-pay_1")
    }

    @Test
    fun `should accept plainly for an ordinary amount`() {
        val result = provider.submit(request("order-1", "100.00"))

        result shouldBe BankSubmissionResult.Accepted("sbx-pay_1")
    }

    @Test
    fun `should reject when the reference carries the reject marker`() {
        val result = provider.submit(request("please-REJECT-this"))

        result shouldBe BankSubmissionResult.Rejected("sandbox rejected by reference marker")
    }

    @Test
    fun `should reject when the amount matches the configured reject amount`() {
        val result = provider.submit(request("order-1", "999.99"))

        result shouldBe BankSubmissionResult.Rejected("sandbox rejected: amount matches the decline scenario")
    }

    @Test
    fun `should reject by reference marker even when the amount matches the configured reject amount`() {
        val result = provider.submit(request("reject-this", "999.99"))

        result shouldBe BankSubmissionResult.Rejected("sandbox rejected by reference marker")
    }

    @Test
    fun `should reject by reference marker even when the amount matches the configured delay amount`() {
        val result = provider.submit(request("reject-this", "888.88"))

        result shouldBe BankSubmissionResult.Rejected("sandbox rejected by reference marker")
    }

    @Test
    fun `should accept after a bounded delay when the amount matches the configured delay amount`() {
        val fast = SandboxBankProvider(SandboxBankProperties(delay = Duration.ofMillis(80), maxDelay = Duration.ofMillis(500)))

        val elapsed = measureMillis { fast.submit(request("order-1", "888.88")).shouldBeInstanceOf<BankSubmissionResult.Accepted>() }

        elapsed shouldBeGreaterThanOrEqualTo 80L
        elapsed shouldBeLessThan 500L
    }

    @Test
    fun `should clamp the delay to the configured maximum when the configured delay exceeds it`() {
        val clamped = SandboxBankProvider(SandboxBankProperties(delay = Duration.ofMillis(800), maxDelay = Duration.ofMillis(120)))

        val elapsed = measureMillis { clamped.submit(request("order-1", "888.88")).shouldBeInstanceOf<BankSubmissionResult.Accepted>() }

        elapsed shouldBeGreaterThanOrEqualTo 120L
        elapsed shouldBeLessThan 800L
    }

    @Test
    fun `should produce the same result when the same plain-accept request is submitted twice`() {
        val request = request("order-1", "100.00")

        provider.submit(request) shouldBe provider.submit(request)
    }

    private fun measureMillis(block: () -> Unit): Long {
        val start = System.nanoTime()
        block()
        return (System.nanoTime() - start) / 1_000_000
    }

    private fun request(
        reference: String,
        amount: String = "100.00",
    ): BankPaymentRequest = BankPaymentRequest("pay_1", Money(BigDecimal(amount), Currency.USD), reference)
}
