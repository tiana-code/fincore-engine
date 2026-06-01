// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.domain

import com.fincore.core.TransactionId
import com.fincore.ledger.domain.enum.TransactionStatus
import com.fincore.ledger.domain.exception.DomainException
import com.fincore.ledger.domain.exception.DoubleEntryViolationException
import java.math.BigDecimal

class Transaction(
    val id: TransactionId,
    val reference: String,
    val description: String?,
    val entries: List<Entry>,
) {
    companion object {
        private const val MIN_ENTRIES = 2
        private const val MAX_ENTRIES = 1000
    }

    val status: TransactionStatus = TransactionStatus.POSTED

    init {
        validateEntryCount()
        validateNoDuplicatePairs()
        validateDoubleEntryBalance()
    }

    private fun validateEntryCount() {
        if (entries.size !in MIN_ENTRIES..MAX_ENTRIES) {
            throw DomainException(
                "Transaction $id must have $MIN_ENTRIES..$MAX_ENTRIES entries, got ${entries.size}",
            )
        }
    }

    private fun validateNoDuplicatePairs() {
        val seen = mutableSetOf<Pair<*, *>>()
        for (entry in entries) {
            val key = Pair(entry.accountId, entry.direction)
            if (!seen.add(key)) {
                throw DomainException(
                    "Duplicate (accountId, direction) pair in transaction $id: " +
                        "accountId=${entry.accountId} direction=${entry.direction}",
                )
            }
        }
    }

    private fun validateDoubleEntryBalance() {
        val netByCurrency =
            entries
                .groupBy { it.amount.currency }
                .mapValues { (_, groupedEntries) ->
                    groupedEntries.fold(BigDecimal.ZERO) { acc, entry ->
                        acc.add(entry.amount.amount)
                    }
                }

        for ((currency, net) in netByCurrency) {
            if (net.compareTo(BigDecimal.ZERO) != 0) {
                throw DoubleEntryViolationException(
                    "Double-entry balance violated for currency $currency in transaction $id: net=$net",
                )
            }
        }
    }
}
