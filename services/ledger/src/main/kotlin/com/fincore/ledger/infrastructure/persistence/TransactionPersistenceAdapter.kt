// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.infrastructure.persistence

import com.fincore.ledger.domain.Transaction
import com.fincore.ledger.domain.enum.TransactionStatus
import org.springframework.stereotype.Component
import java.time.Instant

// Hand-written domain aggregate -> rows (issue #33 deferral): a Transaction maps to one
// transactions row plus N entries rows that share the post instant.
@Component
class TransactionPersistenceAdapter {
    fun toTransactionEntity(
        transaction: Transaction,
        actor: String,
        postedAt: Instant,
    ): TransactionEntity =
        TransactionEntity(
            id = transaction.id.value,
            reference = transaction.reference,
            description = transaction.description,
            status = TransactionStatus.POSTED,
            reversesId = null,
            metadata = EMPTY_JSON,
            postedAt = postedAt,
            createdAt = postedAt,
            createdBy = actor,
        )

    fun toEntryEntities(
        transaction: Transaction,
        postedAt: Instant,
    ): List<EntryEntity> =
        transaction.entries.map { entry ->
            EntryEntity(
                key = EntryKey(entry.id.value, postedAt),
                transactionId = transaction.id.value,
                accountId = entry.accountId.value,
                amount = entry.amount.amount,
                currency = entry.amount.currency.code,
                direction = entry.direction,
                postedAt = postedAt,
            )
        }

    companion object {
        private const val EMPTY_JSON = "{}"
    }
}
