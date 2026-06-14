// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.api

import com.fincore.ledger.api.idempotency.IdempotencyAttributes
import com.fincore.test.containers.PostgresContainerExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
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
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.time.Instant

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(PostgresContainerExtension::class)
@Import(LedgerApiSmokeIT.TestSecurity::class)
class LedgerApiSmokeIT(
    @Autowired private val rest: TestRestTemplate,
) {
    @TestConfiguration
    class TestSecurity {
        @Bean
        fun jwtDecoder(): JwtDecoder =
            JwtDecoder { token ->
                Jwt
                    .withTokenValue(token)
                    .header("alg", "none")
                    .subject("smoke-user")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(EXPIRY_SECONDS))
                    .build()
            }
    }

    @Test
    fun `should serve the openapi document with the ledger operations and license`() {
        val response = rest.getForEntity("/v3/api-docs", String::class.java)

        response.statusCode.value() shouldBe 200
        val body = response.body ?: ""
        body shouldContain "/v1/accounts"
        body shouldContain "/v1/transactions"
        body shouldContain "BUSL-1.1"
    }

    @Test
    fun `should replay an identical response for a repeated create with the same key and body`() {
        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                setBearerAuth("smoke-token")
                set(IdempotencyAttributes.HEADER, "s".repeat(40))
            }
        val payload = """{"name":"Smoke wallet","type":"USER_WALLET","currency":"EUR"}"""
        val request = HttpEntity(payload, headers)

        val first = rest.postForEntity("/v1/accounts", request, String::class.java)
        val second = rest.postForEntity("/v1/accounts", request, String::class.java)

        first.statusCode.value() shouldBe 201
        second.statusCode.value() shouldBe 201
        second.body shouldBe first.body
    }

    @Test
    fun `should reject an unauthenticated request with 401`() {
        val response = rest.getForEntity("/v1/accounts/acc_0000000000000000000000000", String::class.java)
        response.statusCode.value() shouldBe 401
    }

    companion object {
        private const val EXPIRY_SECONDS = 300L

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
