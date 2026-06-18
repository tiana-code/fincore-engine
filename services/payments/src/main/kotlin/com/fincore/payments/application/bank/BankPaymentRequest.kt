// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.application.bank

import com.fincore.core.Money

data class BankPaymentRequest(
    val paymentId: String,
    val amount: Money,
    val reference: String,
) {
    init {
        require(paymentId.isNotBlank()) { "paymentId must not be blank" }
        require(reference.isNotBlank()) { "reference must not be blank" }
    }
}
