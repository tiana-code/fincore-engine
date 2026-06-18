// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.application.bank

import com.fincore.core.Currency
import com.fincore.core.Money
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import java.math.BigDecimal

private class FixedBankProvider(
    private val result: BankSubmissionResult,
) : BankProvider {
    override fun submit(request: BankPaymentRequest): BankSubmissionResult = result
}

class BankProviderContractTest {
    private val request = BankPaymentRequest("pay_1", Money(BigDecimal("100.00"), Currency.USD), "order-1")

    @Test
    fun `should expose a single submit method when inspected`() {
        BankProvider::class.java.isInterface shouldBe true

        val method =
            BankProvider::class.java.declaredMethods
                .filter { !it.isBridge && !it.isSynthetic }
                .single { it.name == "submit" }

        method.returnType shouldBe BankSubmissionResult::class.java
        method.parameterTypes.toList() shouldBe listOf(BankPaymentRequest::class.java)
    }

    @Test
    fun `should return an accepted result when the implementation accepts`() {
        val provider = FixedBankProvider(BankSubmissionResult.Accepted("ref-1"))

        provider.submit(request).shouldBeInstanceOf<BankSubmissionResult.Accepted>()
    }

    @Test
    fun `should return a rejected result when the implementation rejects`() {
        val provider = FixedBankProvider(BankSubmissionResult.Rejected("declined"))

        provider.submit(request).shouldBeInstanceOf<BankSubmissionResult.Rejected>()
    }

    @Test
    fun `should reject a blank payment id or reference`() {
        shouldThrow<IllegalArgumentException> { BankPaymentRequest(" ", Money(BigDecimal.ONE, Currency.USD), "order-1") }
        shouldThrow<IllegalArgumentException> { BankPaymentRequest("pay_1", Money(BigDecimal.ONE, Currency.USD), " ") }
    }
}
