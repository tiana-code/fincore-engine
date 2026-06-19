// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.application.event

import com.fincore.core.PaymentId

/** Published in-process once a payment is persisted as INITIATED, to drive orchestration after the transaction commits. */
data class PaymentInitiatedEvent(
    val paymentId: PaymentId,
)
