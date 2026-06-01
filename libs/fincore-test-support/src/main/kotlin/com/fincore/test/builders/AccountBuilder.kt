// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.test.builders

import com.fincore.core.AccountId
import com.fincore.core.Currency
import java.time.Instant

data class AccountSnapshot(
    val id: AccountId,
    val type: String,
    val currency: Currency,
    val status: String,
    val ownerId: String,
    val createdAt: Instant,
)

class AccountBuilder {
    private var id: AccountId = AccountId.generate()
    private var type: String = "USER_WALLET"
    private var currency: Currency = Currency.EUR
    private var status: String = "ACTIVE"
    private var ownerId: String = "owner-${System.nanoTime()}"
    private var createdAt: Instant = Instant.parse("2026-01-01T00:00:00Z")

    fun id(v: AccountId): AccountBuilder = apply { id = v }

    fun type(v: String): AccountBuilder = apply { type = v }

    fun currency(v: Currency): AccountBuilder = apply { currency = v }

    fun status(v: String): AccountBuilder = apply { status = v }

    fun ownerId(v: String): AccountBuilder = apply { ownerId = v }

    fun createdAt(v: Instant): AccountBuilder = apply { createdAt = v }

    fun build(): AccountSnapshot = AccountSnapshot(id, type, currency, status, ownerId, createdAt)

    companion object {
        fun anAccount(): AccountBuilder = AccountBuilder()
    }
}
