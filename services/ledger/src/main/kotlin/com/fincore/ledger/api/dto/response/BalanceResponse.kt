// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.api.dto.response

import java.math.BigDecimal
import java.time.Instant

data class BalanceResponse(
    val accountId: String,
    val currency: String,
    val amount: BigDecimal,
    val lastPostedAt: Instant?,
)
