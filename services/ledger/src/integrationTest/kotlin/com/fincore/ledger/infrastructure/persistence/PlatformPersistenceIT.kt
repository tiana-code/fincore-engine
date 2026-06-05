// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.infrastructure.persistence

import com.fincore.events.OutboxStatus
import com.fincore.ledger.domain.enum.AuditResult
import com.fincore.test.containers.PostgresContainerExtension
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.time.Instant
import java.util.UUID

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ExtendWith(PostgresContainerExtension::class)
class PlatformPersistenceIT(
    @Autowired private val outboxRepository: OutboxEventRepository,
    @Autowired private val idempotencyRepository: IdempotencyKeyRepository,
    @Autowired private val auditRepository: AuditEventRepository,
    @Autowired private val entityManager: TestEntityManager,
) {
    @Test
    fun `should round trip an outbox event entity`() {
        val id = UUID.randomUUID()
        outboxRepository.saveAndFlush(
            OutboxEventEntity(
                id = id,
                aggregateType = "Account",
                aggregateId = "acc_01HZX",
                eventType = "com.fincore.ledger.account.created.v1",
                payload = "{\"name\": \"acct\"}",
                status = OutboxStatus.PENDING,
                createdAt = Instant.parse("2026-06-05T12:00:00Z"),
                publishedAt = null,
                attempts = 0,
                lastError = null,
            ),
        )
        entityManager.clear()

        val loaded = outboxRepository.findById(id).orElseThrow()
        loaded.aggregateId shouldBe "acc_01HZX"
        loaded.status shouldBe OutboxStatus.PENDING
        loaded.payload shouldBe "{\"name\": \"acct\"}"
        loaded.attempts shouldBe 0
        loaded.publishedAt shouldBe null
    }

    @Test
    fun `should round trip an idempotency key entity`() {
        val keyHash = "a".repeat(64)
        idempotencyRepository.saveAndFlush(
            IdempotencyKeyEntity(
                keyHash = keyHash,
                requestHash = "b".repeat(64),
                statusCode = null,
                responseBody = null,
                createdAt = Instant.parse("2026-06-05T12:00:00Z"),
                expiresAt = Instant.parse("2026-06-06T12:00:00Z"),
            ),
        )
        entityManager.clear()

        val loaded = idempotencyRepository.findById(keyHash).orElseThrow()
        loaded.requestHash shouldBe "b".repeat(64)
        loaded.statusCode shouldBe null
        loaded.responseBody shouldBe null
    }

    @Test
    fun `should round trip an audit event entity`() {
        val id = UUID.randomUUID()
        auditRepository.saveAndFlush(
            AuditEventEntity(
                id = id,
                actorId = "auth0|operator",
                correlationId = "corr-1",
                action = "ACCOUNT_CREATE",
                resourceType = "ACCOUNT",
                resourceId = "acc_01HZX",
                result = AuditResult.SUCCESS,
                requestHash = null,
                createdAt = Instant.parse("2026-06-05T12:00:00Z"),
            ),
        )
        entityManager.clear()

        val loaded = auditRepository.findById(id).orElseThrow()
        loaded.actorId shouldBe "auth0|operator"
        loaded.result shouldBe AuditResult.SUCCESS
        loaded.requestHash shouldBe null
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
}
