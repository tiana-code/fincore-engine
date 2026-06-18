// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.infrastructure.bank

import com.fincore.payments.application.bank.BankPaymentRequest
import com.fincore.payments.application.bank.BankProvider
import com.fincore.payments.application.bank.BankSubmissionResult
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    prefix = "fincore.payments.bank.sandbox",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = false,
)
class SandboxBankProvider : BankProvider {
    override fun submit(request: BankPaymentRequest): BankSubmissionResult =
        if (request.reference.contains(REJECT_MARKER, ignoreCase = true)) {
            BankSubmissionResult.Rejected("sandbox rejected by reference marker")
        } else {
            BankSubmissionResult.Accepted("sbx-${request.paymentId}")
        }

    private companion object {
        const val REJECT_MARKER = "reject"
    }
}
