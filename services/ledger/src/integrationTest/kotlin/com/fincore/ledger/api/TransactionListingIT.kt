// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fincore.core.TransactionId
import com.fincore.ledger.domain.enum.TransactionStatus
import com.fincore.ledger.infrastructure.persistence.TransactionEntity
import com.fincore.ledger.infrastructure.persistence.TransactionRepository
import com.fincore.test.containers.PostgresContainerExtension
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
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.time.Instant

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(PostgresContainerExtension::class)
@Import(TransactionListingIT.TestSecurity::class)
class TransactionListingIT(
    @Autowired private val rest: TestRestTemplate,
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val transactionRepository: TransactionRepository,
) {
    @TestConfiguration
    class TestSecurity {
        @Bean
        fun jwtDecoder(): JwtDecoder =
            JwtDecoder { token ->
                Jwt
                    .withTokenValue(token)
                    .header("alg", "none")
                    .subject("tx-list-it")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(EXPIRY_SECONDS))
                    .build()
            }
    }

    private fun persist(
        reference: String,
        postedAt: Instant,
    ) {
        transactionRepository.saveAndFlush(
            TransactionEntity(
                id = TransactionId.generate().value,
                reference = reference,
                description = null,
                status = TransactionStatus.POSTED,
                reversesId = null,
                metadata = "{}",
                postedAt = postedAt,
                createdAt = postedAt,
                createdBy = "tx-list-it",
            ),
        )
    }

    @Test
    fun `should return transactions newest first across the page`() {
        val base = Instant.parse("2026-06-13T00:00:00Z")
        val references = listOf("tx-list-it-a", "tx-list-it-b", "tx-list-it-c")
        references.forEachIndexed { i, reference -> persist(reference, base.plusSeconds(i.toLong())) }

        val headers = HttpHeaders().apply { setBearerAuth("list-token") }
        val response = rest.exchange("/v1/transactions?page=0&size=100", HttpMethod.GET, HttpEntity<Void>(headers), String::class.java)

        response.statusCode.value() shouldBe 200
        val tree = objectMapper.readTree(response.body)
        (tree.get("totalElements").asLong() >= 3) shouldBe true
        val ordered = tree.get("items").map { it.get("reference").asText() }.filter { it in references }
        ordered shouldBe listOf("tx-list-it-c", "tx-list-it-b", "tx-list-it-a")
        tree
            .get("items")
            .first()
            .get("id")
            .asText()
            .startsWith("tx_") shouldBe true
        tree
            .get("items")
            .first()
            .get("status")
            .asText() shouldBe "POSTED"
    }

    @Test
    fun `should return disjoint slices across successive pages`() {
        val base = Instant.parse("2026-06-14T00:00:00Z")
        (0 until 5).forEach { i -> persist("tx-list-it-page-$i", base.plusSeconds(i.toLong())) }

        val headers = HttpHeaders().apply { setBearerAuth("list-token") }
        val first = idsAndTimes("/v1/transactions?page=0&size=2", headers)
        val second = idsAndTimes("/v1/transactions?page=1&size=2", headers)

        (first.map { it.first }.intersect(second.map { it.first }.toSet())).isEmpty() shouldBe true
        val times = (first + second).map { it.second }
        times shouldBe times.sortedDescending()
    }

    private fun idsAndTimes(
        path: String,
        headers: HttpHeaders,
    ): List<Pair<String, String>> {
        val response = rest.exchange(path, HttpMethod.GET, HttpEntity<Void>(headers), String::class.java)
        response.statusCode.value() shouldBe 200
        return objectMapper.readTree(response.body).get("items").map { it.get("id").asText() to it.get("postedAt").asText() }
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
