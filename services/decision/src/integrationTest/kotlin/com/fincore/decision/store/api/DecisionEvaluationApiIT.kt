// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fincore.decision.store.persistence.DecisionLogRepository
import com.fincore.test.containers.PostgresContainerExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
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
@Import(DecisionEvaluationApiIT.TestSecurity::class)
class DecisionEvaluationApiIT(
    @Autowired private val rest: TestRestTemplate,
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val decisionLogRepository: DecisionLogRepository,
) {
    private val matchRule = """{"condition":{"attr":"amount","op":"gte","value":100},"outcome":{"label":"approve","reasonCodes":["OK"]}}"""
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
    fun `should evaluate a rule and write exactly one decision log`() {
        val rule = publishRule(matchRule)

        val response = evaluate(rule.key, """{"amount":150}""")
        response.statusCode shouldBe HttpStatus.OK
        val body = objectMapper.readTree(response.body)
        body.get("matched").asBoolean() shouldBe true
        body.get("outcome").get("label").asText() shouldBe "approve"

        val logs = decisionLogRepository.findByRuleVersionId(rule.versionId)
        logs.size shouldBe 1
        logs.single().inputHash shouldMatch Regex("^[0-9a-f]{64}$")
        logs.single().outcomeLabel shouldBe "approve"
    }

    @Test
    fun `should return 503 and write no log when a pattern catastrophically backtracks`() {
        val rule = publishRule(redosRule)

        val response = evaluate(rule.key, """{"s":"${"a,".repeat(REDOS_REPEATS)}"}""")

        response.statusCode shouldBe HttpStatus.SERVICE_UNAVAILABLE
        decisionLogRepository.findByRuleVersionId(rule.versionId).size shouldBe 0
    }

    @Test
    fun `should hash the same logical input identically across evaluations`() {
        val rule = publishRule(matchRule)

        evaluate(rule.key, """{"amount":150,"country":"A"}""").statusCode shouldBe HttpStatus.OK
        evaluate(rule.key, """{"country":"A","amount":150}""").statusCode shouldBe HttpStatus.OK

        val hashes = decisionLogRepository.findByRuleVersionId(rule.versionId).map { it.inputHash }.toSet()
        hashes.size shouldBe 1
    }

    @Test
    fun `should fetch logs by rule version and by input hash and reject no filter`() {
        val rule = publishRule(matchRule)
        evaluate(rule.key, """{"amount":150}""").statusCode shouldBe HttpStatus.OK
        val hash = decisionLogRepository.findByRuleVersionId(rule.versionId).single().inputHash

        readLogs("ruleVersionId=${rule.versionId}").statusCode shouldBe HttpStatus.OK
        readLogs("inputHash=$hash").statusCode shouldBe HttpStatus.OK
        readLogs("").statusCode shouldBe HttpStatus.BAD_REQUEST
    }

    @Test
    fun `should reject evaluate carrying only the read scope`() {
        val rule = publishRule(matchRule)
        val response =
            rest.exchange(
                "/v1/decision/rules/${rule.key}/evaluate",
                HttpMethod.POST,
                HttpEntity("""{"amount":1}""", headers(READER)),
                String::class.java,
            )

        response.statusCode shouldBe HttpStatus.FORBIDDEN
    }

    private data class PublishedRule(
        val key: String,
        val versionId: UUID,
    )

    private fun publishRule(dsl: String): PublishedRule {
        val key = newKey()
        rest
            .postForEntity("/v1/decision/rules", HttpEntity("""{"ruleKey":"$key"}""", headers(WRITER)), String::class.java)
            .statusCode shouldBe HttpStatus.CREATED
        val response = rest.postForEntity("/v1/decision/rules/$key/versions", HttpEntity(dsl, headers(WRITER)), String::class.java)
        response.statusCode shouldBe HttpStatus.CREATED
        return PublishedRule(key, UUID.fromString(objectMapper.readTree(response.body).get("id").asText()))
    }

    private fun evaluate(
        key: String,
        input: String,
    ) = rest.postForEntity("/v1/decision/rules/$key/evaluate", HttpEntity(input, headers(WRITER)), String::class.java)

    private fun readLogs(query: String) =
        rest.exchange("/v1/decision/logs?$query", HttpMethod.GET, HttpEntity<Unit>(headers(READER)), String::class.java)

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
