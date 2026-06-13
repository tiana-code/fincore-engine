// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import com.fincore.core.AccountId
import com.fincore.core.Currency
import com.fincore.core.Money
import com.fincore.ledger.infrastructure.persistence.AccountBalanceKey
import com.fincore.ledger.infrastructure.persistence.AccountBalanceRepository
import com.fincore.ledger.infrastructure.persistence.EntryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class BalanceServiceImpl(
    private val balanceRepository: AccountBalanceRepository,
    private val entryRepository: EntryRepository,
) : BalanceService {
    @Transactional(readOnly = true)
    override fun current(
        accountId: AccountId,
        currency: Currency,
    ): AccountBalance {
        val row = balanceRepository.findById(AccountBalanceKey(accountId.value, currency.code)).orElse(null)
        return if (row == null) {
            AccountBalance(accountId, Money.zero(currency), null)
        } else {
            AccountBalance(accountId, Money.of(row.balance, currency), row.lastPostedAt)
        }
    }

    @Transactional(readOnly = true)
    override fun asOf(
        accountId: AccountId,
        currency: Currency,
        instant: Instant,
    ): AccountBalance {
        val sum = entryRepository.sumAmount(accountId.value, currency.code, instant)
        return AccountBalance(accountId, Money.of(sum, currency), null)
    }
}
