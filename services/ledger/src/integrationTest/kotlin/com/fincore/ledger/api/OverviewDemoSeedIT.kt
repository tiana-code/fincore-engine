// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.api

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import java.time.Instant

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(OverviewDemoSeedIT.TestSecurity::class)
class OverviewDemoSeedIT(
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
                    .subject("overview-seed-it")
                    .claim("scope", "ledger:read")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(EXPIRY_SECONDS))
                    .build()
            }
    }

    @Test
    fun `demo seed populates the overview feed and sparkline`() {
        val headers = HttpHeaders().apply { setBearerAuth("overview-token") }
        val response = rest.exchange("/v1/overview", HttpMethod.GET, HttpEntity<Void>(headers), String::class.java)

        response.statusCode.value() shouldBe 200
        val tree = objectMapper.readTree(response.body)

        val activity = tree.get("activity")
        (activity.size() > 0) shouldBe true
        activity.any { it.get("type").asText() == "transaction.posted" } shouldBe true

        val sparkline = tree.get("transactionsLast24h")
        sparkline.size() shouldBe SPARK_HOURS
        (sparkline.sumOf { it.asInt() } > 0) shouldBe true
    }

    companion object {
        private const val EXPIRY_SECONDS = 300L
        private const val SPARK_HOURS = 24

        // Dedicated container: the demo seed commits rows, so it must not share the singleton
        // database used by the other integration tests (it would skew their ordering/counts).
        private val postgres =
            PostgreSQLContainer("postgres:17-alpine")
                .withDatabaseName("fincore_demo_seed")
                .withUsername("fincore")
                .withPassword("fincore")
                .also { it.start() }

        @JvmStatic
        @DynamicPropertySource
        fun datasourceProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
            registry.add("spring.jpa.hibernate.ddl-auto") { "none" }
            registry.add("spring.liquibase.contexts") { "production,demo" }
        }
    }
}
