// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.application

import com.fincore.payments.domain.Payment

data class PaymentEventData(
    val paymentId: String,
    val reference: String,
    val amount: String,
    val currency: String,
    val status: String,
    val detail: String? = null,
) {
    companion object {
        fun from(
            payment: Payment,
            detail: String? = null,
        ): PaymentEventData =
            PaymentEventData(
                paymentId = payment.id.toString(),
                reference = payment.reference,
                amount = payment.amount.amount.toPlainString(),
                currency = payment.amount.currency.code,
                status = payment.status.name,
                detail = detail,
            )
    }
}
