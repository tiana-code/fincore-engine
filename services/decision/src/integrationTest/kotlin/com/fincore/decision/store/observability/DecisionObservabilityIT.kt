// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.observability

import com.fincore.test.containers.PostgresContainerExtension
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.micrometer.tracing.Tracer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.core.env.Environment
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.time.Instant

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureObservability
@ExtendWith(PostgresContainerExtension::class)
@Import(DecisionObservabilityIT.TestSecurity::class)
class DecisionObservabilityIT(
    @Autowired private val rest: TestRestTemplate,
    @Autowired private val tracer: Tracer,
    @Autowired private val environment: Environment,
) {
    @TestConfiguration
    class TestSecurity {
        @Bean
        fun jwtDecoder(): JwtDecoder =
            JwtDecoder { token ->
                Jwt
                    .withTokenValue(token)
                    .header("alg", "none")
                    .subject("observability-it")
                    .claim("scope", "decision:read decision:write")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(EXPIRY_SECONDS))
                    .build()
            }
    }

    @Test
    fun `should expose the prometheus exposition without a token`() {
        val response = rest.getForEntity("/actuator/prometheus", String::class.java)

        response.statusCode.value() shouldBe OK
        (response.body ?: "") shouldContain "# TYPE"
    }

    @Test
    fun `should serve the readiness and liveness probes`() {
        rest.getForEntity("/actuator/health/readiness", String::class.java).statusCode.value() shouldBe OK
        rest.getForEntity("/actuator/health/liveness", String::class.java).statusCode.value() shouldBe OK
    }

    @Test
    fun `should expose a tracer bean and bind the tracing properties`() {
        tracer.shouldNotBeNull()
        environment.getProperty("management.tracing.sampling.probability").shouldNotBeNull()
        (environment.getProperty("management.otlp.tracing.endpoint") ?: "") shouldContain "/v1/traces"
    }

    private companion object {
        const val EXPIRY_SECONDS = 3600L
        const val OK = 200

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
