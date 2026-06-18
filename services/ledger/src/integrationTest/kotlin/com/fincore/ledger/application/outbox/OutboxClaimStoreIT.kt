// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application.outbox

import com.fincore.events.OutboxStatus
import com.fincore.ledger.infrastructure.persistence.OutboxEventEntity
import com.fincore.ledger.infrastructure.persistence.OutboxEventRepository
import com.fincore.test.containers.PostgresContainerExtension
import io.kotest.matchers.collections.shouldBeEmpty
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
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@ExtendWith(PostgresContainerExtension::class)
@Import(OutboxClaimStore::class)
class OutboxClaimStoreIT(
    @Autowired private val claimStore: OutboxClaimStore,
    @Autowired private val repository: OutboxEventRepository,
    @Autowired private val transactionManager: PlatformTransactionManager,
) {
    private val leaseTimeout: Duration = Duration.ofMinutes(5)

    @AfterEach
    fun cleanUp() {
        repository.deleteAll()
    }

    @Test
    fun `should mark claimed rows publishing and stamp the lease when claimed`() {
        val ids = seedPending(2, "claim")

        val claimed = claimStore.claim(MAX_ATTEMPTS, leaseTimeout, BATCH)

        claimed shouldHaveSize 2
        ids.forEach { id ->
            val row = repository.findById(id).orElseThrow()
            row.status shouldBe OutboxStatus.PUBLISHING
            row.leasedAt.shouldNotBeNull()
        }
    }

    @Test
    fun `should not re-claim a freshly leased publishing row when claimed again`() {
        seedPending(2, "fresh")

        claimStore.claim(MAX_ATTEMPTS, leaseTimeout, BATCH)
        val second = claimStore.claim(MAX_ATTEMPTS, leaseTimeout, BATCH)

        second.shouldBeEmpty()
    }

    @Test
    fun `should re-claim an orphaned publishing row whose lease expired when claimed`() {
        val orphan = seedRow(OutboxStatus.PUBLISHING, attempts = 0, leasedAt = Instant.now().minus(Duration.ofMinutes(10)))

        val claimed = claimStore.claim(MAX_ATTEMPTS, leaseTimeout, BATCH)

        claimed.map { it.id } shouldBe listOf(orphan)
    }

    @Test
    fun `should mark a row published when settled successfully`() {
        val id = seedRow(OutboxStatus.PUBLISHING, attempts = 0, leasedAt = Instant.now())

        claimStore.markPublished(id)

        val row = repository.findById(id).orElseThrow()
        row.status shouldBe OutboxStatus.PUBLISHED
        row.publishedAt.shouldNotBeNull()
        row.leasedAt shouldBe null
    }

    @Test
    fun `should transition to failed when retry attempts remain`() {
        val id = seedRow(OutboxStatus.PUBLISHING, attempts = 0, leasedAt = Instant.now())

        claimStore.markFailed(id, attempts = 1, maxAttempts = 3, error = "broker down")

        val row = repository.findById(id).orElseThrow()
        row.status shouldBe OutboxStatus.FAILED
        row.attempts shouldBe 1
        row.lastError shouldBe "broker down"
        row.leasedAt shouldBe null
    }

    @Test
    fun `should transition to permanently failed when retry attempts are exhausted`() {
        val id = seedRow(OutboxStatus.PUBLISHING, attempts = 2, leasedAt = Instant.now())

        claimStore.markFailed(id, attempts = 3, maxAttempts = 3, error = "still down")

        repository.findById(id).orElseThrow().status shouldBe OutboxStatus.PERMANENTLY_FAILED
    }

    @Test
    fun `should not return the same row to two concurrent claims under skip locked`() {
        seedPending(2 * BATCH, "concurrent")
        val template = TransactionTemplate(transactionManager)
        val aLocked = CountDownLatch(1)
        val bDone = CountDownLatch(1)
        val idsA = CopyOnWriteArrayList<UUID>()
        val idsB = CopyOnWriteArrayList<UUID>()
        val holderFailure = AtomicReference<Throwable>()

        val holder =
            Thread {
                template.executeWithoutResult {
                    idsA.addAll(lockBatchIds())
                    aLocked.countDown()
                    bDone.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                }
            }
        holder.setUncaughtExceptionHandler { _, ex -> holderFailure.set(ex) }
        holder.start()
        aLocked.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        template.executeWithoutResult { idsB.addAll(lockBatchIds()) }
        bDone.countDown()
        holder.join(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS))

        holderFailure.get().shouldBeNull()
        idsA shouldHaveSize BATCH
        idsB shouldHaveSize BATCH
        idsA.toSet().intersect(idsB.toSet()).shouldBeEmpty()
    }

    @Test
    fun `should claim a failed row below max attempts but skip one at max attempts`() {
        val claimable = seedRow(OutboxStatus.FAILED, attempts = MAX_ATTEMPTS - 1, leasedAt = null)
        seedRow(OutboxStatus.FAILED, attempts = MAX_ATTEMPTS, leasedAt = null)

        val claimed = claimStore.claim(MAX_ATTEMPTS, leaseTimeout, BATCH)

        claimed.map { it.id } shouldBe listOf(claimable)
    }

    private fun lockBatchIds(): List<UUID> =
        repository.lockClaimableBatch(MAX_ATTEMPTS, Instant.now().minus(leaseTimeout), BATCH).map { it.id }

    private fun seedPending(
        count: Int,
        prefix: String,
    ): List<UUID> = (1..count).map { seedRow(OutboxStatus.PENDING, attempts = 0, leasedAt = null, aggregateId = "$prefix-$it") }

    private fun seedRow(
        status: OutboxStatus,
        attempts: Int,
        leasedAt: Instant?,
        aggregateId: String = "tx_1",
    ): UUID =
        repository
            .save(
                OutboxEventEntity(
                    id = UUID.randomUUID(),
                    aggregateType = "Transaction",
                    aggregateId = aggregateId,
                    eventType = "com.fincore.ledger.transaction.posted.v1",
                    payload = "{\"id\":\"e1\"}",
                    status = status,
                    createdAt = Instant.now(),
                    publishedAt = null,
                    attempts = attempts,
                    lastError = null,
                    leasedAt = leasedAt,
                ),
            ).id

    companion object {
        private const val BATCH = 3
        private const val MAX_ATTEMPTS = 10
        private const val TIMEOUT_SECONDS = 10L

        @JvmStatic
        @DynamicPropertySource
        fun datasourceProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { PostgresContainerExtension.jdbcUrl }
            registry.add("spring.datasource.username") { PostgresContainerExtension.username }
            registry.add("spring.datasource.password") { PostgresContainerExtension.password }
            registry.add("spring.datasource.hikari.maximum-pool-size") { "5" }
            registry.add("spring.jpa.hibernate.ddl-auto") { "none" }
        }
    }
}
