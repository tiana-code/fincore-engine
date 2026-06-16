// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.api

import com.fincore.core.Currency
import com.fincore.ledger.application.AccountService
import com.fincore.ledger.application.CreateAccountCommand
import com.fincore.ledger.application.EntryLine
import com.fincore.ledger.application.PostTransactionCommand
import com.fincore.ledger.application.TransactionService
import com.fincore.ledger.domain.enum.AccountType
import com.fincore.ledger.domain.enum.EntryDirection
import com.fincore.ledger.infrastructure.persistence.OutboxEventRepository
import com.fincore.test.containers.PostgresContainerExtension
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.micrometer.tracing.Tracer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.core.env.Environment
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureObservability
@ExtendWith(PostgresContainerExtension::class)
@Import(ObservabilityIT.TestSecurity::class)
class ObservabilityIT(
    @Autowired private val rest: TestRestTemplate,
    @Autowired private val accountService: AccountService,
    @Autowired private val transactionService: TransactionService,
    @Autowired private val outboxRepository: OutboxEventRepository,
    @Autowired private val tracer: Tracer,
    @Autowired private val environment: Environment,
) {
    @TestConfiguration
    class TestSecurity {
        @Bean
        fun jwtDecoder(): JwtDecoder =
            JwtDecoder { token ->
                Jwt
                    .withTokenValue(token)
                    .header("alg", "none")
                    .subject("observability-it")
                    .claim("scope", "ledger:read ledger:write")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(EXPIRY_SECONDS))
                    .build()
            }
    }

    @AfterEach
    fun cleanOutbox() {
        outboxRepository.deleteAll()
    }

    @Test
    fun `should expose the prometheus exposition without a token`() {
        val response = rest.getForEntity("/actuator/prometheus", String::class.java)

        response.statusCode.value() shouldBe OK
        (response.body ?: "") shouldContain "# TYPE"
    }

    @Test
    fun `should report the posted counter after a transaction is posted`() {
        postBalanced()

        val body = rest.getForEntity("/actuator/prometheus", String::class.java).body ?: ""

        body shouldContain "ledger_transactions_posted_total"
        postedCount(body) shouldBeGreaterThanOrEqual 1.0
    }

    @Test
    fun `should expose a tracer bean and bind the tracing properties`() {
        tracer.shouldNotBeNull()
        environment.getProperty("management.tracing.sampling.probability").shouldNotBeNull()
        (environment.getProperty("management.otlp.tracing.endpoint") ?: "") shouldContain "/v1/traces"
    }

    private fun postedCount(exposition: String): Double =
        POSTED_PATTERN
            .find(exposition)
            ?.groupValues
            ?.get(1)
            ?.toDouble() ?: 0.0

    private fun postBalanced() {
        val debit = accountService.create(CreateAccountCommand("obs-${UUID.randomUUID()}", AccountType.USER_WALLET, Currency.EUR, ACTOR))
        val credit = accountService.create(CreateAccountCommand("obs-${UUID.randomUUID()}", AccountType.USER_WALLET, Currency.EUR, ACTOR))
        transactionService.post(
            PostTransactionCommand(
                reference = "obs-${UUID.randomUUID()}",
                description = null,
                currency = Currency.EUR,
                entries =
                    listOf(
                        EntryLine(debit.id, EntryDirection.DEBIT, BigDecimal(AMOUNT)),
                        EntryLine(credit.id, EntryDirection.CREDIT, BigDecimal(AMOUNT_NEG)),
                    ),
                actor = ACTOR,
                correlationId = UUID.randomUUID().toString(),
            ),
        )
    }

    private companion object {
        const val EXPIRY_SECONDS = 3600L
        const val OK = 200
        const val ACTOR = "observability-it"
        const val AMOUNT = "100.00"
        const val AMOUNT_NEG = "-100.00"
        val POSTED_PATTERN = Regex("""ledger_transactions_posted_total\{[^}]*type="post"[^}]*}\s+([0-9.eE+]+)""")

        @JvmStatic
        @DynamicPropertySource
        fun datasourceProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { PostgresContainerExtension.jdbcUrl }
            registry.add("spring.datasource.username") { PostgresContainerExtension.username }
            registry.add("spring.datasource.password") { PostgresContainerExtension.password }
            registry.add("spring.datasource.hikari.maximum-pool-size") { "2" }
            registry.add("spring.jpa.hibernate.ddl-auto") { "none" }
        }
    }
}
