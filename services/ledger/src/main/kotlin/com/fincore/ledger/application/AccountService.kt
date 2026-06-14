// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import com.fincore.core.AccountId
import com.fincore.ledger.domain.Account
import com.fincore.ledger.domain.enum.AccountStatus

interface AccountService {
    fun create(command: CreateAccountCommand): Account

    fun get(id: AccountId): Account

    fun list(
        page: Int,
        size: Int,
    ): AccountPage

    fun rename(
        id: AccountId,
        newName: String,
        actor: String,
    ): Account

    fun changeStatus(
        id: AccountId,
        target: AccountStatus,
        actor: String,
    ): Account
}
