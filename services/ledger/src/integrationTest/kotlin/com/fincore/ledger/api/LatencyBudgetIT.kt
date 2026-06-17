// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fincore.ledger.api.idempotency.IdempotencyAttributes
import com.fincore.ledger.api.observability.CorrelationIdAttributes
import com.fincore.test.containers.PostgresContainerExtension
import io.kotest.assertions.withClue
import io.kotest.matchers.comparables.shouldBeLessThan
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
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.math.ceil

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(PostgresContainerExtension::class)
@Import(LatencyBudgetIT.ScopeFromTokenSecurity::class)
class LatencyBudgetIT(
    @Autowired private val rest: TestRestTemplate,
    @Autowired private val objectMapper: ObjectMapper,
) {
    @TestConfiguration
    class ScopeFromTokenSecurity {
        @Bean
        fun jwtDecoder(): JwtDecoder =
            JwtDecoder { token ->
                Jwt
                    .withTokenValue(token)
                    .header("alg", "none")
                    .subject("latency-budget-it")
                    .claim("scope", "ledger:$token")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(EXPIRY_SECONDS))
                    .build()
            }
    }

    @Test
    fun `should meet p99 latency budgets when ten workers post and read concurrently`() {
        val pairs = setupWorkerAccounts()
        warmUp(pairs)
        val postSamples = measureLatencies(pairs) { pair, samples -> postTimed(pair, samples) }
        val balanceSamples = measureLatencies(pairs) { pair, samples -> balanceTimed(pair.debitId, samples) }
        assertBudget("POST /v1/transactions", postSamples, POST_BUDGET_MS)
        assertBudget("GET /v1/accounts/{id}/balance", balanceSamples, BALANCE_BUDGET_MS)
    }

    private fun setupWorkerAccounts(): List<AccountPair> =
        (0 until WORKERS).map { worker ->
            AccountPair(createAccount("perf-debit-$worker"), createAccount("perf-credit-$worker"))
        }

    private fun createAccount(name: String): String {
        val body = """{"name":"$name","type":"USER_WALLET","currency":"EUR"}"""
        val response = rest.exchange("/v1/accounts", HttpMethod.POST, HttpEntity(body, writeHeaders()), String::class.java)
        response.statusCode shouldBe HttpStatus.CREATED
        return objectMapper.readTree(response.body)["id"].asText()
    }

    private fun warmUp(pairs: List<AccountPair>) {
        runWorkers(pairs) { pair ->
            val sink = ArrayList<Long>(WARMUP * 2)
            repeat(WARMUP) {
                postTimed(pair, sink)
                balanceTimed(pair.debitId, sink)
            }
        }
    }

    private fun measureLatencies(
        pairs: List<AccountPair>,
        call: (AccountPair, MutableList<Long>) -> Unit,
    ): List<Long> {
        val perWorker =
            runWorkers(pairs) { pair ->
                val samples = ArrayList<Long>(MEASURED)
                repeat(MEASURED) { call(pair, samples) }
                samples
            }
        return perWorker.flatten()
    }

    private fun postTimed(
        pair: AccountPair,
        samples: MutableList<Long>,
    ) {
        val entity = HttpEntity(postBody(pair), writeHeaders())
        val start = System.nanoTime()
        val response = rest.exchange("/v1/transactions", HttpMethod.POST, entity, String::class.java)
        val elapsed = System.nanoTime() - start
        response.statusCode shouldBe HttpStatus.CREATED
        samples.add(elapsed)
    }

    private fun balanceTimed(
        accountId: String,
        samples: MutableList<Long>,
    ) {
        val entity = HttpEntity<Unit>(readHeaders())
        val start = System.nanoTime()
        val response = rest.exchange("/v1/accounts/$accountId/balance", HttpMethod.GET, entity, String::class.java)
        val elapsed = System.nanoTime() - start
        response.statusCode shouldBe HttpStatus.OK
        samples.add(elapsed)
    }

    private fun <T> runWorkers(
        pairs: List<AccountPair>,
        task: (AccountPair) -> T,
    ): List<T> {
        val startGate = CountDownLatch(1)
        Executors.newVirtualThreadPerTaskExecutor().use { executor ->
            val futures =
                pairs.map { pair ->
                    executor.submit(
                        Callable {
                            startGate.await()
                            task(pair)
                        },
                    )
                }
            startGate.countDown()
            return futures.map { it.get() }
        }
    }

    private fun assertBudget(
        operation: String,
        samplesNanos: List<Long>,
        budgetMs: Int,
    ) {
        val p99 = percentile(samplesNanos)
        withClue("p99 latency for $operation was ${String.format(Locale.ROOT, "%.1f", p99)}ms, budget ${budgetMs}ms") {
            p99 shouldBeLessThan budgetMs.toDouble()
        }
    }

    private fun percentile(samplesNanos: List<Long>): Double {
        val sorted = samplesNanos.sorted()
        val index = ceil(PERCENTILE * sorted.size).toInt() - 1
        return sorted[index] / NANOS_PER_MS
    }

    private fun postBody(pair: AccountPair): String =
        """{"reference":"perf-${UUID.randomUUID()}","currency":"EUR","entries":[""" +
            """{"accountId":"${pair.debitId}","direction":"DEBIT","amount":"$AMOUNT"},""" +
            """{"accountId":"${pair.creditId}","direction":"CREDIT","amount":"$AMOUNT_NEG"}]}"""

    private fun writeHeaders(): HttpHeaders =
        HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            setBearerAuth("write")
            set(CorrelationIdAttributes.HEADER, UUID.randomUUID().toString())
            set(IdempotencyAttributes.HEADER, UUID.randomUUID().toString().replace("-", ""))
        }

    private fun readHeaders(): HttpHeaders =
        HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            setBearerAuth("read")
            set(CorrelationIdAttributes.HEADER, UUID.randomUUID().toString())
        }

    private data class AccountPair(
        val debitId: String,
        val creditId: String,
    )

    private companion object {
        const val EXPIRY_SECONDS = 3600L
        const val WORKERS = 10
        const val POOL_SIZE = 16
        const val WARMUP = 20
        const val MEASURED = 50
        const val POST_BUDGET_MS = 300
        const val BALANCE_BUDGET_MS = 50
        const val PERCENTILE = 0.99
        const val NANOS_PER_MS = 1_000_000.0
        const val AMOUNT = "100.00"
        const val AMOUNT_NEG = "-100.00"

        @JvmStatic
        @DynamicPropertySource
        fun datasourceProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { PostgresContainerExtension.jdbcUrl }
            registry.add("spring.datasource.username") { PostgresContainerExtension.username }
            registry.add("spring.datasource.password") { PostgresContainerExtension.password }
            registry.add("spring.datasource.hikari.maximum-pool-size") { POOL_SIZE.toString() }
            registry.add("spring.jpa.hibernate.ddl-auto") { "none" }
        }
    }
}
