// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.domain

import com.fincore.core.AccountId
import com.fincore.core.EntryId
import com.fincore.core.Money
import com.fincore.ledger.domain.enum.EntryDirection
import com.fincore.ledger.domain.exception.DomainException
import java.math.BigDecimal

class Entry(
    val id: EntryId,
    val accountId: AccountId,
    val direction: EntryDirection,
    val amount: Money,
) {
    init {
        if (amount.amount.compareTo(BigDecimal.ZERO) == 0) {
            throw DomainException("Entry amount must not be zero for account $accountId direction $direction")
        }
    }
}
