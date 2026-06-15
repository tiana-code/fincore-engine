// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import com.fincore.core.AccountId
import com.fincore.core.Currency
import com.fincore.ledger.domain.enum.EntryDirection
import java.math.BigDecimal

data class EntryLine(
    val accountId: AccountId,
    val direction: EntryDirection,
    val amount: BigDecimal,
)

data class PostTransactionCommand(
    val reference: String,
    val description: String?,
    val currency: Currency,
    val entries: List<EntryLine>,
    val actor: String,
    val correlationId: String?,
    val requestHash: String? = null,
)
