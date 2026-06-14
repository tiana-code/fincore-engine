// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.api.dto.response

import com.fincore.ledger.domain.enum.AccountStatus
import com.fincore.ledger.domain.enum.AccountType

data class AccountResponse(
    val id: String,
    val name: String,
    val type: AccountType,
    val currency: String,
    val status: AccountStatus,
)
