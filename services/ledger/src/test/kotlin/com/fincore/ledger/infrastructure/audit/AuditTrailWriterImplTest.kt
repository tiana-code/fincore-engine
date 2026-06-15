// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.infrastructure.audit

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fincore.ledger.application.AuditRecord
import com.fincore.ledger.application.AuditTrailWriter
import com.fincore.ledger.domain.enum.AuditAction
import com.fincore.ledger.domain.enum.AuditResourceType
import com.fincore.ledger.domain.enum.AuditResult
import com.fincore.ledger.infrastructure.persistence.AuditEventEntity
import com.fincore.ledger.infrastructure.persistence.AuditEventRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldHaveLength
import io.kotest.matchers.string.shouldNotBeBlank
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.transaction.support.TransactionSynchronizationManager

class AuditTrailWriterImplTest {
    private val auditRepository = mockk<AuditEventRepository>()
    private val objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
    private val writer: AuditTrailWriter = AuditTrailWriterImpl(auditRepository, objectMapper)

    @AfterEach
    fun tearDown() {
        clearAllMocks()
        MDC.clear()
    }

    @Test
    fun `should throw IllegalStateException when record is called with no active transaction`() {
        mockkStatic(TransactionSynchronizationManager::class)
        every { TransactionSynchronizationManager.isActualTransactionActive() } returns false

        shouldThrow<IllegalStateException> {
            writer.record(
                AuditRecord(
                    actorId = "auth0|actor",
                    action = AuditAction.ACCOUNT_CREATE,
                    resourceType = AuditResourceType.ACCOUNT,
                    resourceId = "acc_01",
                    requestHash = null,
                ),
            )
        }

        verify(exactly = 0) { auditRepository.saveAndFlush(any()) }
    }

    @Test
    fun `should save entity with SUCCESS result and all fields mapped when record is called in active transaction`() {
        mockkStatic(TransactionSynchronizationManager::class)
        every { TransactionSynchronizationManager.isActualTransactionActive() } returns true
        val slot = slot<AuditEventEntity>()
        every { auditRepository.saveAndFlush(capture(slot)) } answers { firstArg() }
        MDC.put("correlation_id", "corr-test-001")

        writer.record(
            AuditRecord(
                actorId = "auth0|actor",
                action = AuditAction.ACCOUNT_CREATE,
                resourceType = AuditResourceType.ACCOUNT,
                resourceId = "acc_01",
                requestHash = "a".repeat(64),
            ),
        )

        val saved = slot.captured
        saved.actorId shouldBe "auth0|actor"
        saved.action shouldBe AuditAction.ACCOUNT_CREATE.name
        saved.resourceType shouldBe AuditResourceType.ACCOUNT.name
        saved.resourceId shouldBe "acc_01"
        saved.result shouldBe AuditResult.SUCCESS
        saved.requestHash shouldBe "a".repeat(64)
        saved.correlationId shouldBe "corr-test-001"
        saved.createdAt.shouldNotBeNull()
        saved.id.shouldNotBeNull()
    }

    @Test
    fun `should store serialized status payload when action is ACCOUNT_STATUS_CHANGE`() {
        mockkStatic(TransactionSynchronizationManager::class)
        every { TransactionSynchronizationManager.isActualTransactionActive() } returns true
        val slot = slot<AuditEventEntity>()
        every { auditRepository.saveAndFlush(capture(slot)) } answers { firstArg() }

        writer.record(
            AuditRecord(
                actorId = "auth0|actor",
                action = AuditAction.ACCOUNT_STATUS_CHANGE,
                resourceType = AuditResourceType.ACCOUNT,
                resourceId = "acc_02",
                requestHash = null,
                payload = mapOf("status" to "FROZEN"),
            ),
        )

        val payloadJson = slot.captured.payload.shouldNotBeNull()
        val tree = objectMapper.readTree(payloadJson)
        tree.get("status").asText() shouldBe "FROZEN"
    }

    @Test
    fun `should include reason and compensatingTransactionId in payload when reversal has reason`() {
        mockkStatic(TransactionSynchronizationManager::class)
        every { TransactionSynchronizationManager.isActualTransactionActive() } returns true
        val slot = slot<AuditEventEntity>()
        every { auditRepository.saveAndFlush(capture(slot)) } answers { firstArg() }

        writer.record(
            AuditRecord(
                actorId = "auth0|actor",
                action = AuditAction.TRANSACTION_REVERSE,
                resourceType = AuditResourceType.TRANSACTION,
                resourceId = "tx_original",
                requestHash = null,
                payload = mapOf("reason" to "duplicate posting", "compensatingTransactionId" to "tx_comp_01"),
            ),
        )

        val payloadJson = slot.captured.payload.shouldNotBeNull()
        val tree = objectMapper.readTree(payloadJson)
        tree.has("reason").shouldBeTrue()
        tree.get("reason").asText() shouldBe "duplicate posting"
        tree.has("compensatingTransactionId").shouldBeTrue()
        tree.get("compensatingTransactionId").asText() shouldBe "tx_comp_01"
    }

    @Test
    fun `should omit reason key from payload when reversal has no reason`() {
        mockkStatic(TransactionSynchronizationManager::class)
        every { TransactionSynchronizationManager.isActualTransactionActive() } returns true
        val slot = slot<AuditEventEntity>()
        every { auditRepository.saveAndFlush(capture(slot)) } answers { firstArg() }

        writer.record(
            AuditRecord(
                actorId = "auth0|actor",
                action = AuditAction.TRANSACTION_REVERSE,
                resourceType = AuditResourceType.TRANSACTION,
                resourceId = "tx_original",
                requestHash = null,
                payload = mapOf("compensatingTransactionId" to "tx_comp_02"),
            ),
        )

        val payloadJson = slot.captured.payload.shouldNotBeNull()
        val tree = objectMapper.readTree(payloadJson)
        tree.has("reason").shouldBeFalse()
        tree.has("compensatingTransactionId").shouldBeTrue()
    }

    @Test
    fun `should store null payload when no context payload is provided`() {
        mockkStatic(TransactionSynchronizationManager::class)
        every { TransactionSynchronizationManager.isActualTransactionActive() } returns true
        val slot = slot<AuditEventEntity>()
        every { auditRepository.saveAndFlush(capture(slot)) } answers { firstArg() }

        writer.record(
            AuditRecord(
                actorId = "auth0|actor",
                action = AuditAction.ACCOUNT_CREATE,
                resourceType = AuditResourceType.ACCOUNT,
                resourceId = "acc_03",
                requestHash = null,
            ),
        )

        slot.captured.payload.shouldBeNull()
    }

    @Test
    fun `should generate a non-blank correlationId when MDC is empty`() {
        mockkStatic(TransactionSynchronizationManager::class)
        every { TransactionSynchronizationManager.isActualTransactionActive() } returns true
        val slot = slot<AuditEventEntity>()
        every { auditRepository.saveAndFlush(capture(slot)) } answers { firstArg() }
        MDC.clear()

        writer.record(
            AuditRecord(
                actorId = "auth0|actor",
                action = AuditAction.ACCOUNT_RENAME,
                resourceType = AuditResourceType.ACCOUNT,
                resourceId = "acc_04",
                requestHash = null,
            ),
        )

        slot.captured.correlationId.shouldNotBeBlank()
    }

    @Test
    fun `should store requestHash as null when operation has no request body`() {
        mockkStatic(TransactionSynchronizationManager::class)
        every { TransactionSynchronizationManager.isActualTransactionActive() } returns true
        val slot = slot<AuditEventEntity>()
        every { auditRepository.saveAndFlush(capture(slot)) } answers { firstArg() }

        writer.record(
            AuditRecord(
                actorId = "auth0|actor",
                action = AuditAction.ACCOUNT_STATUS_CHANGE,
                resourceType = AuditResourceType.ACCOUNT,
                resourceId = "acc_05",
                requestHash = null,
                payload = mapOf("status" to "CLOSED"),
            ),
        )

        slot.captured.requestHash.shouldBeNull()
    }

    @Test
    fun `should save entity with the given result and no active-transaction check when recordOutcome is called`() {
        val slot = slot<AuditEventEntity>()
        every { auditRepository.saveAndFlush(capture(slot)) } answers { firstArg() }

        writer.recordOutcome(
            AuditRecord(
                actorId = "auth0|actor",
                action = AuditAction.TRANSACTION_POST,
                resourceType = AuditResourceType.TRANSACTION,
                resourceId = "unknown",
                requestHash = "c".repeat(64),
                payload = mapOf("code" to "ENTRIES_SUM_NOT_ZERO"),
            ),
            AuditResult.FAILURE,
        )

        val saved = slot.captured
        saved.result shouldBe AuditResult.FAILURE
        saved.resourceId shouldBe "unknown"
        saved.requestHash shouldBe "c".repeat(64)
        val tree = objectMapper.readTree(saved.payload.shouldNotBeNull())
        tree.get("code").asText() shouldBe "ENTRIES_SUM_NOT_ZERO"
    }

    @Test
    fun `should save a DENIED entity when recordOutcome is called with DENIED`() {
        val slot = slot<AuditEventEntity>()
        every { auditRepository.saveAndFlush(capture(slot)) } answers { firstArg() }

        writer.recordOutcome(
            AuditRecord(
                actorId = "auth0|actor",
                action = AuditAction.ACCOUNT_CREATE,
                resourceType = AuditResourceType.ACCOUNT,
                resourceId = "unknown",
                requestHash = null,
                payload = mapOf("code" to "ACCESS_DENIED"),
            ),
            AuditResult.DENIED,
        )

        slot.captured.result shouldBe AuditResult.DENIED
        slot.captured.requestHash.shouldBeNull()
    }

    @Test
    fun `should store a 64-char requestHash when provided`() {
        mockkStatic(TransactionSynchronizationManager::class)
        every { TransactionSynchronizationManager.isActualTransactionActive() } returns true
        val slot = slot<AuditEventEntity>()
        every { auditRepository.saveAndFlush(capture(slot)) } answers { firstArg() }

        val hash = "b".repeat(64)
        writer.record(
            AuditRecord(
                actorId = "auth0|actor",
                action = AuditAction.TRANSACTION_POST,
                resourceType = AuditResourceType.TRANSACTION,
                resourceId = "tx_06",
                requestHash = hash,
            ),
        )

        slot.captured.requestHash.shouldNotBeNull() shouldHaveLength 64
    }
}
