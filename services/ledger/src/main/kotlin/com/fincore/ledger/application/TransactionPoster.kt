// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.fincore.core.AccountId
import com.fincore.core.Currency
import com.fincore.core.EntryId
import com.fincore.core.Money
import com.fincore.core.TransactionId
import com.fincore.events.EventEnvelope
import com.fincore.events.LedgerEvents
import com.fincore.events.OutboxStatus
import com.fincore.ledger.application.event.EntryLinePayload
import com.fincore.ledger.application.event.TransactionPostedPayload
import com.fincore.ledger.domain.Entry
import com.fincore.ledger.domain.Transaction
import com.fincore.ledger.domain.enum.AccountStatus
import com.fincore.ledger.domain.enum.TransactionStatus
import com.fincore.ledger.domain.exception.AccountNotFoundException
import com.fincore.ledger.domain.exception.DomainException
import com.fincore.ledger.domain.exception.DuplicateTransactionException
import com.fincore.ledger.domain.exception.TransactionAlreadyReversedException
import com.fincore.ledger.domain.exception.TransactionNotFoundException
import com.fincore.ledger.infrastructure.persistence.AccountBalanceEntity
import com.fincore.ledger.infrastructure.persistence.AccountBalanceKey
import com.fincore.ledger.infrastructure.persistence.AccountBalanceRepository
import com.fincore.ledger.infrastructure.persistence.AccountRepository
import com.fincore.ledger.infrastructure.persistence.EntryEntity
import com.fincore.ledger.infrastructure.persistence.EntryRepository
import com.fincore.ledger.infrastructure.persistence.OutboxEventEntity
import com.fincore.ledger.infrastructure.persistence.OutboxEventRepository
import com.fincore.ledger.infrastructure.persistence.TransactionPersistenceAdapter
import com.fincore.ledger.infrastructure.persistence.TransactionRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Component
class TransactionPoster(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val entryRepository: EntryRepository,
    private val balanceRepository: AccountBalanceRepository,
    private val outboxRepository: OutboxEventRepository,
    private val adapter: TransactionPersistenceAdapter,
    private val objectMapper: ObjectMapper,
) {
    @Transactional
    fun post(command: PostTransactionCommand): PostedTransaction {
        val transaction = buildDomain(command)
        guardAccounts(transaction, command.currency)
        if (transactionRepository.existsByReference(command.reference)) {
            throw DuplicateTransactionException(command.reference)
        }
        val postedAt = Instant.now()
        try {
            write(transaction, command.actor, postedAt, reversesId = null, correlationId = command.correlationId)
        } catch (duplicate: DataIntegrityViolationException) {
            throw DuplicateTransactionException(command.reference, duplicate)
        }
        return PostedTransaction(transaction.id, transaction.reference, transaction.status, postedAt)
    }

    @Transactional
    fun postReversal(
        originalId: TransactionId,
        actor: String,
        correlationId: String?,
    ): PostedTransaction {
        val original =
            transactionRepository.findById(originalId.value).orElseThrow { TransactionNotFoundException(originalId) }
        if (original.status != TransactionStatus.POSTED) {
            throw TransactionAlreadyReversedException(originalId)
        }
        val compensating = buildReversal(originalId, original.reference, entryRepository.findByTransactionId(originalId.value))
        original.status = TransactionStatus.REVERSED
        transactionRepository.saveAndFlush(original)
        val postedAt = Instant.now()
        try {
            write(compensating, actor, postedAt, reversesId = originalId.value, correlationId = correlationId)
        } catch (conflict: DataIntegrityViolationException) {
            throw TransactionAlreadyReversedException(originalId, conflict)
        }
        return PostedTransaction(compensating.id, compensating.reference, compensating.status, postedAt)
    }

    private fun buildDomain(command: PostTransactionCommand): Transaction {
        val entries =
            command.entries.map { line ->
                Entry(EntryId.generate(), line.accountId, line.direction, Money.of(line.amount, command.currency))
            }
        return Transaction(TransactionId.generate(), command.reference, command.description, entries, command.currency)
    }

    private fun buildReversal(
        originalId: TransactionId,
        originalReference: String,
        originalEntries: List<EntryEntity>,
    ): Transaction {
        val currency = Currency.of(originalEntries.first().currency)
        val inverted =
            originalEntries.map { entry ->
                Entry(
                    EntryId.generate(),
                    AccountId(entry.accountId),
                    entry.direction.opposite(),
                    Money(entry.amount.negate(), Currency.of(entry.currency)),
                )
            }
        return Transaction(
            TransactionId.generate(),
            "reversal-of-$originalId",
            "reversal of $originalReference",
            inverted,
            currency,
        )
    }

    private fun guardAccounts(
        transaction: Transaction,
        currency: Currency,
    ) {
        transaction.entries.map { it.accountId }.distinct().forEach { accountId ->
            val account =
                accountRepository.findById(accountId.value).orElseThrow { AccountNotFoundException(accountId) }
            if (account.status != AccountStatus.ACTIVE) {
                throw DomainException("Cannot post to non-active account $accountId with status ${account.status}")
            }
            if (account.currency != currency.code) {
                throw DomainException(
                    "Account $accountId currency ${account.currency} does not match transaction currency $currency",
                )
            }
        }
    }

    private fun write(
        transaction: Transaction,
        actor: String,
        postedAt: Instant,
        reversesId: UUID?,
        correlationId: String?,
    ) {
        transactionRepository.saveAndFlush(adapter.toTransactionEntity(transaction, actor, postedAt, reversesId))
        adapter.toEntryEntities(transaction, postedAt).forEach { entryRepository.saveAndFlush(it) }
        applyBalances(transaction, postedAt)
        emit(transaction, correlationId, postedAt)
    }

    private fun applyBalances(
        transaction: Transaction,
        postedAt: Instant,
    ) {
        transaction.entries.forEach { entry ->
            val key = AccountBalanceKey(entry.accountId.value, entry.amount.currency.code)
            val existing = balanceRepository.findById(key).orElse(null)
            if (existing == null) {
                balanceRepository.saveAndFlush(AccountBalanceEntity(key, entry.amount.amount, postedAt, 0))
            } else {
                existing.balance = existing.balance.add(entry.amount.amount)
                existing.lastPostedAt = postedAt
                balanceRepository.saveAndFlush(existing)
            }
        }
    }

    private fun emit(
        transaction: Transaction,
        correlationId: String?,
        postedAt: Instant,
    ) {
        val envelope =
            EventEnvelope.of(
                source = SOURCE,
                type = LedgerEvents.TransactionPosted,
                data = buildPayload(transaction, postedAt),
                subject = transaction.id.toString(),
                correlationId = correlationId,
            )
        outboxRepository.saveAndFlush(
            OutboxEventEntity(
                id = UUID.randomUUID(),
                aggregateType = AGGREGATE_TYPE,
                aggregateId = transaction.id.toString(),
                eventType = LedgerEvents.TransactionPosted.fullType,
                payload = objectMapper.writeValueAsString(envelope),
                status = OutboxStatus.PENDING,
                createdAt = postedAt,
                publishedAt = null,
                attempts = 0,
                lastError = null,
            ),
        )
    }

    private fun buildPayload(
        transaction: Transaction,
        postedAt: Instant,
    ): TransactionPostedPayload =
        TransactionPostedPayload(
            transactionId = transaction.id.toString(),
            reference = transaction.reference,
            currency = transaction.currency.code,
            postedAt = postedAt,
            entries =
                transaction.entries.map {
                    EntryLinePayload(it.accountId.toString(), it.direction.name, it.amount.amount.toPlainString())
                },
        )

    private companion object {
        const val SOURCE = "ledger-service"
        const val AGGREGATE_TYPE = "Transaction"
    }
}
