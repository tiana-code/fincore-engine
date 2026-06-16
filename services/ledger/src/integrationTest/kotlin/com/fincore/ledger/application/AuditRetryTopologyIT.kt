// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fincore.core.Currency
import com.fincore.core.IdempotencyKey
import com.fincore.ledger.config.IdempotencyProperties
import com.fincore.ledger.domain.enum.AccountType
import com.fincore.ledger.domain.enum.AuditAction
import com.fincore.ledger.domain.enum.EntryDirection
import com.fincore.ledger.domain.enum.TransactionStatus
import com.fincore.ledger.domain.exception.ConcurrencyConflictException
import com.fincore.ledger.infrastructure.audit.AuditTrailWriterImpl
import com.fincore.ledger.infrastructure.outbox.OutboxEventPublisherImpl
import com.fincore.ledger.infrastructure.persistence.AccountPersistenceAdapter
import com.fincore.ledger.infrastructure.persistence.AuditEventRepository
import com.fincore.ledger.infrastructure.persistence.IdempotencyKeyRepository
import com.fincore.ledger.infrastructure.persistence.OutboxEventRepository
import com.fincore.ledger.infrastructure.persistence.TransactionPersistenceAdapter
import com.fincore.ledger.infrastructure.persistence.TransactionRepository
import com.fincore.test.containers.PostgresContainerExtension
import io.kotest.assertions.throwables.shouldThrow
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
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicInteger

/**
 * Drives the real nesting idempotencyService.execute -> IdempotencyStore.runOrReplay (@Transactional)
 * -> action lambda -> TransactionServiceImpl, and asserts atomicity and audit invariants.
 *
 * Contended-path tests (AC-1..AC-4) use @Transactional(propagation = NOT_SUPPORTED) at the class
 * level so no ambient test tx wraps runOrReplay. Each runOrReplay call opens its own standalone,
 * committing physical TX (INV-6). This is the proven pattern from OutboxEventPublisherIT.
 *
 * The forced OLF is injected at the action-lambda level: the lambda throws
 * OptimisticLockingFailureException on the first invocation, then runs normally on subsequent
 * invocations. This simulates the real OLF that the poster would throw on a @Version conflict:
 * - On current broken code: OLF propagates out of runOrReplay (TX rolls back) and out of execute
 *   with no retry -> test fails as RED.
 * - After fix: execute catches OLF, retries runOrReplay in a fresh TX -> test passes as GREEN.
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
    AuditRetryTopologyIT.JacksonConfig::class,
    MetricsTestConfig::class,
)
class AuditRetryTopologyIT(
    @Autowired private val accountService: AccountService,
    @Autowired private val transactionService: TransactionService,
    @Autowired private val idempotencyService: IdempotencyService,
    @Autowired private val auditRepository: AuditEventRepository,
    @Autowired private val transactionRepository: TransactionRepository,
    @Autowired private val idempotencyKeyRepository: IdempotencyKeyRepository,
    @Autowired private val outboxRepository: OutboxEventRepository,
) {
    @TestConfiguration
    class JacksonConfig {
        @Bean
        fun objectMapper(): ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
    }

    // These tests commit (NOT_SUPPORTED), so committed outbox rows would leak into the shared
    // container and inflate the global-count assertions in other committing ITs. Audit rows cannot be
    // deleted (append-only trigger 018); other ITs scope audit by unique resourceId, so leaving them is safe.
    @AfterEach
    fun cleanOutbox() {
        outboxRepository.deleteAll()
    }

    private fun newAccount(): com.fincore.ledger.domain.Account =
        accountService.create(CreateAccountCommand("Topology Account", AccountType.USER_WALLET, Currency.USD, ACTOR))

    // AC-5 (regression guard): happy path, no forced conflict - must stay green
    @Test
    fun `should write exactly one TRANSACTION_POST audit row when post runs through the idempotency path`() {
        val debit = newAccount()
        val credit = newAccount()
        val key = IdempotencyKey.of("retry-topo-happy-00000000000000000000000".take(40))
        val body = """{"reference":"ref-topo-happy","currency":"USD"}"""
        var postedId = ""

        idempotencyService.execute(key, body) { hash ->
            val posted =
                transactionService.post(
                    PostTransactionCommand(
                        reference = "ref-topo-happy",
                        description = null,
                        currency = Currency.USD,
                        entries =
                            listOf(
                                EntryLine(debit.id, EntryDirection.DEBIT, BigDecimal("100.00")),
                                EntryLine(credit.id, EntryDirection.CREDIT, BigDecimal("-100.00")),
                            ),
                        actor = ACTOR,
                        correlationId = CORR_ID,
                        requestHash = hash,
                    ),
                )
            postedId = posted.id.toString()
            StoredResponse(201, """{"id":"${posted.id}"}""")
        }

        auditRepository.findAll().filter { it.resourceId == postedId && it.action == AuditAction.TRANSACTION_POST.name }.size shouldBe 1
        transactionRepository.findAll().filter { it.reference == "ref-topo-happy" }.size shouldBe 1
    }

    // AC-1 / AC-2: action throws OLF on first invocation; execute must retry and succeed on second.
    // On current broken code: OLF propagates out of execute uncaught -> test fails (RED).
    // After fix: execute catches OLF, re-invokes runOrReplay in fresh TX -> exactly 1 business row + 1 audit row.
    @Test
    fun `should write exactly one audit row when post succeeds after one optimistic retry`() {
        val debit = newAccount()
        val credit = newAccount()
        val ref = "ref-retry-once-${uniqueSuffix()}"
        val key = idempotencyKey("retry-once-${uniqueSuffix()}")
        val body = """{"reference":"$ref","currency":"USD"}"""
        var postedId = ""
        val actionCallCount = AtomicInteger(0)

        idempotencyService.execute(key, body) { hash ->
            val callIndex = actionCallCount.incrementAndGet()
            // first invocation simulates the OLF that poster would throw on a @Version conflict
            if (callIndex == 1) throw OptimisticLockingFailureException("forced OLF on attempt 1")
            val posted =
                transactionService.post(
                    PostTransactionCommand(
                        reference = ref,
                        description = null,
                        currency = Currency.USD,
                        entries =
                            listOf(
                                EntryLine(debit.id, EntryDirection.DEBIT, BigDecimal("50.00")),
                                EntryLine(credit.id, EntryDirection.CREDIT, BigDecimal("-50.00")),
                            ),
                        actor = ACTOR,
                        correlationId = null,
                        requestHash = hash,
                    ),
                )
            postedId = posted.id.toString()
            StoredResponse(201, """{"id":"${posted.id}"}""")
        }

        // AC-1: exactly one business row and one audit row (filter by unique resourceId - not global count)
        auditRepository
            .findAll()
            .filter { it.resourceId == postedId && it.action == AuditAction.TRANSACTION_POST.name }
            .size shouldBe 1
        transactionRepository.findAll().filter { it.reference == ref }.size shouldBe 1
        // AC-2: action was called exactly twice (first OLF, second success)
        actionCallCount.get() shouldBe 2
    }

    // AC-3: every attempt writes a business row + audit row then the action throws OLF, so each TX#k
    // rolls back wholesale. After MAX_ATTEMPTS the ConcurrencyConflictException surfaces and nothing
    // survives: zero business rows, zero audit rows (proves audit-business rollback atomicity, FR-5),
    // no committed idempotency reservation. failActor is a unique handle safe under parallel forks.
    @Test
    fun `should leave zero business rows and zero audit rows when optimistic conflict forces full rollback`() {
        val debit = newAccount()
        val credit = newAccount()
        val ref = "ref-full-fail-${uniqueSuffix()}"
        val failActor = "auth0|full-fail-${uniqueSuffix()}"
        val key = idempotencyKey("full-fail-${uniqueSuffix()}")
        val body = """{"reference":"$ref","currency":"USD"}"""
        val keyHash = sha256Hex(key.value)

        shouldThrow<ConcurrencyConflictException> {
            idempotencyService.execute(key, body) { hash ->
                transactionService.post(
                    PostTransactionCommand(
                        reference = ref,
                        description = null,
                        currency = Currency.USD,
                        entries =
                            listOf(
                                EntryLine(debit.id, EntryDirection.DEBIT, BigDecimal("25.00")),
                                EntryLine(credit.id, EntryDirection.CREDIT, BigDecimal("-25.00")),
                            ),
                        actor = failActor,
                        correlationId = null,
                        requestHash = hash,
                    ),
                )
                throw OptimisticLockingFailureException("forced OLF after post on every attempt")
            }
        }

        transactionRepository.findAll().filter { it.reference == ref }.size shouldBe 0
        auditRepository.findAll().filter { it.actorId == failActor }.size shouldBe 0
        idempotencyKeyRepository.findById(keyHash).isPresent shouldBe false
    }

    // AC-4: reverse path with forced first-attempt OLF on the action lambda -> succeeds on retry.
    // The action lambda throws OLF on first call (simulating poster's @Version conflict on reversal).
    // On current broken code: OLF propagates out of execute uncaught -> test fails (RED).
    @Test
    fun `should reverse on retry when first action invocation hits an optimistic lock`() {
        val debit = newAccount()
        val credit = newAccount()
        val ref = "ref-rev-retry-${uniqueSuffix()}"
        val postKey = idempotencyKey("post-for-rev-${uniqueSuffix()}")
        val postBody = """{"reference":"$ref","currency":"USD"}"""
        var originalId: com.fincore.core.TransactionId? = null

        // post the original first (no conflict)
        idempotencyService.execute(postKey, postBody) { hash ->
            val posted =
                transactionService.post(
                    PostTransactionCommand(
                        reference = ref,
                        description = null,
                        currency = Currency.USD,
                        entries =
                            listOf(
                                EntryLine(debit.id, EntryDirection.DEBIT, BigDecimal("75.00")),
                                EntryLine(credit.id, EntryDirection.CREDIT, BigDecimal("-75.00")),
                            ),
                        actor = ACTOR,
                        correlationId = null,
                        requestHash = hash,
                    ),
                )
            originalId = posted.id
            StoredResponse(201, """{"id":"${posted.id}"}""")
        }

        val txId = checkNotNull(originalId) { "original post must succeed" }
        val revKey = idempotencyKey("reversal-${uniqueSuffix()}")
        val revBody = """{"reversalOf":"$txId"}"""
        val reversalCallCount = AtomicInteger(0)

        idempotencyService.execute(revKey, revBody) { hash ->
            val callIndex = reversalCallCount.incrementAndGet()
            // first invocation simulates the OLF that postReversal would throw on @Version conflict
            if (callIndex == 1) throw OptimisticLockingFailureException("forced OLF on reversal attempt 1")
            val reversed = transactionService.reverse(txId, ACTOR, null, "test reversal", hash)
            StoredResponse(201, """{"id":"${reversed.id}"}""")
        }

        // AC-4: exactly one TRANSACTION_REVERSE audit row for the original transaction id
        auditRepository
            .findAll()
            .filter { it.resourceId == txId.toString() && it.action == AuditAction.TRANSACTION_REVERSE.name }
            .size shouldBe 1
        // AC-4: original marked REVERSED
        transactionRepository.findById(txId.value).orElse(null)?.status shouldBe TransactionStatus.REVERSED
        // AC-4: action was called exactly twice
        reversalCallCount.get() shouldBe 2
    }

    companion object {
        const val ACTOR = "auth0|topology-actor"
        const val CORR_ID = "corr-retry-topo-001"

        @JvmStatic
        @DynamicPropertySource
        fun datasourceProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { PostgresContainerExtension.jdbcUrl }
            registry.add("spring.datasource.username") { PostgresContainerExtension.username }
            registry.add("spring.datasource.password") { PostgresContainerExtension.password }
            registry.add("spring.jpa.hibernate.ddl-auto") { "none" }
        }

        private val suffixCounter = AtomicInteger(0)

        fun uniqueSuffix(): String = suffixCounter.incrementAndGet().toString().padStart(4, '0')

        fun idempotencyKey(seed: String): IdempotencyKey {
            val padded = seed.padEnd(40, '0').take(40)
            return IdempotencyKey.of(padded)
        }

        fun sha256Hex(value: String): String =
            MessageDigest
                .getInstance("SHA-256")
                .digest(value.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
    }
}
