// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import com.fincore.core.Currency
import com.fincore.ledger.domain.enum.AccountType

data class CreateAccountCommand(
    val name: String,
    val type: AccountType,
    val currency: Currency,
    val actor: String,
)
