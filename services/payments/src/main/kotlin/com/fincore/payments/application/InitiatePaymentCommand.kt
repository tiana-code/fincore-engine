// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.application

import com.fincore.core.Money

data class InitiatePaymentCommand(
    val idempotencyKey: String,
    val amount: Money,
    val reference: String,
)
