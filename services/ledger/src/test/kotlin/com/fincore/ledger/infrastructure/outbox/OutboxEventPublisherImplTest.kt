// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.infrastructure.outbox

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fincore.events.EventEnvelope
import com.fincore.events.LedgerEvents
import com.fincore.events.OutboxStatus
import com.fincore.ledger.infrastructure.persistence.OutboxEventEntity
import com.fincore.ledger.infrastructure.persistence.OutboxEventRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Instant

class OutboxEventPublisherImplTest {
    private val outboxRepository = mockk<OutboxEventRepository>()
    private val objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
    private val publisher = OutboxEventPublisherImpl(outboxRepository, objectMapper)

    private val createdAt = Instant.parse("2026-06-15T10:00:00Z")
    private val envelope: EventEnvelope<Map<String, String>> =
        EventEnvelope.of(
            source = "ledger",
            type = LedgerEvents.TransactionPosted,
            data = mapOf("ref" to "ref-1"),
            subject = "tx_01",
            correlationId = "corr-1",
        )

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `should persist a pending row with zero attempts when publish is called in an active transaction`() {
        mockkStatic(TransactionSynchronizationManager::class)
        every { TransactionSynchronizationManager.isActualTransactionActive() } returns true
        val slot = slot<OutboxEventEntity>()
        every { outboxRepository.saveAndFlush(capture(slot)) } answers { firstArg() }

        publisher.publish(envelope, "Transaction", "tx-uuid", LedgerEvents.TransactionPosted.fullType, createdAt)

        val saved = slot.captured
        saved.aggregateType shouldBe "Transaction"
        saved.aggregateId shouldBe "tx-uuid"
        saved.eventType shouldBe LedgerEvents.TransactionPosted.fullType
        saved.status shouldBe OutboxStatus.PENDING
        saved.attempts shouldBe 0
        saved.publishedAt shouldBe null
        saved.lastError shouldBe null
        saved.createdAt shouldBe createdAt
    }

    @Test
    fun `should serialize the envelope into the payload when publish is called`() {
        mockkStatic(TransactionSynchronizationManager::class)
        every { TransactionSynchronizationManager.isActualTransactionActive() } returns true
        val slot = slot<OutboxEventEntity>()
        every { outboxRepository.saveAndFlush(capture(slot)) } answers { firstArg() }

        publisher.publish(envelope, "Transaction", "tx-uuid", LedgerEvents.TransactionPosted.fullType, createdAt)

        slot.captured.payload shouldBe objectMapper.writeValueAsString(envelope)
    }

    @Test
    fun `should throw IllegalStateException when no active transaction is present`() {
        mockkStatic(TransactionSynchronizationManager::class)
        every { TransactionSynchronizationManager.isActualTransactionActive() } returns false

        shouldThrow<IllegalStateException> {
            publisher.publish(envelope, "Transaction", "tx-uuid", LedgerEvents.TransactionPosted.fullType, createdAt)
        }

        verify(exactly = 0) { outboxRepository.saveAndFlush(any()) }
    }

    @Test
    fun `should set pending status and null published at when caller supplies only metadata`() {
        mockkStatic(TransactionSynchronizationManager::class)
        every { TransactionSynchronizationManager.isActualTransactionActive() } returns true
        val slot = slot<OutboxEventEntity>()
        every { outboxRepository.saveAndFlush(capture(slot)) } answers { firstArg() }

        publisher.publish(envelope, "Account", "acc-uuid", LedgerEvents.AccountCreated.fullType, createdAt)

        slot.captured.status shouldBe OutboxStatus.PENDING
        slot.captured.publishedAt shouldBe null
    }
}
