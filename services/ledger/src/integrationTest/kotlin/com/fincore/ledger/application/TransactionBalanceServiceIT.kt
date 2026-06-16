// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fincore.core.Currency
import com.fincore.ledger.domain.enum.AccountStatus
import com.fincore.ledger.domain.enum.AccountType
import com.fincore.ledger.domain.enum.EntryDirection
import com.fincore.ledger.domain.exception.DomainException
import com.fincore.ledger.domain.exception.DoubleEntryViolationException
import com.fincore.ledger.domain.exception.DuplicateTransactionException
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
import java.time.Instant

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
    TransactionBalanceServiceIT.JacksonConfig::class,
    OutboxEventPublisherImpl::class,
    AuditTrailWriterImpl::class,
    MetricsTestConfig::class,
)
class TransactionBalanceServiceIT(
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

    private fun newAccount(currency: Currency = Currency.USD) =
        accountService.create(CreateAccountCommand("Account", AccountType.USER_WALLET, currency, "op"))

    private fun command(
        reference: String,
        debitAccount: com.fincore.core.AccountId,
        creditAccount: com.fincore.core.AccountId,
        currency: Currency = Currency.USD,
        debit: BigDecimal = BigDecimal("100.00"),
        credit: BigDecimal = BigDecimal("-100.00"),
    ) = PostTransactionCommand(
        reference = reference,
        description = null,
        currency = currency,
        entries =
            listOf(
                EntryLine(debitAccount, EntryDirection.DEBIT, debit),
                EntryLine(creditAccount, EntryDirection.CREDIT, credit),
            ),
        actor = "op",
        correlationId = "corr-1",
    )

    @Test
    fun `should post a balanced transaction and update both balances and the outbox`() {
        val debit = newAccount()
        val credit = newAccount()

        transactionService.post(command("ref-1", debit.id, credit.id))

        balanceService
            .current(debit.id, Currency.USD)
            .amount.amount
            .compareTo(BigDecimal("100.00")) shouldBe 0
        balanceService
            .current(credit.id, Currency.USD)
            .amount.amount
            .compareTo(BigDecimal("-100.00")) shouldBe 0
        val events = outboxRepository.findAll()
        events.size shouldBe 1
        events.first().eventType shouldBe "com.fincore.ledger.transaction.posted.v1"
    }

    @Test
    fun `should reject an unbalanced transaction`() {
        val debit = newAccount()
        val credit = newAccount()

        shouldThrow<DoubleEntryViolationException> {
            transactionService.post(command("ref-2", debit.id, credit.id, credit = BigDecimal("-90.00")))
        }
        outboxRepository.findAll().size shouldBe 0
    }

    @Test
    fun `should reject posting to a frozen account`() {
        val debit = newAccount()
        val credit = newAccount()
        accountService.changeStatus(debit.id, AccountStatus.FROZEN, "op")

        shouldThrow<DomainException> { transactionService.post(command("ref-3", debit.id, credit.id)) }
    }

    @Test
    fun `should reject a currency mismatch`() {
        val debit = newAccount(Currency.USD)
        val credit = newAccount(Currency.USD)

        shouldThrow<DomainException> {
            transactionService.post(command("ref-4", debit.id, credit.id, currency = Currency.EUR))
        }
    }

    @Test
    fun `should reject a duplicate reference`() {
        val debit = newAccount()
        val credit = newAccount()
        transactionService.post(command("ref-5", debit.id, credit.id))

        shouldThrow<DuplicateTransactionException> {
            transactionService.post(command("ref-5", debit.id, credit.id))
        }
    }

    @Test
    fun `should time-travel the balance with asOf`() {
        val debit = newAccount()
        val credit = newAccount()
        transactionService.post(command("ref-6", debit.id, credit.id))

        balanceService
            .asOf(debit.id, Currency.USD, Instant.now().plusSeconds(3600))
            .amount.amount
            .compareTo(BigDecimal("100.00")) shouldBe 0
        balanceService
            .asOf(debit.id, Currency.USD, Instant.parse("2020-01-01T00:00:00Z"))
            .amount
            .isZero() shouldBe true
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
