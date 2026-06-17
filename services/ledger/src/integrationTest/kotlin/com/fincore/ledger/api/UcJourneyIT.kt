// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.api

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fincore.core.AccountId
import com.fincore.core.TransactionId
import com.fincore.ledger.api.idempotency.IdempotencyAttributes
import com.fincore.ledger.api.observability.CorrelationIdAttributes
import com.fincore.ledger.domain.enum.AccountStatus
import com.fincore.ledger.domain.enum.AccountType
import com.fincore.ledger.domain.enum.EntryDirection
import com.fincore.ledger.domain.enum.TransactionStatus
import com.fincore.ledger.infrastructure.persistence.AccountRepository
import com.fincore.ledger.infrastructure.persistence.EntryRepository
import com.fincore.ledger.infrastructure.persistence.OutboxEventRepository
import com.fincore.ledger.infrastructure.persistence.TransactionRepository
import com.fincore.test.containers.PostgresContainerExtension
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
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
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(PostgresContainerExtension::class)
@Import(UcJourneyIT.ScopeFromTokenSecurity::class)
class UcJourneyIT(
    @Autowired private val rest: TestRestTemplate,
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val accountRepository: AccountRepository,
    @Autowired private val transactionRepository: TransactionRepository,
    @Autowired private val entryRepository: EntryRepository,
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
                    .subject("uc-journey-it")
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
    fun `should drive the full ledger journey over http and persist the expected rows`() {
        val debitId = createAccount("uc-debit")
        val creditId = createAccount("uc-credit")
        val transactionId = postTransaction("uc-ref-${UUID.randomUUID()}", debitId, creditId)
        assertBalance(debitId, BigDecimal(AMOUNT))
        assertBalance(creditId, BigDecimal(AMOUNT_NEG))
        assertSingleDebitEntry(debitId, transactionId)
        reverseTransaction(transactionId, debitId, creditId)
    }

    private fun createAccount(name: String): String {
        val body = """{"name":"$name","type":"USER_WALLET","currency":"EUR"}"""
        val response = rest.exchange("/v1/accounts", HttpMethod.POST, HttpEntity(body, writeHeaders()), String::class.java)
        response.statusCode shouldBe HttpStatus.CREATED
        val id = json(response)["id"].asText()
        id shouldStartWith "acc_"
        val entity = accountRepository.findById(AccountId.fromString(id).value).orElseThrow()
        entity.name shouldBe name
        entity.type shouldBe AccountType.USER_WALLET
        entity.currency shouldBe "EUR"
        entity.status shouldBe AccountStatus.ACTIVE
        return id
    }

    private fun postTransaction(
        reference: String,
        debitId: String,
        creditId: String,
    ): String {
        val body =
            """{"reference":"$reference","currency":"EUR","entries":[""" +
                """{"accountId":"$debitId","direction":"DEBIT","amount":"$AMOUNT"},""" +
                """{"accountId":"$creditId","direction":"CREDIT","amount":"$AMOUNT_NEG"}]}"""
        val response = rest.exchange("/v1/transactions", HttpMethod.POST, HttpEntity(body, writeHeaders()), String::class.java)
        response.statusCode shouldBe HttpStatus.CREATED
        val id = json(response)["id"].asText()
        id shouldStartWith "tx_"
        val uuid = TransactionId.fromString(id).value
        transactionRepository.findById(uuid).orElseThrow().status shouldBe TransactionStatus.POSTED
        val entries = entryRepository.findByTransactionId(uuid)
        entries shouldHaveSize 2
        entries.single { it.direction == EntryDirection.DEBIT }.amount.compareTo(BigDecimal(AMOUNT)) shouldBe 0
        entries.single { it.direction == EntryDirection.CREDIT }.amount.compareTo(BigDecimal(AMOUNT_NEG)) shouldBe 0
        entries.sumOf { it.amount }.compareTo(BigDecimal.ZERO) shouldBe 0
        outboxFor(id) shouldHaveSize 1
        return id
    }

    private fun assertBalance(
        accountId: String,
        expected: BigDecimal,
    ) {
        val response = rest.exchange("/v1/accounts/$accountId/balance", HttpMethod.GET, HttpEntity<Unit>(readHeaders()), String::class.java)
        response.statusCode shouldBe HttpStatus.OK
        val node = json(response)
        node["currency"].asText() shouldBe "EUR"
        BigDecimal(node["amount"].asText()).compareTo(expected) shouldBe 0
    }

    private fun assertSingleDebitEntry(
        accountId: String,
        transactionId: String,
    ) {
        val response = rest.exchange("/v1/accounts/$accountId/entries", HttpMethod.GET, HttpEntity<Unit>(readHeaders()), String::class.java)
        response.statusCode shouldBe HttpStatus.OK
        val items = json(response)["items"]
        items shouldHaveSize 1
        val entry = items.first()
        entry["direction"].asText() shouldBe "DEBIT"
        entry["transactionId"].asText() shouldBe transactionId
        BigDecimal(entry["amount"].asText()).compareTo(BigDecimal(AMOUNT)) shouldBe 0
    }

    private fun reverseTransaction(
        transactionId: String,
        debitId: String,
        creditId: String,
    ) {
        val response =
            rest.exchange("/v1/transactions/$transactionId/reverse", HttpMethod.POST, HttpEntity("{}", writeHeaders()), String::class.java)
        response.statusCode shouldBe HttpStatus.CREATED
        val compensatingId = json(response)["id"].asText()
        val originalUuid = TransactionId.fromString(transactionId).value
        transactionRepository.findById(originalUuid).orElseThrow().status shouldBe TransactionStatus.REVERSED
        val compensating = transactionRepository.findById(TransactionId.fromString(compensatingId).value).orElseThrow()
        compensating.status shouldBe TransactionStatus.POSTED
        compensating.reversesId shouldBe originalUuid
        assertBalance(debitId, BigDecimal.ZERO)
        assertBalance(creditId, BigDecimal.ZERO)
        outboxFor(compensatingId) shouldHaveSize 1
    }

    private fun outboxFor(transactionId: String) =
        outboxRepository.findAll().filter { it.aggregateId == transactionId && it.eventType == POSTED_EVENT }

    private fun json(response: ResponseEntity<String>): JsonNode = objectMapper.readTree(response.body)

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

    private companion object {
        const val EXPIRY_SECONDS = 3600L
        const val AMOUNT = "100.00"
        const val AMOUNT_NEG = "-100.00"
        const val POSTED_EVENT = "com.fincore.ledger.transaction.posted.v1"

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
