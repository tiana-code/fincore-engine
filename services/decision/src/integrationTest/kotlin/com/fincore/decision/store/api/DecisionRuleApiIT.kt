// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fincore.decision.store.persistence.RuleVersionRepository
import com.fincore.test.containers.PostgresContainerExtension
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.ints.shouldBeGreaterThan
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
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(PostgresContainerExtension::class)
@Import(DecisionRuleApiIT.TestSecurity::class)
class DecisionRuleApiIT(
    @Autowired private val rest: TestRestTemplate,
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val versionRepository: RuleVersionRepository,
) {
    private val dsl = """{"condition":{"attr":"amount","op":"gte","value":100},"outcome":{"label":"approve"}}"""

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
    fun `should serve the openapi document with the decision rule operations`() {
        val response = rest.getForEntity("/v3/api-docs", String::class.java)
        response.statusCode shouldBe HttpStatus.OK
        response.body.orEmpty() shouldContain "/v1/decision/rules"
    }

    @Test
    fun `should reject an unauthenticated read with 401`() {
        rest.getForEntity("/v1/decision/rules/anything", String::class.java).statusCode shouldBe HttpStatus.UNAUTHORIZED
    }

    @Test
    fun `should reject a publish carrying only the read scope with 403`() {
        val key = newKey()
        createRule(key)
        val response = rest.exchange(versionsUrl(key), HttpMethod.POST, HttpEntity(dsl, headers(READER)), String::class.java)
        response.statusCode shouldBe HttpStatus.FORBIDDEN
    }

    @Test
    fun `should publish versions monotonically and move the active pointer when republished`() {
        val key = newKey()
        createRule(key)

        publishVersion(key).statusCode shouldBe HttpStatus.CREATED
        readActiveVersionNo(key) shouldBe 1
        publishVersion(key).statusCode shouldBe HttpStatus.CREATED
        readActiveVersionNo(key) shouldBe 2
    }

    @Test
    fun `should assign one version number per concurrent publish and never return 500`() {
        val key = newKey()
        val ruleId = createRule(key)
        val statuses = publishConcurrently(key)

        // Every concurrent publish is a typed outcome: 201 success, 409 duplicate version_no, or 503
        // optimistic-lock retry on the active pointer. Never a 500. The unique (rule_id, version_no) and the
        // @Version pointer lock are the guards, so stored versions equal the successes and are all distinct.
        statuses.none { it == HttpStatus.INTERNAL_SERVER_ERROR } shouldBe true
        statuses.all { it in TYPED_OUTCOMES } shouldBe true
        statuses shouldContain HttpStatus.CREATED
        val versions = versionRepository.findByRuleId(ruleId)
        versions.map { it.versionNo }.toSet().size shouldBe versions.size
        versions.size shouldBe statuses.count { it == HttpStatus.CREATED }
        versions.size shouldBeGreaterThan 0
    }

    private fun publishConcurrently(key: String): List<HttpStatusCode> {
        val pool = Executors.newFixedThreadPool(THREADS)
        val start = CountDownLatch(1)
        val tasks =
            (1..THREADS).map {
                pool.submit<HttpStatusCode> {
                    start.await()
                    publishVersion(key).statusCode
                }
            }
        start.countDown()
        val statuses = tasks.map { it.get(TASK_TIMEOUT_SECONDS, TimeUnit.SECONDS) }
        pool.shutdown()
        return statuses
    }

    private fun createRule(key: String): UUID {
        val response = rest.postForEntity("/v1/decision/rules", HttpEntity("""{"ruleKey":"$key"}""", headers(WRITER)), String::class.java)
        response.statusCode shouldBe HttpStatus.CREATED
        return UUID.fromString(objectMapper.readTree(response.body).get("id").asText())
    }

    private fun publishVersion(key: String) = rest.postForEntity(versionsUrl(key), HttpEntity(dsl, headers(WRITER)), String::class.java)

    private fun readActiveVersionNo(key: String): Int {
        val response = rest.exchange("/v1/decision/rules/$key", HttpMethod.GET, HttpEntity<Unit>(headers(READER)), String::class.java)
        response.statusCode shouldBe HttpStatus.OK
        return objectMapper
            .readTree(response.body)
            .get("activeVersion")
            .get("versionNo")
            .asInt()
    }

    private fun headers(token: String): HttpHeaders =
        HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            setBearerAuth(token)
        }

    private fun versionsUrl(key: String): String = "/v1/decision/rules/$key/versions"

    private fun newKey(): String = "rule-${UUID.randomUUID()}"

    companion object {
        private val TYPED_OUTCOMES = setOf(HttpStatus.CREATED, HttpStatus.CONFLICT, HttpStatus.SERVICE_UNAVAILABLE)
        private const val WRITER = "writer"
        private const val READER = "reader"
        private const val THREADS = 6
        private const val EXPIRY_SECONDS = 300L
        private const val TASK_TIMEOUT_SECONDS = 20L

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
