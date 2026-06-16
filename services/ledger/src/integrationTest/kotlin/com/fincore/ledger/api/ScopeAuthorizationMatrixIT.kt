// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.api

import com.fincore.core.AccountId
import com.fincore.core.Currency
import com.fincore.ledger.api.idempotency.IdempotencyAttributes
import com.fincore.ledger.api.observability.CorrelationIdAttributes
import com.fincore.ledger.application.AccountService
import com.fincore.ledger.application.CreateAccountCommand
import com.fincore.ledger.application.EntryLine
import com.fincore.ledger.application.PostTransactionCommand
import com.fincore.ledger.application.TransactionService
import com.fincore.ledger.domain.enum.AccountType
import com.fincore.ledger.domain.enum.EntryDirection
import com.fincore.ledger.infrastructure.persistence.OutboxEventRepository
import com.fincore.test.containers.PostgresContainerExtension
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(PostgresContainerExtension::class)
@Import(ScopeAuthorizationMatrixIT.ScopeFromTokenSecurity::class)
class ScopeAuthorizationMatrixIT(
    @Autowired private val rest: TestRestTemplate,
    @Autowired private val accountService: AccountService,
    @Autowired private val transactionService: TransactionService,
    @Autowired private val outboxRepository: OutboxEventRepository,
) {
    @TestConfiguration
    class ScopeFromTokenSecurity {
        @Bean
        fun jwtDecoder(): JwtDecoder =
            JwtDecoder { token ->
                Jwt
                    .withTokenValue(token)
                    .header("alg", "none")
                    .subject("scope-matrix-it")
                    .claim("scope", "ledger:$token")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(EXPIRY_SECONDS))
                    .build()
            }
    }

    private enum class Access { READ, WRITE }

    private data class Endpoint(
        val method: HttpMethod,
        val path: () -> String,
        val access: Access,
        val body: () -> String? = { null },
    )

    private lateinit var accountAId: String
    private lateinit var accountBId: String
    private lateinit var readTransactionId: String
    private lateinit var reverseTransactionId: String

    private val endpoints =
        listOf(
            Endpoint(HttpMethod.GET, { "/v1/accounts" }, Access.READ),
            Endpoint(HttpMethod.POST, { "/v1/accounts" }, Access.WRITE, { accountBody() }),
            Endpoint(HttpMethod.GET, { "/v1/accounts/$accountAId" }, Access.READ),
            Endpoint(HttpMethod.GET, { "/v1/accounts/$accountAId/balance" }, Access.READ),
            Endpoint(HttpMethod.GET, { "/v1/accounts/$accountAId/entries" }, Access.READ),
            Endpoint(HttpMethod.GET, { "/v1/transactions" }, Access.READ),
            Endpoint(HttpMethod.POST, { "/v1/transactions" }, Access.WRITE, { transactionBody() }),
            Endpoint(HttpMethod.GET, { "/v1/transactions/$readTransactionId" }, Access.READ),
            Endpoint(HttpMethod.POST, { "/v1/transactions/$reverseTransactionId/reverse" }, Access.WRITE),
        )

    @BeforeEach
    fun seed() {
        val debit = accountService.create(CreateAccountCommand("matrix-${nextSeq()}", AccountType.USER_WALLET, Currency.EUR, ACTOR))
        val credit = accountService.create(CreateAccountCommand("matrix-${nextSeq()}", AccountType.USER_WALLET, Currency.EUR, ACTOR))
        accountAId = debit.id.toString()
        accountBId = credit.id.toString()
        readTransactionId = postBalanced("matrix-read-${nextSeq()}", debit.id, credit.id)
        reverseTransactionId = postBalanced("matrix-rev-${nextSeq()}", debit.id, credit.id)
    }

    @AfterEach
    fun cleanOutbox() {
        outboxRepository.deleteAll()
    }

    @Test
    fun `should return 2xx for every endpoint when the token carries the matching scope`() {
        endpoints.forEach { endpoint ->
            val response = call(endpoint, endpoint.access)
            withClue("${endpoint.method} ${endpoint.path()} expected 2xx got ${response.statusCode.value()}") {
                (response.statusCode.value() in OK_MIN..OK_MAX) shouldBe true
            }
        }
    }

    @Test
    fun `should return 403 for every endpoint when the token carries only the opposite scope`() {
        endpoints.forEach { endpoint ->
            val opposite = if (endpoint.access == Access.READ) Access.WRITE else Access.READ
            val response = call(endpoint, opposite)
            withClue("${endpoint.method} ${endpoint.path()} with $opposite") {
                response.statusCode.value() shouldBe FORBIDDEN
            }
        }
    }

    @Test
    fun `should return 401 for every endpoint when no token is presented`() {
        endpoints.forEach { endpoint ->
            val response = call(endpoint, null)
            withClue("${endpoint.method} ${endpoint.path()} without a token") {
                response.statusCode.value() shouldBe UNAUTHORIZED
            }
        }
    }

    private fun call(
        endpoint: Endpoint,
        access: Access?,
    ): ResponseEntity<String> {
        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                if (access != null) setBearerAuth(bearerFor(access))
                set(CorrelationIdAttributes.HEADER, UUID.randomUUID().toString())
                if (endpoint.method == HttpMethod.POST) set(IdempotencyAttributes.HEADER, idemKey())
            }
        val body = endpoint.body() ?: if (endpoint.method == HttpMethod.POST) "{}" else null
        return rest.exchange(endpoint.path(), endpoint.method, HttpEntity(body, headers), String::class.java)
    }

    private fun accountBody(): String = """{"name":"matrix-${nextSeq()}","type":"USER_WALLET","currency":"EUR"}"""

    private fun transactionBody(): String =
        """{"reference":"matrix-${nextSeq()}","currency":"EUR","entries":[""" +
            """{"accountId":"$accountAId","direction":"DEBIT","amount":"$AMOUNT"},""" +
            """{"accountId":"$accountBId","direction":"CREDIT","amount":"$AMOUNT_NEG"}]}"""

    private fun postBalanced(
        reference: String,
        debit: AccountId,
        credit: AccountId,
    ): String =
        transactionService
            .post(
                PostTransactionCommand(
                    reference = reference,
                    description = null,
                    currency = Currency.EUR,
                    entries =
                        listOf(
                            EntryLine(debit, EntryDirection.DEBIT, BigDecimal(AMOUNT)),
                            EntryLine(credit, EntryDirection.CREDIT, BigDecimal(AMOUNT_NEG)),
                        ),
                    actor = ACTOR,
                    correlationId = UUID.randomUUID().toString(),
                ),
            ).id
            .toString()

    private fun bearerFor(access: Access): String = if (access == Access.READ) "read" else "write"

    private companion object {
        const val EXPIRY_SECONDS = 3600L
        const val KEY_LENGTH = 40
        const val FORBIDDEN = 403
        const val UNAUTHORIZED = 401
        const val OK_MIN = 200
        const val OK_MAX = 299
        const val ACTOR = "scope-matrix-it"
        const val AMOUNT = "100.00"
        const val AMOUNT_NEG = "-100.00"
        private var counter = 0

        fun nextSeq(): Int = ++counter

        fun idemKey(): String {
            val suffix = nextSeq().toString()
            return "m".repeat(KEY_LENGTH - suffix.length) + suffix
        }

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
