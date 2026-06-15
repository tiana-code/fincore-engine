// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.config

import com.fincore.ledger.api.idempotency.IdempotencyAttributes
import com.fincore.ledger.api.observability.CorrelationIdAttributes
import com.fincore.ledger.domain.enum.AuditResult
import com.fincore.ledger.infrastructure.persistence.AuditEventRepository
import com.fincore.test.containers.PostgresContainerExtension
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.core.annotation.Order
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.time.Instant

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(PostgresContainerExtension::class)
@Import(DeniedAuditIT.DenyWritesSecurity::class)
class DeniedAuditIT(
    @Autowired private val rest: TestRestTemplate,
    @Autowired private val auditRepository: AuditEventRepository,
) {
    @TestConfiguration
    class DenyWritesSecurity {
        @Bean
        @Order(1)
        fun deniedAccountsChain(
            http: HttpSecurity,
            accessDeniedHandler: AuditingAccessDeniedHandler,
        ): SecurityFilterChain =
            http
                .securityMatcher("/v1/accounts")
                .csrf { it.disable() }
                .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
                .authorizeHttpRequests { it.anyRequest().denyAll() }
                .exceptionHandling { it.accessDeniedHandler(accessDeniedHandler) }
                .oauth2ResourceServer { it.jwt {} }
                .build()

        @Bean
        fun jwtDecoder(): JwtDecoder =
            JwtDecoder { token ->
                Jwt
                    .withTokenValue(token)
                    .header("alg", "none")
                    .subject(ACTOR)
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(EXPIRY_SECONDS))
                    .build()
            }
    }

    @Test
    fun `should write a single committed DENIED row when an authenticated write is forbidden`() {
        val correlationId = "corr-denied-${unique()}"

        val response = post(token = "valid-token", correlationId = correlationId)

        response.statusCode.value() shouldBe 403
        val rows = auditRepository.findAll().filter { it.correlationId == correlationId }
        rows shouldHaveSize 1
        val row = rows.first()
        row.result shouldBe AuditResult.DENIED
        row.action shouldBe "ACCOUNT_CREATE"
        row.resourceType shouldBe "ACCOUNT"
        row.resourceId shouldBe "unknown"
        row.actorId shouldBe ACTOR
    }

    @Test
    fun `should write no audit row when an unauthenticated write is rejected`() {
        val correlationId = "corr-unauth-${unique()}"

        val response = post(token = null, correlationId = correlationId)

        response.statusCode.value() shouldBe 401
        auditRepository.findAll().filter { it.correlationId == correlationId } shouldHaveSize 0
    }

    private fun post(
        token: String?,
        correlationId: String,
    ) = rest.exchange(
        "/v1/accounts",
        HttpMethod.POST,
        HttpEntity(
            """{"name":"Denied wallet","type":"USER_WALLET","currency":"USD"}""",
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set(IdempotencyAttributes.HEADER, idemKey())
                set(CorrelationIdAttributes.HEADER, correlationId)
                token?.let { setBearerAuth(it) }
            },
        ),
        String::class.java,
    )

    private companion object {
        const val ACTOR = "denied-audit-it"
        const val EXPIRY_SECONDS = 3600L
        const val KEY_LENGTH = 40
        private var counter = 0

        fun unique(): Int = ++counter

        fun idemKey(): String {
            val suffix = unique().toString()
            return "d".repeat(KEY_LENGTH - suffix.length) + suffix
        }

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
