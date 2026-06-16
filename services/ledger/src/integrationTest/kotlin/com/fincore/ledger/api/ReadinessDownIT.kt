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
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import java.time.Instant

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ReadinessDownIT.TestSecurity::class)
class ReadinessDownIT(
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
                    .subject("readiness-down-it")
                    .claim("scope", "ledger:read")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(EXPIRY_SECONDS))
                    .build()
            }
    }

    @Test
    fun `should flip readiness to down when the database becomes unreachable`() {
        rest.getForEntity(READINESS, String::class.java).statusCode.value() shouldBe OK

        database.stop()

        val response = rest.getForEntity(READINESS, String::class.java)
        response.statusCode.value() shouldBe SERVICE_UNAVAILABLE
        objectMapper.readTree(response.body).get("status").asText() shouldBe "DOWN"
    }

    companion object {
        private const val EXPIRY_SECONDS = 300L
        private const val OK = 200
        private const val SERVICE_UNAVAILABLE = 503
        private const val READINESS = "/actuator/health/readiness"

        @JvmStatic
        private val database =
            PostgreSQLContainer("postgres:17-alpine")
                .withDatabaseName("readiness_down_it")
                .withUsername("fincore")
                .withPassword("fincore")
                .also { it.start() }

        @JvmStatic
        @DynamicPropertySource
        fun datasourceProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { database.jdbcUrl }
            registry.add("spring.datasource.username") { database.username }
            registry.add("spring.datasource.password") { database.password }
            registry.add("spring.datasource.hikari.maximum-pool-size") { "2" }
            registry.add("spring.datasource.hikari.connection-timeout") { "2000" }
            registry.add("spring.jpa.hibernate.ddl-auto") { "none" }
        }
    }
}
