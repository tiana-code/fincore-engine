// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import com.fincore.ledger.domain.exception.ConcurrencyConflictException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Service

@Service
class TransactionServiceImpl(
    private val poster: TransactionPoster,
) : TransactionService {
    override fun post(command: PostTransactionCommand): PostedTransaction {
        var attempt = 0
        while (true) {
            try {
                return poster.post(command)
            } catch (lock: OptimisticLockingFailureException) {
                attempt++
                if (attempt >= MAX_ATTEMPTS) {
                    throw ConcurrencyConflictException(lock)
                }
            }
        }
    }

    private companion object {
        const val MAX_ATTEMPTS = 3
    }
}
