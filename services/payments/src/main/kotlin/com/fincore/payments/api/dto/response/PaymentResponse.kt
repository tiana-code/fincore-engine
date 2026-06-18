// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.api.dto.response

import java.math.BigDecimal

data class PaymentResponse(
    val id: String,
    val reference: String,
    val amount: BigDecimal,
    val currency: String,
    val status: String,
)
