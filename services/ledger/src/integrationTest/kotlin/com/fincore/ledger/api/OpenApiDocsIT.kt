// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.api

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fincore.test.containers.PostgresContainerExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.time.Instant

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(PostgresContainerExtension::class)
@Import(OpenApiDocsIT.TestSecurity::class)
class OpenApiDocsIT(
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
                    .subject("openapi-docs-it")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(EXPIRY_SECONDS))
                    .build()
            }
    }

    private fun apiDocs(): JsonNode = objectMapper.readTree(rest.getForEntity("/v3/api-docs", String::class.java).body)

    @Test
    fun `should document the create-account operation with a summary tag and responses`() {
        val post = apiDocs().at("/paths/~1v1~1accounts/post")
        post.get("summary").asText().shouldNotBeBlank()
        post.get("tags").map { it.asText() }.contains("Accounts") shouldBe true
        val responses = post.get("responses")
        listOf("201", "400", "409").forEach { responses.has(it) shouldBe true }
    }

    @Test
    fun `should document the reverse-transaction operation with conflict and not-found responses`() {
        val post = apiDocs().at("/paths/~1v1~1transactions~1{id}~1reverse/post")
        post.get("summary").asText().shouldNotBeBlank()
        val responses = post.get("responses")
        listOf("201", "404", "409").forEach { responses.has(it) shouldBe true }
    }

    @Test
    fun `should document the get-account operation with a not-found response`() {
        val get = apiDocs().at("/paths/~1v1~1accounts~1{id}/get")
        get.get("summary").asText().shouldNotBeBlank()
        get.get("responses").has("404") shouldBe true
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
