// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fincore.core.AccountId
import com.fincore.core.Currency
import com.fincore.ledger.domain.enum.AccountType
import com.fincore.ledger.domain.enum.EntryDirection
import com.fincore.ledger.domain.enum.TransactionStatus
import com.fincore.ledger.domain.exception.TransactionAlreadyReversedException
import com.fincore.ledger.infrastructure.audit.AuditTrailWriterImpl
import com.fincore.ledger.infrastructure.outbox.OutboxEventPublisherImpl
import com.fincore.ledger.infrastructure.persistence.AccountPersistenceAdapter
import com.fincore.ledger.infrastructure.persistence.OutboxEventRepository
import com.fincore.ledger.infrastructure.persistence.TransactionPersistenceAdapter
import com.fincore.test.containers.PostgresContainerExtension
import io.kotest.assertions.throwables.shouldThrow
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

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ExtendWith(PostgresContainerExtension::class)
@Import(
    TransactionServiceImpl::class,
    TransactionPoster::class,
    TransactionPersistenceAdapter::class,
    BalanceServiceImpl::class,
    AccountServiceImpl::class,
    AccountPersistenceAdapter::class,
    TransactionReversalIT.JacksonConfig::class,
    OutboxEventPublisherImpl::class,
    AuditTrailWriterImpl::class,
)
class TransactionReversalIT(
    @Autowired private val transactionService: TransactionService,
    @Autowired private val balanceService: BalanceService,
    @Autowired private val accountService: AccountService,
    @Autowired private val outboxRepository: OutboxEventRepository,
) {
    @TestConfiguration
    class JacksonConfig {
        @Bean
        fun objectMapper(): ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
    }

    private fun newAccount() = accountService.create(CreateAccountCommand("Account", AccountType.USER_WALLET, Currency.USD, "op"))

    private fun postBalanced(
        reference: String,
        debit: AccountId,
        credit: AccountId,
    ) = transactionService.post(
        PostTransactionCommand(
            reference = reference,
            description = null,
            currency = Currency.USD,
            entries =
                listOf(
                    EntryLine(debit, EntryDirection.DEBIT, BigDecimal("100.00")),
                    EntryLine(credit, EntryDirection.CREDIT, BigDecimal("-100.00")),
                ),
            actor = "op",
            correlationId = "corr-1",
        ),
    )

    @Test
    fun `should reverse a posted transaction and return both balances to zero`() {
        val debit = newAccount()
        val credit = newAccount()
        val original = postBalanced("ref-rev-1", debit.id, credit.id)

        transactionService.reverse(original.id, "op", "corr-2", null, null)

        balanceService.current(debit.id, Currency.USD).amount.isZero() shouldBe true
        balanceService.current(credit.id, Currency.USD).amount.isZero() shouldBe true
    }

    @Test
    fun `should mark the original reversed and link the compensating transaction`() {
        val debit = newAccount()
        val credit = newAccount()
        val original = postBalanced("ref-rev-2", debit.id, credit.id)

        val compensating = transactionService.reverse(original.id, "op", "corr-2", null, null)

        transactionService.get(original.id).status shouldBe TransactionStatus.REVERSED
        val detail = transactionService.get(compensating.id)
        detail.reversesId shouldBe original.id
        detail.status shouldBe TransactionStatus.POSTED
        detail.entries.size shouldBe 2
    }

    @Test
    fun `should reject reversing the same transaction twice`() {
        val debit = newAccount()
        val credit = newAccount()
        val original = postBalanced("ref-rev-3", debit.id, credit.id)
        transactionService.reverse(original.id, "op", "corr-2", null, null)

        shouldThrow<TransactionAlreadyReversedException> {
            transactionService.reverse(original.id, "op", "corr-3", null, null)
        }
    }

    @Test
    fun `should emit an outbox event for the compensating transaction`() {
        val debit = newAccount()
        val credit = newAccount()
        val original = postBalanced("ref-rev-4", debit.id, credit.id)

        transactionService.reverse(original.id, "op", "corr-2", null, null)

        outboxRepository.findAll().size shouldBe 2
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
