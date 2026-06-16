// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import com.fincore.events.OutboxStatus
import com.fincore.ledger.config.CleanupProperties
import com.fincore.ledger.infrastructure.persistence.IdempotencyKeyEntity
import com.fincore.ledger.infrastructure.persistence.IdempotencyKeyRepository
import com.fincore.ledger.infrastructure.persistence.OutboxEventEntity
import com.fincore.ledger.infrastructure.persistence.OutboxEventRepository
import com.fincore.test.containers.PostgresContainerExtension
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.UUID

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@ExtendWith(PostgresContainerExtension::class)
@EnableConfigurationProperties(CleanupProperties::class)
@Import(CleanupServiceImpl::class)
class CleanupServiceIT(
    @Autowired private val cleanupService: CleanupService,
    @Autowired private val idempotencyKeyRepository: IdempotencyKeyRepository,
    @Autowired private val outboxEventRepository: OutboxEventRepository,
) {
    @AfterEach
    fun tearDown() {
        outboxEventRepository.deleteAll()
        idempotencyKeyRepository.deleteAll()
    }

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun datasourceProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { PostgresContainerExtension.jdbcUrl }
            registry.add("spring.datasource.username") { PostgresContainerExtension.username }
            registry.add("spring.datasource.password") { PostgresContainerExtension.password }
            registry.add("spring.jpa.hibernate.ddl-auto") { "none" }
        }
    }

    @Test
    fun `should purge expired keys and old published events while retaining the rest`() {
        val now = Instant.now()
        idempotencyKeyRepository.save(idempotencyKey("expired", now.minus(Duration.ofHours(1))))
        idempotencyKeyRepository.save(idempotencyKey("fresh", now.plus(Duration.ofHours(1))))
        outboxEventRepository.save(publishedEvent(now.minus(Duration.ofDays(40))))
        outboxEventRepository.save(publishedEvent(now.minus(Duration.ofDays(1))))
        outboxEventRepository.save(unpublishedEvent(OutboxStatus.PENDING, now.minus(Duration.ofDays(60))))

        val result = cleanupService.purge()

        result.idempotencyKeysDeleted shouldBe 1
        result.outboxEventsDeleted shouldBe 1
        idempotencyKeyRepository.existsById("expired") shouldBe false
        idempotencyKeyRepository.existsById("fresh") shouldBe true
        outboxEventRepository.count() shouldBe 2
    }

    @Test
    fun `should never delete unpublished outbox events regardless of age`() {
        val old = Instant.now().minus(Duration.ofDays(40))
        outboxEventRepository.save(unpublishedEvent(OutboxStatus.PENDING, old))
        outboxEventRepository.save(unpublishedEvent(OutboxStatus.PUBLISHING, old))
        outboxEventRepository.save(unpublishedEvent(OutboxStatus.FAILED, old))
        outboxEventRepository.save(unpublishedEvent(OutboxStatus.PERMANENTLY_FAILED, old))

        val result = cleanupService.purge()

        result.outboxEventsDeleted shouldBe 0
        outboxEventRepository.count() shouldBe 4
    }

    @Test
    fun `should be idempotent when run again with nothing left to purge`() {
        idempotencyKeyRepository.save(idempotencyKey("expired", Instant.now().minus(Duration.ofHours(1))))
        outboxEventRepository.save(publishedEvent(Instant.now().minus(Duration.ofDays(40))))

        cleanupService.purge()
        val second = cleanupService.purge()

        second shouldBe CleanupResult(0, 0)
    }

    private fun idempotencyKey(
        keyHash: String,
        expiresAt: Instant,
    ): IdempotencyKeyEntity =
        IdempotencyKeyEntity(
            keyHash = keyHash,
            requestHash = "hash",
            statusCode = null,
            responseBody = null,
            createdAt = Instant.now(),
            expiresAt = expiresAt,
        )

    private fun publishedEvent(publishedAt: Instant): OutboxEventEntity =
        outboxEvent(OutboxStatus.PUBLISHED, createdAt = publishedAt, publishedAt = publishedAt)

    private fun unpublishedEvent(
        status: OutboxStatus,
        createdAt: Instant,
    ): OutboxEventEntity = outboxEvent(status, createdAt = createdAt, publishedAt = null)

    private fun outboxEvent(
        status: OutboxStatus,
        createdAt: Instant,
        publishedAt: Instant?,
    ): OutboxEventEntity =
        OutboxEventEntity(
            id = UUID.randomUUID(),
            aggregateType = "Transaction",
            aggregateId = "tx_1",
            eventType = "ledger.transaction.posted",
            payload = "{}",
            status = status,
            createdAt = createdAt,
            publishedAt = publishedAt,
            attempts = 0,
            lastError = null,
        )
}
