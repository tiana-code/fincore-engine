// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import com.fincore.core.AccountId
import com.fincore.core.Currency
import java.time.Instant

interface BalanceService {
    fun current(
        accountId: AccountId,
        currency: Currency,
    ): AccountBalance

    fun asOf(
        accountId: AccountId,
        currency: Currency,
        instant: Instant,
    ): AccountBalance
}
