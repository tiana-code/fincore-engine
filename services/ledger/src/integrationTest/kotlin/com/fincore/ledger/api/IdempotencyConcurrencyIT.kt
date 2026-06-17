// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.api

import com.fincore.ledger.api.idempotency.IdempotencyAttributes
import com.fincore.ledger.api.observability.CorrelationIdAttributes
import com.fincore.ledger.infrastructure.persistence.AccountRepository
import com.fincore.ledger.infrastructure.persistence.OutboxEventRepository
import com.fincore.test.containers.PostgresContainerExtension
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
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
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

/**
 * Proves the idempotency contract under real concurrency: POSTERS identical POST /v1/accounts requests
 * sharing one Idempotency-Key must resolve to exactly one persisted account and one committed response that
 * every caller receives, never a duplicate row and never a 5xx. The reservation PK row-lock serializes the
 * racers until the winner commits, so the single-shot replay returns the winner's stored response.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(PostgresContainerExtension::class)
@Import(IdempotencyConcurrencyIT.ScopeFromTokenSecurity::class)
class IdempotencyConcurrencyIT(
    @Autowired private val rest: TestRestTemplate,
    @Autowired private val accountRepository: AccountRepository,
    @Autowired private val outboxRepository: OutboxEventRepository,
) {
    @TestConfiguration
    class ScopeFromTokenSecurity {
        @Bean
        fun jwtDecoder(): JwtDecoder =
            JwtDecoder { token ->
                Jwt
                    .withTokenValue(token)
                    .header("alg", "none")
                    .subject("idempotency-concurrency-it")
                    .claim("scope", "ledger:$token")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(EXPIRY_SECONDS))
                    .build()
            }
    }

    @AfterEach
    fun cleanOutbox() {
        outboxRepository.deleteAll()
    }

    @Test
    fun `should create exactly one account when posters share an idempotency key`() {
        val name = "uc-idem-${UUID.randomUUID()}"
        val body = """{"name":"$name","type":"USER_WALLET","currency":"EUR"}"""
        val key = UUID.randomUUID().toString().replace("-", "")
        val statuses = ConcurrentLinkedQueue<Int>()
        val bodies = ConcurrentLinkedQueue<String>()
        val unexpected = AtomicReference<Throwable?>(null)
        val startGate = CountDownLatch(1)

        Executors.newVirtualThreadPerTaskExecutor().use { executor ->
            val futures =
                (0 until POSTERS).map {
                    executor.submit(
                        Callable {
                            startGate.await()
                            post(body, key, statuses, bodies, unexpected)
                        },
                    )
                }
            startGate.countDown()
            futures.forEach { it.get() }
        }

        unexpected.get()?.let { throw AssertionError("a poster surfaced an unexpected throwable: ${it::class.qualifiedName}", it) }
        statuses shouldHaveSize POSTERS
        statuses.toSet() shouldHaveSize 1
        statuses.first() shouldBe HttpStatus.CREATED.value()
        bodies.toSet() shouldHaveSize 1
        accountRepository.findAll().count { it.name == name } shouldBe 1
    }

    @Suppress("TooGenericExceptionCaught")
    private fun post(
        body: String,
        key: String,
        statuses: ConcurrentLinkedQueue<Int>,
        bodies: ConcurrentLinkedQueue<String>,
        unexpected: AtomicReference<Throwable?>,
    ) {
        try {
            val headers =
                HttpHeaders().apply {
                    contentType = MediaType.APPLICATION_JSON
                    setBearerAuth("write")
                    set(CorrelationIdAttributes.HEADER, UUID.randomUUID().toString())
                    set(IdempotencyAttributes.HEADER, key)
                }
            val response = rest.exchange("/v1/accounts", HttpMethod.POST, HttpEntity(body, headers), String::class.java)
            statuses.add(response.statusCode.value())
            bodies.add(response.body.orEmpty())
        } catch (other: Throwable) {
            unexpected.compareAndSet(null, other)
        }
    }

    private companion object {
        const val EXPIRY_SECONDS = 3600L
        const val POSTERS = 50

        @JvmStatic
        @DynamicPropertySource
        fun datasourceProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { PostgresContainerExtension.jdbcUrl }
            registry.add("spring.datasource.username") { PostgresContainerExtension.username }
            registry.add("spring.datasource.password") { PostgresContainerExtension.password }
            registry.add("spring.datasource.hikari.maximum-pool-size") { "16" }
            registry.add("spring.datasource.hikari.connection-timeout") { "30000" }
            registry.add("spring.jpa.hibernate.ddl-auto") { "none" }
        }
    }
}
