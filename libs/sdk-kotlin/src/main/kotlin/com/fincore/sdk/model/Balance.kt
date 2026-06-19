// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.sdk.model

import java.math.BigDecimal

data class Balance(
    val accountId: String,
    val currency: String,
    val amount: BigDecimal,
    val lastPostedAt: String?,
)
