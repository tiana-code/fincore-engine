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
import com.fincore.ledger.infrastructure.outbox.OutboxEventPublisherImpl
import com.fincore.ledger.infrastructure.persistence.AccountPersistenceAdapter
import com.fincore.ledger.infrastructure.persistence.TransactionPersistenceAdapter
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
import java.time.Instant

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ExtendWith(PostgresContainerExtension::class)
@Import(
    EntryQueryServiceImpl::class,
    TransactionServiceImpl::class,
    TransactionPoster::class,
    TransactionPersistenceAdapter::class,
    BalanceServiceImpl::class,
    AccountServiceImpl::class,
    AccountPersistenceAdapter::class,
    AccountEntriesIT.JacksonConfig::class,
    OutboxEventPublisherImpl::class,
)
class AccountEntriesIT(
    @Autowired private val entryQueryService: EntryQueryService,
    @Autowired private val transactionService: TransactionService,
    @Autowired private val accountService: AccountService,
) {
    @TestConfiguration
    class JacksonConfig {
        @Bean
        fun objectMapper(): ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
    }

    private fun newAccount() = accountService.create(CreateAccountCommand("Account", AccountType.USER_WALLET, Currency.USD, "op"))

    private fun post(
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
    fun `should page account entries newest first with an opaque cursor`() {
        val account = newAccount()
        val other = newAccount()
        post("ref-e1", account.id, other.id)
        post("ref-e2", account.id, other.id)
        post("ref-e3", account.id, other.id)

        val firstPage = entryQueryService.listAccountEntries(account.id, null, null, null, 2)
        firstPage.items.size shouldBe 2
        (firstPage.nextCursor != null) shouldBe true

        val secondPage = entryQueryService.listAccountEntries(account.id, null, null, firstPage.nextCursor, 2)
        secondPage.items.size shouldBe 1
        secondPage.nextCursor shouldBe null

        val firstIds = firstPage.items.map { it.id }.toSet()
        val secondIds = secondPage.items.map { it.id }.toSet()
        firstIds.intersect(secondIds).isEmpty() shouldBe true
    }

    @Test
    fun `should only return the debit leg for the queried account`() {
        val account = newAccount()
        val other = newAccount()
        post("ref-e4", account.id, other.id)

        val page = entryQueryService.listAccountEntries(account.id, null, null, null, 50)

        page.items.size shouldBe 1
        page.items.single().direction shouldBe EntryDirection.DEBIT
        page.items
            .single()
            .amount
            .compareTo(BigDecimal("100.00")) shouldBe 0
    }

    @Test
    fun `should exclude entries outside the window`() {
        val account = newAccount()
        val other = newAccount()
        post("ref-e5", account.id, other.id)

        val future = Instant.now().plusSeconds(3600)
        val page = entryQueryService.listAccountEntries(account.id, future, future.plusSeconds(3600), null, 50)

        page.items.size shouldBe 0
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
