// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.domain

import com.fincore.core.AccountId
import com.fincore.core.Currency
import com.fincore.ledger.domain.enum.AccountStatus
import com.fincore.ledger.domain.enum.AccountType
import com.fincore.ledger.domain.exception.DomainException

class Account(
    val id: AccountId,
    name: String,
    val type: AccountType,
    val currency: Currency,
    status: AccountStatus = AccountStatus.ACTIVE,
) {
    companion object {
        private const val MIN_NAME_LENGTH = 1
        private const val MAX_NAME_LENGTH = 255
    }

    var name: String = validated(name)
        private set

    var status: AccountStatus = status
        private set

    fun transitionStatus(target: AccountStatus) {
        if (!status.canTransitionTo(target)) {
            throw DomainException(
                "Illegal account status transition: $status -> $target for account $id",
            )
        }
        status = target
    }

    fun rename(newName: String) {
        if (status == AccountStatus.CLOSED) {
            throw DomainException("Cannot rename a CLOSED account $id")
        }
        name = validated(newName)
    }

    fun isPostable(): Boolean = status == AccountStatus.ACTIVE

    private fun validated(value: String): String {
        if (value.length !in MIN_NAME_LENGTH..MAX_NAME_LENGTH) {
            throw DomainException(
                "Account name must be $MIN_NAME_LENGTH..$MAX_NAME_LENGTH characters, got ${value.length}",
            )
        }
        return value
    }
}
