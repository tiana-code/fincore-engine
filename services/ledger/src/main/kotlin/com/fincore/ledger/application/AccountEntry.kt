// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import com.fincore.core.EntryId
import com.fincore.core.TransactionId
import com.fincore.ledger.domain.enum.EntryDirection
import java.math.BigDecimal
import java.time.Instant

data class AccountEntry(
    val id: EntryId,
    val transactionId: TransactionId,
    val direction: EntryDirection,
    val amount: BigDecimal,
    val currency: String,
    val postedAt: Instant,
)

data class AccountEntryPage(
    val items: List<AccountEntry>,
    val nextCursor: String?,
)
