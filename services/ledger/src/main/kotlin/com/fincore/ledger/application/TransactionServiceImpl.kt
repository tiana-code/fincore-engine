// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import com.fincore.core.AccountId
import com.fincore.core.TransactionId
import com.fincore.ledger.domain.exception.TransactionNotFoundException
import com.fincore.ledger.infrastructure.persistence.EntryEntity
import com.fincore.ledger.infrastructure.persistence.EntryRepository
import com.fincore.ledger.infrastructure.persistence.TransactionEntity
import com.fincore.ledger.infrastructure.persistence.TransactionRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TransactionServiceImpl(
    private val poster: TransactionPoster,
    private val transactionRepository: TransactionRepository,
    private val entryRepository: EntryRepository,
) : TransactionService {
    override fun post(command: PostTransactionCommand): PostedTransaction = poster.post(command)

    override fun reverse(
        id: TransactionId,
        actor: String,
        correlationId: String?,
        reason: String?,
        requestHash: String?,
    ): PostedTransaction = poster.postReversal(id, actor, correlationId, reason, requestHash)

    @Transactional(readOnly = true)
    override fun get(id: TransactionId): TransactionDetail {
        val transaction =
            transactionRepository.findById(id.value).orElseThrow { TransactionNotFoundException(id) }
        val entries = entryRepository.findByTransactionId(id.value).map(::toView)
        return toDetail(transaction, entries)
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

    private fun toDetail(
        entity: TransactionEntity,
        entries: List<EntryView>,
    ): TransactionDetail =
        TransactionDetail(
            id = TransactionId(entity.id),
            reference = entity.reference,
            description = entity.description,
            status = entity.status,
            reversesId = entity.reversesId?.let(::TransactionId),
            postedAt = entity.postedAt,
            entries = entries,
        )

    private fun toView(entry: EntryEntity): EntryView = EntryView(AccountId(entry.accountId), entry.direction, entry.amount, entry.currency)
}
