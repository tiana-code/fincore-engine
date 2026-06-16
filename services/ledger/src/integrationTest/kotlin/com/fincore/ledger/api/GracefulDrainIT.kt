// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.api

import com.fincore.test.containers.PostgresContainerExtension
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.context.WebServerApplicationContext
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(PostgresContainerExtension::class)
@Import(GracefulDrainIT.TestSecurity::class, GracefulDrainIT.SlowEndpointConfig::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class GracefulDrainIT(
    @Autowired private val rest: TestRestTemplate,
    @Autowired private val context: ApplicationContext,
) {
    @TestConfiguration
    class TestSecurity {
        @Bean
        fun jwtDecoder(): JwtDecoder =
            JwtDecoder { token ->
                Jwt
                    .withTokenValue(token)
                    .header("alg", "none")
                    .subject("graceful-drain-it")
                    .claim("scope", "ledger:read")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(EXPIRY_SECONDS))
                    .build()
            }
    }

    @TestConfiguration
    class SlowEndpointConfig {
        @Bean
        fun slowController(): SlowController = SlowController()
    }

    @RestController
    class SlowController {
        @GetMapping(SLOW_PATH)
        fun slow(): String {
            enteredLatch.countDown()
            proceedLatch.await(DRAIN_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            return "done"
        }
    }

    @BeforeEach
    fun resetLatches() {
        enteredLatch = CountDownLatch(1)
        proceedLatch = CountDownLatch(1)
    }

    @Test
    fun `should complete an in-flight request while the web server shuts down gracefully`() {
        val holder = AtomicReference<ResponseEntity<String>>()
        val request = issueSlowRequest(holder)
        enteredLatch.await(ENTRY_TIMEOUT_SECONDS, TimeUnit.SECONDS) shouldBe true

        val drained = CountDownLatch(1)
        val webServer = (context as WebServerApplicationContext).webServer
        webServer.shutDownGracefully { drained.countDown() }
        proceedLatch.countDown()

        request.join(JOIN_TIMEOUT_MILLIS)
        drained.await(JOIN_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS) shouldBe true
        holder.get().statusCode.value() shouldBe OK
    }

    private fun issueSlowRequest(holder: AtomicReference<ResponseEntity<String>>): Thread {
        val entity = HttpEntity<Void>(HttpHeaders().apply { setBearerAuth("drain-token") })
        return thread { holder.set(rest.exchange(SLOW_PATH, HttpMethod.GET, entity, String::class.java)) }
    }

    companion object {
        const val SLOW_PATH = "/test/slow"
        private const val EXPIRY_SECONDS = 300L
        private const val OK = 200
        private const val DRAIN_TIMEOUT_SECONDS = 10L
        private const val ENTRY_TIMEOUT_SECONDS = 5L
        private const val JOIN_TIMEOUT_MILLIS = 35_000L

        @Volatile
        internal var enteredLatch = CountDownLatch(1)

        @Volatile
        internal var proceedLatch = CountDownLatch(1)

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
