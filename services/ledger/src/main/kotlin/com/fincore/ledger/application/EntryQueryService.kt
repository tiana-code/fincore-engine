// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import com.fincore.core.AccountId
import java.time.Instant

interface EntryQueryService {
    fun listAccountEntries(
        accountId: AccountId,
        from: Instant?,
        to: Instant?,
        cursor: String?,
        limit: Int,
    ): AccountEntryPage
}
