// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.sdk.model

import java.math.BigDecimal

data class Payment(
    val id: String,
    val reference: String,
    val amount: BigDecimal,
    val currency: String,
    val status: String,
)
