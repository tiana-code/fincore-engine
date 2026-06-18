// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.application.outbox

import com.fincore.events.OutboxStatus
import com.fincore.payments.infrastructure.persistence.PaymentOutboxEventEntity
import com.fincore.payments.infrastructure.persistence.PaymentOutboxEventRepository
import com.fincore.test.containers.PostgresContainerExtension
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.time.Duration
import java.time.Instant
import java.util.UUID

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(PaymentOutboxStore::class)
@ExtendWith(PostgresContainerExtension::class)
class PaymentOutboxStoreIT(
    @Autowired private val store: PaymentOutboxStore,
    @Autowired private val repository: PaymentOutboxEventRepository,
    @Autowired txManager: PlatformTransactionManager,
) {
    private val tx = TransactionTemplate(txManager)

    @AfterEach
    fun cleanUp() {
        tx.execute { repository.deleteAll() }
    }

    @Test
    fun `should claim a pending row, lease it, and skip it while the lease is held`() {
        val id = insertPending()

        val claimed = store.claim(MAX_ATTEMPTS, LEASE, BATCH_SIZE)
        val again = store.claim(MAX_ATTEMPTS, LEASE, BATCH_SIZE)

        claimed shouldHaveSize 1
        claimed.first().id shouldBe id
        again shouldHaveSize 0
        val row = tx.execute { repository.findById(id).orElseThrow() }!!
        row.status shouldBe OutboxStatus.PUBLISHING
        row.leasedAt.shouldNotBeNull()
    }

    @Test
    fun `should mark a claimed row published and clear the lease`() {
        val id = insertPending()
        store.claim(MAX_ATTEMPTS, LEASE, BATCH_SIZE)

        store.markPublished(id)

        val row = tx.execute { repository.findById(id).orElseThrow() }!!
        row.status shouldBe OutboxStatus.PUBLISHED
        row.publishedAt.shouldNotBeNull()
        row.leasedAt.shouldBeNull()
    }

    @Test
    fun `should mark failed below the cap and permanently failed at the cap`() {
        val id = insertPending()

        store.markFailed(id, attempts = 1, maxAttempts = MAX_ATTEMPTS, error = "boom")
        tx.execute { repository.findById(id).orElseThrow() }!!.status shouldBe OutboxStatus.FAILED

        store.markFailed(id, attempts = MAX_ATTEMPTS, maxAttempts = MAX_ATTEMPTS, error = "boom")
        val row = tx.execute { repository.findById(id).orElseThrow() }!!
        row.status shouldBe OutboxStatus.PERMANENTLY_FAILED
        row.leasedAt.shouldBeNull()
    }

    private fun insertPending(): UUID {
        val id = UUID.randomUUID()
        tx.execute {
            repository.saveAndFlush(
                PaymentOutboxEventEntity(
                    id = id,
                    aggregateType = "Payment",
                    aggregateId = "pay_1",
                    eventType = "PaymentInitiated",
                    payload = "{}",
                    status = OutboxStatus.PENDING,
                    createdAt = Instant.now(),
                    publishedAt = null,
                    attempts = 0,
                    lastError = null,
                    leasedAt = null,
                ),
            )
        }
        return id
    }

    companion object {
        private const val MAX_ATTEMPTS = 10
        private const val BATCH_SIZE = 10
        private val LEASE: Duration = Duration.ofMinutes(5)

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
