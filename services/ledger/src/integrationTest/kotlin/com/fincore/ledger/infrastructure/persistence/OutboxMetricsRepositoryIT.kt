// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.infrastructure.persistence

import com.fincore.events.OutboxStatus
import com.fincore.test.containers.PostgresContainerExtension
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@ExtendWith(PostgresContainerExtension::class)
class OutboxMetricsRepositoryIT(
    @Autowired private val repository: OutboxEventRepository,
) {
    @AfterEach
    fun cleanUp() {
        repository.deleteAll()
    }

    @Test
    fun `should count rows by status`() {
        seed(OutboxStatus.PENDING, Instant.parse("2026-06-18T10:00:00Z"))
        seed(OutboxStatus.PENDING, Instant.parse("2026-06-18T10:05:00Z"))
        seed(OutboxStatus.FAILED, Instant.parse("2026-06-18T10:00:00Z"))

        repository.countByStatus(OutboxStatus.PENDING) shouldBe 2L
        repository.countByStatus(OutboxStatus.FAILED) shouldBe 1L
        repository.countByStatus(OutboxStatus.PERMANENTLY_FAILED) shouldBe 0L
    }

    @Test
    fun `should return the oldest created-at for a status and null when none`() {
        val oldest = Instant.parse("2026-06-18T10:00:00Z")
        seed(OutboxStatus.PENDING, Instant.parse("2026-06-18T10:05:00Z"))
        seed(OutboxStatus.PENDING, oldest)

        repository.oldestCreatedAt(OutboxStatus.PENDING) shouldBe oldest
        repository.oldestCreatedAt(OutboxStatus.PUBLISHED).shouldBeNull()
    }

    private fun seed(
        status: OutboxStatus,
        createdAt: Instant,
    ) {
        repository.save(
            OutboxEventEntity(
                id = UUID.randomUUID(),
                aggregateType = "Transaction",
                aggregateId = UUID.randomUUID().toString(),
                eventType = "com.fincore.ledger.transaction.posted.v1",
                payload = "{\"id\":\"e1\"}",
                status = status,
                createdAt = createdAt,
                publishedAt = null,
                attempts = 0,
                lastError = null,
                leasedAt = null,
            ),
        )
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
