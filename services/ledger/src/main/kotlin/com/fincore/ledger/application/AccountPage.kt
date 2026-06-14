// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import com.fincore.ledger.domain.Account

data class AccountPage(
    val items: List<Account>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
