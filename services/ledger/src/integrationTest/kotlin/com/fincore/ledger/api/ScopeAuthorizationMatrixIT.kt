// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.api

import com.fincore.core.AccountId
import com.fincore.core.TransactionId
import com.fincore.ledger.api.idempotency.IdempotencyAttributes
import com.fincore.ledger.api.observability.CorrelationIdAttributes
import com.fincore.test.containers.PostgresContainerExtension
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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
import java.time.Instant
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(PostgresContainerExtension::class)
@Import(ScopeAuthorizationMatrixIT.ScopeFromTokenSecurity::class)
class ScopeAuthorizationMatrixIT(
    @Autowired private val rest: TestRestTemplate,
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
        val path: String,
        val access: Access,
    )

    private val accountId = AccountId.generate().toString()
    private val transactionId = TransactionId.generate().toString()

    private val endpoints =
        listOf(
            Endpoint(HttpMethod.POST, "/v1/accounts", Access.WRITE),
            Endpoint(HttpMethod.GET, "/v1/accounts", Access.READ),
            Endpoint(HttpMethod.GET, "/v1/accounts/$accountId", Access.READ),
            Endpoint(HttpMethod.GET, "/v1/accounts/$accountId/balance", Access.READ),
            Endpoint(HttpMethod.GET, "/v1/accounts/$accountId/entries", Access.READ),
            Endpoint(HttpMethod.POST, "/v1/transactions", Access.WRITE),
            Endpoint(HttpMethod.GET, "/v1/transactions", Access.READ),
            Endpoint(HttpMethod.GET, "/v1/transactions/$transactionId", Access.READ),
            Endpoint(HttpMethod.POST, "/v1/transactions/$transactionId/reverse", Access.WRITE),
        )

    @Test
    fun `should pass authorization for every endpoint when the token carries the matching scope`() {
        endpoints.forEach { endpoint ->
            val response = call(endpoint, endpoint.access)
            withClue("${endpoint.method} ${endpoint.path} with ${endpoint.access}") {
                response.statusCode.value() shouldNotBe FORBIDDEN
                response.statusCode.value() shouldNotBe UNAUTHORIZED
            }
        }
    }

    @Test
    fun `should return 403 for every endpoint when the token carries only the opposite scope`() {
        endpoints.forEach { endpoint ->
            val opposite = if (endpoint.access == Access.READ) Access.WRITE else Access.READ
            val response = call(endpoint, opposite)
            withClue("${endpoint.method} ${endpoint.path} with $opposite") {
                response.statusCode.value() shouldBe FORBIDDEN
            }
        }
    }

    private fun call(
        endpoint: Endpoint,
        access: Access,
    ): ResponseEntity<String> {
        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                setBearerAuth(bearerFor(access))
                set(CorrelationIdAttributes.HEADER, UUID.randomUUID().toString())
                if (endpoint.method == HttpMethod.POST) set(IdempotencyAttributes.HEADER, idemKey())
            }
        val body = if (endpoint.method == HttpMethod.POST) "{}" else null
        return rest.exchange(endpoint.path, endpoint.method, HttpEntity(body, headers), String::class.java)
    }

    private fun bearerFor(access: Access): String = if (access == Access.READ) "read" else "write"

    private companion object {
        const val EXPIRY_SECONDS = 3600L
        const val KEY_LENGTH = 40
        const val FORBIDDEN = 403
        const val UNAUTHORIZED = 401
        private var counter = 0

        fun idemKey(): String {
            val suffix = (++counter).toString()
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
