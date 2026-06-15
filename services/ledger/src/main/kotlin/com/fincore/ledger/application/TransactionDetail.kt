// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import com.fincore.core.AccountId
import com.fincore.core.TransactionId
import com.fincore.ledger.domain.enum.EntryDirection
import com.fincore.ledger.domain.enum.TransactionStatus
import java.math.BigDecimal
import java.time.Instant

data class EntryView(
    val accountId: AccountId,
    val direction: EntryDirection,
    val amount: BigDecimal,
    val currency: String,
)

data class TransactionDetail(
    val id: TransactionId,
    val reference: String,
    val description: String?,
    val status: TransactionStatus,
    val reversesId: TransactionId?,
    val postedAt: Instant,
    val entries: List<EntryView>,
)
