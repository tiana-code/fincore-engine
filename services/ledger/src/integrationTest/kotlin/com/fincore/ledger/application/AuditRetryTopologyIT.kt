// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fincore.core.Currency
import com.fincore.core.IdempotencyKey
import com.fincore.ledger.domain.enum.AccountType
import com.fincore.ledger.domain.enum.AuditAction
import com.fincore.ledger.domain.enum.EntryDirection
import com.fincore.ledger.infrastructure.audit.AuditTrailWriterImpl
import com.fincore.ledger.infrastructure.outbox.OutboxEventPublisherImpl
import com.fincore.ledger.infrastructure.persistence.AccountPersistenceAdapter
import com.fincore.ledger.infrastructure.persistence.AuditEventRepository
import com.fincore.ledger.infrastructure.persistence.TransactionPersistenceAdapter
import com.fincore.ledger.infrastructure.persistence.TransactionRepository
import com.fincore.test.containers.PostgresContainerExtension
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.math.BigDecimal

/**
 * Drives the real nesting idempotencyService.execute -> IdempotencyStore.runOrReplay (@Transactional)
 * -> withOptimisticRetry -> TransactionPoster.post, and asserts that posting through that chain writes
 * exactly one TRANSACTION_POST audit row together with the business write. The contended
 * optimistic-retry-inside-the-idempotency-transaction path is a pre-existing concern tracked in a
 * separate follow-up issue and is not exercised here.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ExtendWith(PostgresContainerExtension::class)
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
)
class AuditRetryTopologyIT(
    @Autowired private val accountService: AccountService,
    @Autowired private val transactionService: TransactionService,
    @Autowired private val idempotencyService: IdempotencyService,
    @Autowired private val auditRepository: AuditEventRepository,
    @Autowired private val transactionRepository: TransactionRepository,
) {
    @TestConfiguration
    class JacksonConfig {
        @Bean
        fun objectMapper(): ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
    }

    private fun newAccount(): com.fincore.ledger.domain.Account =
        accountService.create(CreateAccountCommand("Topology Account", AccountType.USER_WALLET, Currency.USD, ACTOR))

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
    }
}
