// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.infrastructure.persistence

import com.fincore.core.AccountId
import com.fincore.core.Currency
import com.fincore.ledger.domain.Account
import org.springframework.stereotype.Component
import java.time.Instant

// Hand-written because MapStruct cannot construct the value-class domain aggregate (issue #33 deferral):
// the domain stays pure; audit columns live only here and are supplied by the caller's actor.
@Component
class AccountPersistenceAdapter {
    fun toDomain(entity: AccountEntity): Account =
        Account(
            id = AccountId(entity.id),
            name = entity.name,
            type = entity.type,
            currency = Currency.of(entity.currency),
            status = entity.status,
        )

    fun toNewEntity(
        account: Account,
        actor: String,
        now: Instant,
    ): AccountEntity =
        AccountEntity(
            id = account.id.value,
            name = account.name,
            type = account.type,
            currency = account.currency.code,
            status = account.status,
            metadata = EMPTY_JSON,
            version = 0,
            createdAt = now,
            createdBy = actor,
            updatedAt = now,
            updatedBy = actor,
        )

    companion object {
        private const val EMPTY_JSON = "{}"
    }
}
