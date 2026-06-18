// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.infrastructure.bank

import com.fincore.payments.application.bank.BankPaymentRequest
import com.fincore.payments.application.bank.BankProvider
import com.fincore.payments.application.bank.BankSubmissionResult
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.Duration

@Component
@ConditionalOnProperty(
    prefix = "fincore.payments.bank.sandbox",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = false,
)
class SandboxBankProvider(
    private val properties: SandboxBankProperties = SandboxBankProperties(),
) : BankProvider {
    override fun submit(request: BankPaymentRequest): BankSubmissionResult {
        if (request.reference.contains(REJECT_MARKER, ignoreCase = true)) {
            return BankSubmissionResult.Rejected("sandbox rejected by reference marker")
        }
        val amount = request.amount.amount
        if (amount.compareTo(properties.rejectAmount) == 0) {
            return BankSubmissionResult.Rejected("sandbox rejected: amount matches the decline scenario")
        }
        if (amount.compareTo(properties.delayAmount) == 0) {
            sleepBounded(properties.delay)
        }
        return BankSubmissionResult.Accepted("sbx-${request.paymentId}")
    }

    private fun sleepBounded(delay: Duration) {
        val millis = delay.coerceAtMost(properties.maxDelay).coerceAtLeast(Duration.ZERO).toMillis()
        if (millis <= 0) {
            return
        }
        try {
            Thread.sleep(millis)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    private companion object {
        const val REJECT_MARKER = "reject"
    }
}
