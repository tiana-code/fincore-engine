// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fincore.core.Currency
import com.fincore.core.IdempotencyKey
import com.fincore.ledger.config.IdempotencyProperties
import com.fincore.ledger.domain.enum.AccountStatus
import com.fincore.ledger.domain.enum.AccountType
import com.fincore.ledger.domain.exception.IdempotencyConflictException
import com.fincore.ledger.infrastructure.audit.AuditTrailWriterImpl
import com.fincore.ledger.infrastructure.persistence.AccountBalanceEntity
import com.fincore.ledger.infrastructure.persistence.AccountBalanceKey
import com.fincore.ledger.infrastructure.persistence.AccountBalanceRepository
import com.fincore.ledger.infrastructure.persistence.AccountPersistenceAdapter
import com.fincore.test.containers.PostgresContainerExtension
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
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
import java.math.BigDecimal
import java.time.Instant

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ExtendWith(PostgresContainerExtension::class)
@EnableConfigurationProperties(IdempotencyProperties::class)
@Import(
    AccountServiceImpl::class,
    AccountPersistenceAdapter::class,
    IdempotencyServiceImpl::class,
    IdempotencyStore::class,
    AuditTrailWriterImpl::class,
    AccountIdempotencyServiceIT.JacksonConfig::class,
)
class AccountIdempotencyServiceIT(
    @Autowired private val accountService: AccountService,
    @Autowired private val idempotencyService: IdempotencyService,
    @Autowired private val balanceRepository: AccountBalanceRepository,
) {
    @TestConfiguration
    class JacksonConfig {
        @Bean
        fun objectMapper(): ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
    }

    @Test
    fun `should create and read back an account`() {
        val created = accountService.create(CreateAccountCommand("Wallet", AccountType.USER_WALLET, Currency.USD, "auth0|op"))

        val loaded = accountService.get(created.id)
        loaded.name shouldBe "Wallet"
        loaded.currency shouldBe Currency.USD
        loaded.status shouldBe AccountStatus.ACTIVE
    }

    @Test
    fun `should replay an idempotent action without re-running it`() {
        val key = IdempotencyKey.of("k".repeat(40))
        var runs = 0

        val first =
            idempotencyService.execute(key, "{\"a\":1}") {
                runs++
                StoredResponse(201, "{\"id\":\"x\"}")
            }
        val second =
            idempotencyService.execute(key, "{\"a\":1}") {
                runs++
                StoredResponse(500, "should not run")
            }

        runs shouldBe 1
        first.replayed shouldBe false
        second.replayed shouldBe true
        second.statusCode shouldBe 201
    }

    @Test
    fun `should conflict when the same key sees a different request`() {
        val key = IdempotencyKey.of("c".repeat(40))
        idempotencyService.execute(key, "{\"a\":1}") { StoredResponse(201, "{}") }

        shouldThrow<IdempotencyConflictException> {
            idempotencyService.execute(key, "{\"a\":2}") { StoredResponse(201, "{}") }
        }
    }

    @Test
    fun `should refuse to close an account holding a non-zero balance`() {
        val created = accountService.create(CreateAccountCommand("Wallet", AccountType.USER_WALLET, Currency.USD, "op"))
        balanceRepository.saveAndFlush(
            AccountBalanceEntity(AccountBalanceKey(created.id.value, "USD"), BigDecimal("10.50"), Instant.now(), 0),
        )

        shouldThrow<com.fincore.ledger.domain.exception.DomainException> {
            accountService.changeStatus(created.id, AccountStatus.CLOSED, "op")
        }
    }

    @Test
    fun `should close an empty account`() {
        val created = accountService.create(CreateAccountCommand("Wallet", AccountType.USER_WALLET, Currency.USD, "op"))

        val closed = accountService.changeStatus(created.id, AccountStatus.CLOSED, "op")

        closed.status shouldBe AccountStatus.CLOSED
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
