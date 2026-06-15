// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.api

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fincore.core.AccountId
import com.fincore.core.TransactionId
import com.fincore.ledger.api.idempotency.IdempotencyAttributes
import com.fincore.test.containers.PostgresContainerExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
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
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.time.Instant

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(PostgresContainerExtension::class)
@Import(ErrorContractIT.TestSecurity::class)
class ErrorContractIT(
    @Autowired private val rest: TestRestTemplate,
    @Autowired private val objectMapper: ObjectMapper,
) {
    @TestConfiguration
    class TestSecurity {
        @Bean
        fun jwtDecoder(): JwtDecoder =
            JwtDecoder { token ->
                Jwt
                    .withTokenValue(token)
                    .header("alg", "none")
                    .subject("err-contract-it")
                    .claim("scope", "ledger:read ledger:write")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(EXPIRY_SECONDS))
                    .build()
            }
    }

    @Test
    fun `should return the account-not-found contract for an unknown account`() {
        val body = get("/v1/accounts/${AccountId.generate()}")
        body.get("type").asText() shouldBe "https://fincore.dev/errors/account-not-found"
        body.get("code").asText() shouldBe "ACCOUNT_NOT_FOUND"
    }

    @Test
    fun `should return the transaction-not-found contract for an unknown transaction`() {
        val body = get("/v1/transactions/${TransactionId.generate()}")
        body.get("type").asText() shouldBe "https://fincore.dev/errors/transaction-not-found"
        body.get("code").asText() shouldBe "TRANSACTION_NOT_FOUND"
    }

    @Test
    fun `should return the idempotency-conflict contract on key reuse with a different body`() {
        val key = "i".repeat(KEY_LENGTH)
        post("/v1/accounts", account("First wallet"), key)
        val conflict = post("/v1/accounts", account("Second wallet"), key)
        conflict.statusCode.value() shouldBe 409
        val body = objectMapper.readTree(conflict.body)
        body.get("type").asText() shouldBe "https://fincore.dev/errors/idempotency-conflict"
        body.get("code").asText() shouldBe "IDEMPOTENCY_KEY_CONFLICT"
    }

    @Test
    fun `should return the double-entry contract for an unbalanced transaction`() {
        val unbalanced =
            """
            {"reference":"err-it-422","currency":"EUR","entries":[
              {"accountId":"${AccountId.generate()}","direction":"DEBIT","amount":"100"},
              {"accountId":"${AccountId.generate()}","direction":"CREDIT","amount":"50"}]}
            """.trimIndent()
        val response = post("/v1/transactions", unbalanced, "d".repeat(KEY_LENGTH))
        response.statusCode.value() shouldBe 422
        val body = objectMapper.readTree(response.body)
        body.get("type").asText() shouldBe "https://fincore.dev/errors/double-entry-violation"
        body.get("code").asText() shouldBe "ENTRIES_SUM_NOT_ZERO"
    }

    @Test
    fun `should return per-field codes for an invalid transaction body`() {
        val invalid =
            """
            {"reference":"err-it-400","currency":"","entries":[
              {"accountId":"${AccountId.generate()}","direction":"DEBIT","amount":"100"}]}
            """.trimIndent()
        val response = post("/v1/transactions", invalid, "v".repeat(KEY_LENGTH))
        response.statusCode.value() shouldBe 400
        val body = objectMapper.readTree(response.body)
        body.get("type").asText() shouldBe "https://fincore.dev/errors/validation-failed"
        body.get("code").asText() shouldBe "VALIDATION_FAILED"
        val fields = body.get("errors").map { it.get("field").asText() }
        (fields.contains("currency") || fields.contains("entries")) shouldBe true
        body.get("errors").forEach { (it.get("code").asText().isNotBlank()) shouldBe true }
    }

    @Test
    fun `should return the invalid-request contract when the idempotency key is missing`() {
        val response = post("/v1/transactions", account("ignored"), idemKey = null)
        response.statusCode.value() shouldBe 400
        val body = objectMapper.readTree(response.body)
        body.get("type").asText() shouldStartWith "https://fincore.dev/errors/"
        body.get("code").asText() shouldBe "INVALID_REQUEST"
    }

    private fun account(name: String): String = """{"name":"$name","type":"USER_WALLET","currency":"EUR"}"""

    private fun get(path: String): JsonNode {
        val headers = HttpHeaders().apply { setBearerAuth("err-it-token") }
        val response = rest.exchange(path, HttpMethod.GET, HttpEntity<Void>(headers), String::class.java)
        return objectMapper.readTree(response.body)
    }

    private fun post(
        path: String,
        body: String,
        idemKey: String?,
    ) = rest.exchange(path, HttpMethod.POST, HttpEntity(body, postHeaders(idemKey)), String::class.java)

    private fun postHeaders(idemKey: String?): HttpHeaders =
        HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            setBearerAuth("err-it-token")
            idemKey?.let { set(IdempotencyAttributes.HEADER, it) }
        }

    companion object {
        private const val EXPIRY_SECONDS = 300L
        private const val KEY_LENGTH = 40

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
