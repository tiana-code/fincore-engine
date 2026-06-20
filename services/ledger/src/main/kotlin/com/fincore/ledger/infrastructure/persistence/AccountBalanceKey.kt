// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable
import java.util.UUID

@Embeddable
data class AccountBalanceKey(
    @Column(name = "account_id", nullable = false, updatable = false)
    var accountId: UUID,
    @Column(name = "currency", nullable = false, updatable = false)
    var currency: String,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
