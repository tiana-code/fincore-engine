// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.sdk.model

import java.math.BigDecimal

data class TransactionDetail(
    val id: String,
    val reference: String,
    val description: String?,
    val status: String,
    val reversesId: String?,
    val postedAt: String,
    val entries: List<Entry>,
)

data class Entry(
    val accountId: String,
    val direction: String,
    val amount: BigDecimal,
    val currency: String,
)
