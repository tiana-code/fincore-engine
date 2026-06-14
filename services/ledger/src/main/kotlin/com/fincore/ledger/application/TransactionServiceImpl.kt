// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import com.fincore.core.TransactionId
import com.fincore.ledger.domain.exception.ConcurrencyConflictException
import com.fincore.ledger.infrastructure.persistence.TransactionEntity
import com.fincore.ledger.infrastructure.persistence.TransactionRepository
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TransactionServiceImpl(
    private val poster: TransactionPoster,
    private val transactionRepository: TransactionRepository,
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

    @Transactional(readOnly = true)
    override fun list(
        page: Int,
        size: Int,
    ): TransactionPage {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("postedAt"), Sort.Order.desc("id")))
        val result = transactionRepository.findAll(pageable)
        return TransactionPage(
            items = result.content.map(::toSummary),
            page = page,
            size = size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }

    private fun toSummary(entity: TransactionEntity): TransactionSummary =
        TransactionSummary(TransactionId(entity.id), entity.reference, entity.status, entity.postedAt)

    private companion object {
        const val MAX_ATTEMPTS = 3
    }
}
