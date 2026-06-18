// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.application

import com.fincore.core.PaymentId
import com.fincore.payments.domain.Payment

interface PaymentService {
    fun initiate(command: InitiatePaymentCommand): Payment

    fun get(id: PaymentId): Payment

    fun cancel(id: PaymentId): Payment

    fun screen(id: PaymentId): Payment

    fun markSubmitted(
        id: PaymentId,
        providerReference: String,
    ): Payment

    fun markFailed(
        id: PaymentId,
        reason: String,
    ): Payment

    fun markSettled(id: PaymentId): Payment
}
