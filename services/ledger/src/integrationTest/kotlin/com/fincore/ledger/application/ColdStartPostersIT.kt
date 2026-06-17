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
import com.fincore.ledger.infrastructure.persistence.AccountBalanceRepository
import com.fincore.ledger.infrastructure.persistence.AccountPersistenceAdapter
import com.fincore.ledger.infrastructure.persistence.EntryRepository
import com.fincore.ledger.infrastructure.persistence.OutboxEventRepository
import com.fincore.ledger.infrastructure.persistence.TransactionPersistenceAdapter
import com.fincore.ledger.infrastructure.persistence.TransactionRepository
import com.fincore.test.containers.PostgresContainerExtension
import io.kotest.matchers.shouldBe
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
    ColdStartPostersIT.JacksonConfig::class,
    MetricsTestConfig::class,
)
class ColdStartPostersIT(
    @Autowired private val accountService: AccountService,
    @Autowired private val transactionService: TransactionService,
    @Autowired private val idempotencyService: IdempotencyService,
    @Autowired private val balanceRepository: AccountBalanceRepository,
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
        accountService.create(CreateAccountCommand("Cold Start Account", AccountType.USER_WALLET, Currency.USD, ACTOR))

    @Test
    fun `should resolve every cold start poster without mislabel or lost update when contending on a fresh pair`() {
        val debit = newAccount()
        val credit = newAccount()
        val prefix = "ref-cold-${UUID.randomUUID()}"
        val outcome = postConcurrently(debit.id, credit.id, prefix)

        outcome.unexpected.get()?.let { throw AssertionError("a poster surfaced an unexpected throwable: ${it::class.qualifiedName}", it) }
        outcome.success.get() shouldBe POSTERS
        outcome.conflict.get() shouldBe 0
        val expectedDebit = BigDecimal(AMOUNT).multiply(BigDecimal(POSTERS))
        balanceRepository
            .findByKeyAccountId(debit.id.value)
            .single()
            .balance
            .compareTo(expectedDebit) shouldBe 0
        balanceRepository
            .findByKeyAccountId(credit.id.value)
            .single()
            .balance
            .compareTo(expectedDebit.negate()) shouldBe 0
        transactionRepository.findAll().count { it.reference.startsWith(prefix) } shouldBe POSTERS
        entryRepository.findAll().count { it.accountId == debit.id.value || it.accountId == credit.id.value } shouldBe 2 * POSTERS
    }

    @Test
    fun `should insert then increment the balance row version when posting twice to a fresh pair`() {
        val debit = newAccount()
        val credit = newAccount()
        post(debit.id, credit.id, "ref-seq-${UUID.randomUUID()}-1")
        post(debit.id, credit.id, "ref-seq-${UUID.randomUUID()}-2")
        val row = balanceRepository.findByKeyAccountId(debit.id.value).single()
        row.version shouldBe 1
        row.balance.compareTo(BigDecimal(AMOUNT).multiply(BigDecimal(2))) shouldBe 0
    }

    private fun postConcurrently(
        debit: AccountId,
        credit: AccountId,
        prefix: String,
    ): PosterOutcome {
        val outcome = PosterOutcome()
        val startGate = CountDownLatch(1)
        Executors.newVirtualThreadPerTaskExecutor().use { executor ->
            val futures =
                (0 until POSTERS).map { index ->
                    executor.submit(
                        Callable {
                            startGate.await()
                            runPoster(debit, credit, "$prefix-$index", outcome)
                        },
                    )
                }
            startGate.countDown()
            futures.forEach { it.get() }
        }
        return outcome
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun runPoster(
        debit: AccountId,
        credit: AccountId,
        reference: String,
        outcome: PosterOutcome,
    ) {
        try {
            post(debit, credit, reference)
            outcome.success.incrementAndGet()
        } catch (conflict: ConcurrencyConflictException) {
            outcome.conflict.incrementAndGet()
        } catch (other: Throwable) {
            outcome.unexpected.compareAndSet(null, other)
        }
    }

    private fun post(
        debit: AccountId,
        credit: AccountId,
        reference: String,
    ) {
        idempotencyService.execute(IdempotencyKey.generate(), body(reference)) { hash ->
            val posted = transactionService.post(command(reference, debit, credit, hash))
            StoredResponse(CREATED, """{"id":"${posted.id}"}""")
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

    private class PosterOutcome {
        val success = AtomicInteger(0)
        val conflict = AtomicInteger(0)
        val unexpected = AtomicReference<Throwable?>(null)
    }

    companion object {
        const val POSTERS = 100
        const val ACTOR = "auth0|cold-start-actor"
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
