// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.api

import com.fincore.core.AccountId
import com.fincore.core.TransactionId
import com.fincore.ledger.api.idempotency.IdempotencyAttributes
import com.fincore.ledger.api.observability.CorrelationIdAttributes
import com.fincore.ledger.application.RequestHashing
import com.fincore.ledger.domain.enum.AuditResult
import com.fincore.ledger.infrastructure.persistence.AuditEventRepository
import com.fincore.ledger.infrastructure.persistence.OutboxEventRepository
import com.fincore.test.containers.PostgresContainerExtension
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
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
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(PostgresContainerExtension::class)
@Import(FailureAuditIT.TestSecurity::class)
class FailureAuditIT(
    @Autowired private val rest: TestRestTemplate,
    @Autowired private val auditRepository: AuditEventRepository,
    @Autowired private val outboxRepository: OutboxEventRepository,
) {
    @TestConfiguration
    class TestSecurity {
        @Bean
        fun jwtDecoder(): JwtDecoder =
            JwtDecoder { token ->
                Jwt
                    .withTokenValue(token)
                    .header("alg", "none")
                    .subject(ACTOR)
                    .claim("scope", "ledger:write")
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
    fun `should write a single committed FAILURE row with request hash for an unbalanced post`() {
        val correlationId = UUID.randomUUID().toString()
        val body =
            """{"reference":"ref-${unique()}","currency":"USD","entries":[""" +
                """{"accountId":"${AccountId.generate()}","direction":"DEBIT","amount":"100"},""" +
                """{"accountId":"${AccountId.generate()}","direction":"CREDIT","amount":"50"}]}"""

        val response = post("/v1/transactions", body, correlationId)

        response.statusCode.value() shouldBe 422
        val rows = auditRepository.findAll().filter { it.correlationId == correlationId }
        rows shouldHaveSize 1
        val row = rows.first()
        row.result shouldBe AuditResult.FAILURE
        row.action shouldBe "TRANSACTION_POST"
        row.resourceType shouldBe "TRANSACTION"
        row.actorId shouldBe ACTOR
        row.requestHash shouldBe RequestHashing.sha256Hex(body)
    }

    @Test
    fun `should write a single FAILURE row with the original transaction id for a reverse of an unknown transaction`() {
        val correlationId = UUID.randomUUID().toString()
        val unknownId = TransactionId.generate().toString()
        val body = "{}"

        val response = post("/v1/transactions/$unknownId/reverse", body, correlationId)

        response.statusCode.value() shouldBe 404
        val rows = auditRepository.findAll().filter { it.correlationId == correlationId }
        rows shouldHaveSize 1
        val row = rows.first()
        row.result shouldBe AuditResult.FAILURE
        row.action shouldBe "TRANSACTION_REVERSE"
        row.resourceId shouldBe unknownId
        row.requestHash shouldBe RequestHashing.sha256Hex(body)
    }

    @Test
    fun `should not write any audit row for a validation failure`() {
        val correlationId = UUID.randomUUID().toString()

        val response = post("/v1/transactions", """{"currency":""}""", correlationId)

        response.statusCode.value() shouldBe 400
        auditRepository.findAll().filter { it.correlationId == correlationId } shouldHaveSize 0
    }

    private fun post(
        path: String,
        body: String,
        correlationId: String,
    ) = rest.exchange(
        path,
        HttpMethod.POST,
        HttpEntity(
            body,
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                setBearerAuth("failure-audit-it-token")
                set(IdempotencyAttributes.HEADER, idemKey())
                set(CorrelationIdAttributes.HEADER, correlationId)
            },
        ),
        String::class.java,
    )

    private companion object {
        const val ACTOR = "failure-audit-it"
        const val EXPIRY_SECONDS = 3600L
        const val KEY_LENGTH = 40
        private var counter = 0

        fun unique(): Int = ++counter

        fun idemKey(): String {
            val suffix = unique().toString()
            return "k".repeat(KEY_LENGTH - suffix.length) + suffix
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
