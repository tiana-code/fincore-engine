// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.domain

import com.fincore.core.Money
import com.fincore.core.PaymentId
import com.fincore.payments.domain.enum.PaymentStatus
import com.fincore.payments.domain.exception.PaymentDomainException

class Payment(
    val id: PaymentId,
    val amount: Money,
    reference: String,
    status: PaymentStatus = PaymentStatus.INITIATED,
) {
    val reference: String = validatedReference(reference)

    var status: PaymentStatus = status
        private set

    fun transitionTo(target: PaymentStatus) {
        if (!status.canTransitionTo(target)) {
            throw PaymentDomainException("Illegal payment status transition: $status -> $target for payment $id")
        }
        status = target
    }

    fun isTerminal(): Boolean = status.isTerminal()

    private fun validatedReference(value: String): String {
        if (value.isBlank() || value.length > MAX_REFERENCE_LENGTH) {
            throw PaymentDomainException(
                "Payment reference must be non-blank and at most $MAX_REFERENCE_LENGTH characters",
            )
        }
        return value
    }

    private companion object {
        const val MAX_REFERENCE_LENGTH = 140
    }
}
