// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fincore.test.containers.PostgresContainerExtension
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
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
@Import(HealthProbesIT.TestSecurity::class)
class HealthProbesIT(
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
                    .subject("health-probes-it")
                    .claim("scope", "ledger:read")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(EXPIRY_SECONDS))
                    .build()
            }
    }

    @Test
    fun `should serve the liveness probe without a token and report only the jvm state`() {
        val response = rest.getForEntity("/actuator/health/liveness", String::class.java)

        response.statusCode.value() shouldBe OK
        val components = objectMapper.readTree(response.body).get("components")
        components.shouldNotBeNull()
        components.has("livenessState") shouldBe true
        components.has("db") shouldBe false
    }

    @Test
    fun `should serve the readiness probe without a token aggregating database and migration state`() {
        val response = rest.getForEntity("/actuator/health/readiness", String::class.java)

        response.statusCode.value() shouldBe OK
        val components = objectMapper.readTree(response.body).get("components")
        components.shouldNotBeNull()
        components.get("db").get("status").asText() shouldBe "UP"
        components.get("liquibase").get("status").asText() shouldBe "UP"
    }

    @Test
    fun `should serve the aggregate health endpoint without a token`() {
        rest.getForEntity("/actuator/health", String::class.java).statusCode.value() shouldBe OK
    }

    companion object {
        private const val EXPIRY_SECONDS = 300L
        private const val OK = 200

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
