// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.infrastructure.bank

import com.fincore.core.Currency
import com.fincore.core.Money
import com.fincore.payments.application.bank.BankPaymentRequest
import com.fincore.payments.application.bank.BankSubmissionResult
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class SandboxBankProviderTest {
    private val provider = SandboxBankProvider()

    @Test
    fun `should accept with a deterministic provider reference when the reference has no reject marker`() {
        val result = provider.submit(request("order-1"))

        result shouldBe BankSubmissionResult.Accepted("sbx-pay_1")
    }

    @Test
    fun `should reject when the reference carries the reject marker`() {
        val result = provider.submit(request("please-REJECT-this"))

        result shouldBe BankSubmissionResult.Rejected("sandbox rejected by reference marker")
    }

    @Test
    fun `should produce the same result when the same request is submitted twice`() {
        val request = request("order-1")

        provider.submit(request) shouldBe provider.submit(request)
    }

    private fun request(reference: String): BankPaymentRequest =
        BankPaymentRequest("pay_1", Money(BigDecimal("100.00"), Currency.USD), reference)
}
