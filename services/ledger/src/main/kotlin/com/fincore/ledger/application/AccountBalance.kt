// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import com.fincore.core.AccountId
import com.fincore.core.Money
import java.time.Instant

data class AccountBalance(
    val accountId: AccountId,
    val amount: Money,
    val lastPostedAt: Instant?,
)
