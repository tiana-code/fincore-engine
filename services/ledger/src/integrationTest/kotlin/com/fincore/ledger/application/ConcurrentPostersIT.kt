// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fincore.core.AccountId
import com.fincore.core.Currency
import com.fincore.core.IdempotencyKey
import com.fincore.ledger.config.IdempotencyProperties
import com.fincore.ledger.domain.enum.AccountType
import com.fincore.ledger.domain.enum.EntryDirection
import com.fincore.ledger.domain.exception.ConcurrencyConflictException
import com.fincore.ledger.infrastructure.audit.AuditTrailWriterImpl
import com.fincore.ledger.infrastructure.outbox.OutboxEventPublisherImpl
import com.fincore.ledger.infrastructure.persistence.AccountPersistenceAdapter
import com.fincore.ledger.infrastructure.persistence.EntryRepository
import com.fincore.ledger.infrastructure.persistence.OutboxEventRepository
import com.fincore.ledger.infrastructure.persistence.TransactionPersistenceAdapter
import com.fincore.ledger.infrastructure.persistence.TransactionRepository
import com.fincore.test.containers.PostgresContainerExtension
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Proves the no-lost-update invariant under real contention: POSTERS virtual threads each commit a
 * distinct balanced transaction to one shared account pair through the idempotency-wrapped path, where the
 * bounded optimistic-retry (IdempotencyServiceImpl.runWithRetry) absorbs the @Version conflicts on the
 * shared account_balances rows.
 *
 * @Transactional(NOT_SUPPORTED) at class level so worker threads commit in their own physical tx and the
 * read-back sees committed state (INV-6). Assertions are phrased over the observed success count S, so the
 * test is non-flaky regardless of how the scheduler resolves contention: every poster resolves to either a
 * committed success or a typed ConcurrencyConflictException, never a raw OptimisticLockingFailureException.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@ExtendWith(PostgresContainerExtension::class)
@EnableConfigurationProperties(IdempotencyProperties::class)
@Import(
    AccountServiceImpl::class,
    AccountPersistenceAdapter::class,
    TransactionServiceImpl::class,
    TransactionPoster::class,
    TransactionPersistenceAdapter::class,
    BalanceServiceImpl::class,
    IdempotencyServiceImpl::class,
    IdempotencyStore::class,
    AuditTrailWriterImpl::class,
    OutboxEventPublisherImpl::class,
    ConcurrentPostersIT.JacksonConfig::class,
    MetricsTestConfig::class,
)
class ConcurrentPostersIT(
    @Autowired private val accountService: AccountService,
    @Autowired private val transactionService: TransactionService,
    @Autowired private val idempotencyService: IdempotencyService,
    @Autowired private val balanceService: BalanceService,
    @Autowired private val transactionRepository: TransactionRepository,
    @Autowired private val entryRepository: EntryRepository,
    @Autowired private val outboxRepository: OutboxEventRepository,
) {
    @TestConfiguration
    class JacksonConfig {
        @Bean
        fun objectMapper(): ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
    }

    @AfterEach
    fun cleanOutbox() {
        outboxRepository.deleteAll()
    }

    private fun newAccount() =
        accountService.create(CreateAccountCommand("Concurrency Account", AccountType.USER_WALLET, Currency.USD, ACTOR))

    @Test
    fun `should preserve every committed posting without lost updates when posters contend on one account pair`() {
        val debit = newAccount()
        val credit = newAccount()
        val referencePrefix = "ref-conc-${UUID.randomUUID()}"
        val successCount = AtomicInteger(0)
        val conflictCount = AtomicInteger(0)
        val unexpected = AtomicReference<Throwable?>(null)
        val startGate = CountDownLatch(1)

        Executors.newVirtualThreadPerTaskExecutor().use { executor ->
            val futures =
                (0 until POSTERS).map { index ->
                    executor.submit(
                        Callable {
                            startGate.await()
                            runPoster(debit.id, credit.id, "$referencePrefix-$index", successCount, conflictCount, unexpected)
                        },
                    )
                }
            startGate.countDown()
            futures.forEach { it.get() }
        }

        unexpected.get()?.let { throw AssertionError("a poster surfaced an unexpected throwable: ${it::class.qualifiedName}", it) }
        val succeeded = successCount.get()
        succeeded shouldBeGreaterThanOrEqualTo 1
        succeeded shouldBeLessThanOrEqualTo POSTERS
        (succeeded + conflictCount.get()) shouldBe POSTERS

        val expectedDebit = BigDecimal(AMOUNT).multiply(BigDecimal(succeeded))
        val debitBalance = balanceService.current(debit.id, Currency.USD).amount.amount
        val creditBalance = balanceService.current(credit.id, Currency.USD).amount.amount
        debitBalance.compareTo(expectedDebit) shouldBe 0
        creditBalance.compareTo(expectedDebit.negate()) shouldBe 0
        debitBalance.compareTo(BigDecimal.ZERO) shouldNotBe 0
        transactionRepository.findAll().count { it.reference.startsWith(referencePrefix) } shouldBe succeeded
        entryRepository.findAll().count { it.accountId == debit.id.value || it.accountId == credit.id.value } shouldBe 2 * succeeded
    }

    @Suppress("TooGenericExceptionCaught", "LongParameterList")
    private fun runPoster(
        debit: AccountId,
        credit: AccountId,
        reference: String,
        successCount: AtomicInteger,
        conflictCount: AtomicInteger,
        unexpected: AtomicReference<Throwable?>,
    ) {
        try {
            idempotencyService.execute(IdempotencyKey.generate(), body(reference)) { hash ->
                val posted = transactionService.post(command(reference, debit, credit, hash))
                StoredResponse(CREATED, """{"id":"${posted.id}"}""")
            }
            successCount.incrementAndGet()
        } catch (conflict: ConcurrencyConflictException) {
            conflictCount.incrementAndGet()
        } catch (other: Throwable) {
            unexpected.compareAndSet(null, other)
        }
    }

    private fun body(reference: String) = """{"reference":"$reference","currency":"USD"}"""

    private fun command(
        reference: String,
        debit: AccountId,
        credit: AccountId,
        requestHash: String,
    ) = PostTransactionCommand(
        reference = reference,
        description = null,
        currency = Currency.USD,
        entries =
            listOf(
                EntryLine(debit, EntryDirection.DEBIT, BigDecimal(AMOUNT)),
                EntryLine(credit, EntryDirection.CREDIT, BigDecimal(AMOUNT).negate()),
            ),
        actor = ACTOR,
        correlationId = null,
        requestHash = requestHash,
    )

    companion object {
        const val POSTERS = 100
        const val ACTOR = "auth0|concurrency-actor"
        const val AMOUNT = "100.00"
        const val CREATED = 201

        @JvmStatic
        @DynamicPropertySource
        fun datasourceProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { PostgresContainerExtension.jdbcUrl }
            registry.add("spring.datasource.username") { PostgresContainerExtension.username }
            registry.add("spring.datasource.password") { PostgresContainerExtension.password }
            registry.add("spring.jpa.hibernate.ddl-auto") { "none" }
            registry.add("spring.datasource.hikari.maximum-pool-size") { "16" }
            registry.add("spring.datasource.hikari.connection-timeout") { "30000" }
        }
    }
}
