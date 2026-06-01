// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.test.builders

import com.fincore.core.AccountId
import com.fincore.core.Money
import com.fincore.core.TransactionId
import com.fincore.test.builders.MoneyBuilder.Companion.anEur
import java.time.Instant

data class EntrySnapshot(
    val accountId: AccountId,
    val direction: String, // CREDIT / DEBIT
    val amount: Money,
)

data class TransactionSnapshot(
    val id: TransactionId,
    val entries: List<EntrySnapshot>,
    val description: String,
    val postedAt: Instant,
    val status: String,
)

class TransactionBuilder {
    private var id: TransactionId = TransactionId.generate()
    private var entries: List<EntrySnapshot> = emptyList()
    private var description: String = "test transaction"
    private var postedAt: Instant = Instant.parse("2026-01-01T00:00:00Z")
    private var status: String = "POSTED"

    fun id(v: TransactionId): TransactionBuilder = apply { id = v }

    fun entries(v: List<EntrySnapshot>): TransactionBuilder = apply { entries = v }

    fun simpleTransfer(
        from: AccountId,
        to: AccountId,
        amount: Money,
    ): TransactionBuilder =
        apply {
            entries =
                listOf(
                    EntrySnapshot(from, "DEBIT", -amount),
                    EntrySnapshot(to, "CREDIT", amount),
                )
        }

    fun description(v: String): TransactionBuilder = apply { description = v }

    fun postedAt(v: Instant): TransactionBuilder = apply { postedAt = v }

    fun status(v: String): TransactionBuilder = apply { status = v }

    fun build(): TransactionSnapshot = TransactionSnapshot(id, entries, description, postedAt, status)

    companion object {
        fun aTransaction(): TransactionBuilder = TransactionBuilder()

        fun aTransferOf100Eur(
            from: AccountId,
            to: AccountId,
        ): TransactionSnapshot = aTransaction().simpleTransfer(from, to, anEur("100.00")).build()
    }
}
