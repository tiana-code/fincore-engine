// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fincore.decision.store.persistence.DecisionLogRepository
import com.fincore.test.containers.PostgresContainerExtension
import io.kotest.matchers.shouldBe
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
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.time.Instant
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(PostgresContainerExtension::class)
@Import(DecisionReplayApiIT.TestSecurity::class)
class DecisionReplayApiIT(
    @Autowired private val rest: TestRestTemplate,
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val decisionLogRepository: DecisionLogRepository,
) {
    private val matchRule = """{"condition":{"attr":"amount","op":"gte","value":100},"outcome":{"label":"approve"}}"""
    private val candidateRule = """{"condition":{"attr":"amount","op":"gte","value":1000},"outcome":{"label":"approve"}}"""
    private val redosRule = """{"condition":{"attr":"s","op":"matches","value":"(.*,){1,100}Z"},"outcome":{"label":"x"}}"""

    @TestConfiguration
    class TestSecurity {
        @Bean
        fun jwtDecoder(): JwtDecoder =
            JwtDecoder { token ->
                val scope = if (token == WRITER) "decision:read decision:write" else "decision:read"
                Jwt
                    .withTokenValue(token)
                    .header("alg", "none")
                    .subject("api-user")
                    .claim("scope", scope)
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(EXPIRY_SECONDS))
                    .build()
            }
    }

    @Test
    fun `should diff a candidate against a recorded decision and write nothing`() {
        val key = newKey()
        publishRule(key, matchRule)
        evaluate(key, """{"amount":150}""").statusCode shouldBe HttpStatus.OK
        val auditCountBefore = decisionLogRepository.count()

        val response = replay("""{"candidate":$candidateRule,"inputs":[{"amount":150}]}""")

        response.statusCode shouldBe HttpStatus.OK
        val report = objectMapper.readTree(response.body)
        report.get("changed").asInt() shouldBe 1
        report
            .get("diffs")
            .get(0)
            .get("status")
            .asText() shouldBe "CHANGED"
        decisionLogRepository.count() shouldBe auditCountBefore
    }

    @Test
    fun `should return no baseline for an input never evaluated`() {
        val response = replay("""{"candidate":$matchRule,"inputs":[{"amount":${UUID.randomUUID().hashCode()}}]}""")

        response.statusCode shouldBe HttpStatus.OK
        objectMapper.readTree(response.body).get("noBaseline").asInt() shouldBe 1
    }

    @Test
    fun `should return 503 when a candidate pattern catastrophically backtracks`() {
        val response = replay("""{"candidate":$redosRule,"inputs":[{"s":"${"a,".repeat(REDOS_REPEATS)}"}]}""")

        response.statusCode shouldBe HttpStatus.SERVICE_UNAVAILABLE
    }

    @Test
    fun `should reject replay carrying only the read scope`() {
        val response =
            rest.exchange(
                "/v1/decision/replay",
                HttpMethod.POST,
                HttpEntity("""{"candidate":$matchRule,"inputs":[{"amount":1}]}""", headers(READER)),
                String::class.java,
            )

        response.statusCode shouldBe HttpStatus.FORBIDDEN
    }

    private fun publishRule(
        key: String,
        dsl: String,
    ) {
        rest
            .postForEntity("/v1/decision/rules", HttpEntity("""{"ruleKey":"$key"}""", headers(WRITER)), String::class.java)
            .statusCode shouldBe HttpStatus.CREATED
        rest
            .postForEntity("/v1/decision/rules/$key/versions", HttpEntity(dsl, headers(WRITER)), String::class.java)
            .statusCode shouldBe HttpStatus.CREATED
    }

    private fun evaluate(
        key: String,
        input: String,
    ) = rest.postForEntity("/v1/decision/rules/$key/evaluate", HttpEntity(input, headers(WRITER)), String::class.java)

    private fun replay(body: String) = rest.postForEntity("/v1/decision/replay", HttpEntity(body, headers(WRITER)), String::class.java)

    private fun headers(token: String): HttpHeaders =
        HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            setBearerAuth(token)
        }

    private fun newKey(): String = "rule-${UUID.randomUUID()}"

    companion object {
        private const val WRITER = "writer"
        private const val READER = "reader"
        private const val EXPIRY_SECONDS = 300L
        private const val REDOS_REPEATS = 25

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { PostgresContainerExtension.jdbcUrl }
            registry.add("spring.datasource.username") { PostgresContainerExtension.username }
            registry.add("spring.datasource.password") { PostgresContainerExtension.password }
            registry.add("spring.jpa.hibernate.ddl-auto") { "none" }
            registry.add("fincore.decision.api.evaluation-timeout-millis") { "100" }
        }
    }
}
