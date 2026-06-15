// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.api

import com.fincore.core.AccountId
import com.fincore.core.TransactionId
import com.fincore.ledger.api.idempotency.IdempotencyAttributes
import com.fincore.ledger.api.idempotency.IdempotencyFilter
import com.fincore.ledger.api.mapper.LedgerApiMapper
import com.fincore.ledger.application.EntryView
import com.fincore.ledger.application.PostTransactionCommand
import com.fincore.ledger.application.PostedTransaction
import com.fincore.ledger.application.TransactionDetail
import com.fincore.ledger.application.TransactionPage
import com.fincore.ledger.application.TransactionService
import com.fincore.ledger.application.TransactionSummary
import com.fincore.ledger.config.SecurityConfig
import com.fincore.ledger.domain.enum.EntryDirection
import com.fincore.ledger.domain.enum.TransactionStatus
import com.fincore.ledger.domain.exception.AccountNotFoundException
import com.fincore.ledger.domain.exception.ConcurrencyConflictException
import com.fincore.ledger.domain.exception.CurrencyConsistencyViolationException
import com.fincore.ledger.domain.exception.DoubleEntryViolationException
import com.fincore.ledger.domain.exception.DuplicateTransactionException
import com.fincore.ledger.domain.exception.TransactionAlreadyReversedException
import com.fincore.ledger.domain.exception.TransactionNotFoundException
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.hamcrest.Matchers.matchesPattern
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.Instant

@WebMvcTest(TransactionController::class)
@Import(SecurityConfig::class, LedgerApiMapper::class, IdempotencyFilter::class, TransactionControllerTest.Mocks::class)
class TransactionControllerTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val transactionService: TransactionService,
    @Autowired private val idempotencyService: FakeIdempotencyService,
) {
    private val key = "t".repeat(40)
    private val accountA = AccountId.generate()
    private val accountB = AccountId.generate()

    @TestConfiguration
    class Mocks {
        @Bean fun transactionService(): TransactionService = mockk()

        @Bean fun idempotencyService(): FakeIdempotencyService = FakeIdempotencyService()

        @Bean fun jwtDecoder(): JwtDecoder = mockk()
    }

    @BeforeEach
    fun resetMocks() {
        idempotencyService.reset()
        clearMocks(transactionService)
    }

    private fun balancedBody(): String =
        """
        {"reference":"tx-ref-1","currency":"EUR","entries":[
          {"accountId":"$accountA","direction":"DEBIT","amount":100.00},
          {"accountId":"$accountB","direction":"CREDIT","amount":-100.00}]}
        """.trimIndent()

    private fun postBalanced(correlationId: String? = null) =
        mockMvc.perform(
            post("/v1/transactions")
                .with(jwt().jwt { it.subject("user-123") })
                .header(IdempotencyAttributes.HEADER, key)
                .apply { if (correlationId != null) header("X-Correlation-Id", correlationId) }
                .contentType(MediaType.APPLICATION_JSON)
                .content(balancedBody()),
        )

    @Test
    fun `should post a balanced transaction and pass through signed amounts and actor`() {
        val commandSlot = slot<PostTransactionCommand>()
        every { transactionService.post(capture(commandSlot)) } returns
            PostedTransaction(TransactionId.generate(), "tx-ref-1", TransactionStatus.POSTED, Instant.parse("2026-06-13T10:00:00Z"))

        postBalanced(correlationId = "corr-1")
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value("POSTED"))
            .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/v1/transactions/tx_")))

        val command = commandSlot.captured
        command.actor shouldBe "user-123"
        command.correlationId shouldBe "corr-1"
        command.currency.code shouldBe "EUR"
        command.entries[0].accountId shouldBe accountA
        command.entries[0].direction shouldBe EntryDirection.DEBIT
        command.entries[0].amount.compareTo(BigDecimal("100.00")) shouldBe 0
        command.entries[1].amount.compareTo(BigDecimal("-100.00")) shouldBe 0
    }

    @Test
    fun `should map a double-entry violation to 422`() {
        every { transactionService.post(any()) } throws DoubleEntryViolationException("net != 0")
        postBalanced().andExpect(status().isUnprocessableEntity)
    }

    @Test
    fun `should map a currency consistency violation to 422`() {
        every { transactionService.post(any()) } throws CurrencyConsistencyViolationException("mismatch")
        postBalanced().andExpect(status().isUnprocessableEntity)
    }

    @Test
    fun `should map a duplicate reference to 409`() {
        every { transactionService.post(any()) } throws DuplicateTransactionException("tx-ref-1")
        postBalanced().andExpect(status().isConflict)
    }

    @Test
    fun `should map a concurrency conflict to 503 with retry-after`() {
        every { transactionService.post(any()) } throws ConcurrencyConflictException(RuntimeException("lock"))
        postBalanced()
            .andExpect(status().isServiceUnavailable)
            .andExpect(header().string("Retry-After", "1"))
    }

    @Test
    fun `should map a missing account to 404`() {
        every { transactionService.post(any()) } throws AccountNotFoundException(accountA)
        postBalanced().andExpect(status().isNotFound)
    }

    @Test
    fun `should reject a single-entry transaction with 400 before calling the service`() {
        mockMvc
            .perform(
                post("/v1/transactions")
                    .with(jwt().jwt { it.subject("user-123") })
                    .header(IdempotencyAttributes.HEADER, key)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"reference":"r","currency":"EUR","entries":[{"accountId":"$accountA","direction":"DEBIT","amount":1}]}"""),
            ).andExpect(status().isBadRequest)
    }

    private fun summary(
        reference: String,
        status: TransactionStatus,
    ) = TransactionSummary(TransactionId.generate(), reference, status, Instant.parse("2026-06-13T10:00:00Z"))

    @Test
    fun `should list transactions as a page with prefixed ids and status`() {
        every { transactionService.list(0, 20) } returns
            TransactionPage(
                items = listOf(summary("tx-ref-2", TransactionStatus.POSTED), summary("tx-ref-1", TransactionStatus.REVERSED)),
                page = 0,
                size = 20,
                totalElements = 2,
                totalPages = 1,
            )

        mockMvc
            .perform(get("/v1/transactions?page=0&size=20").with(jwt()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].id").value(matchesPattern("^tx_[0-9A-HJKMNP-TV-Z]{26}$")))
            .andExpect(jsonPath("$.items[0].status").value("POSTED"))
            .andExpect(jsonPath("$.items[1].status").value("REVERSED"))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.totalElements").value(2))
    }

    @Test
    fun `should return an empty page when no transactions exist`() {
        every { transactionService.list(0, 20) } returns TransactionPage(emptyList(), 0, 20, 0, 0)

        mockMvc
            .perform(get("/v1/transactions").with(jwt()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items").isEmpty)
            .andExpect(jsonPath("$.totalElements").value(0))
            .andExpect(jsonPath("$.totalPages").value(0))
    }

    @Test
    fun `should reject an oversized page request with 400`() {
        mockMvc
            .perform(get("/v1/transactions?size=101").with(jwt()))
            .andExpect(status().isBadRequest)

        verify(exactly = 0) { transactionService.list(any(), any()) }
    }

    @Test
    fun `should reject a zero page size with 400`() {
        mockMvc
            .perform(get("/v1/transactions?size=0").with(jwt()))
            .andExpect(status().isBadRequest)

        verify(exactly = 0) { transactionService.list(any(), any()) }
    }

    @Test
    fun `should reject a negative page index with 400`() {
        mockMvc
            .perform(get("/v1/transactions?page=-1").with(jwt()))
            .andExpect(status().isBadRequest)

        verify(exactly = 0) { transactionService.list(any(), any()) }
    }

    @Test
    fun `should reject an unauthenticated list request with 401`() {
        mockMvc.perform(get("/v1/transactions")).andExpect(status().isUnauthorized)
    }

    @Test
    fun `should return a transaction detail with entries as decimal strings and prefixed ids`() {
        val txId = TransactionId.generate()
        every { transactionService.get(any()) } returns
            TransactionDetail(
                id = txId,
                reference = "tx-ref-1",
                description = "a tx",
                status = TransactionStatus.POSTED,
                reversesId = null,
                postedAt = Instant.parse("2026-06-13T10:00:00Z"),
                entries =
                    listOf(
                        EntryView(accountA, EntryDirection.DEBIT, BigDecimal("100.00"), "EUR"),
                        EntryView(accountB, EntryDirection.CREDIT, BigDecimal("-100.00"), "EUR"),
                    ),
            )

        mockMvc
            .perform(get("/v1/transactions/$txId").with(jwt()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(txId.toString()))
            .andExpect(jsonPath("$.id").value(matchesPattern("^tx_[0-9A-HJKMNP-TV-Z]{26}$")))
            .andExpect(jsonPath("$.status").value("POSTED"))
            .andExpect(jsonPath("$.entries.length()").value(2))
            .andExpect(jsonPath("$.entries[0].accountId").value(accountA.toString()))
            .andExpect(jsonPath("$.entries[0].accountId").value(matchesPattern("^acc_[0-9A-HJKMNP-TV-Z]{26}$")))
            .andExpect(jsonPath("$.entries[0].amount").value("100.00"))
            .andExpect(jsonPath("$.entries[1].amount").value("-100.00"))
    }

    @Test
    fun `should return 404 when the transaction is not found`() {
        every { transactionService.get(any()) } throws TransactionNotFoundException(TransactionId.generate())

        mockMvc
            .perform(get("/v1/transactions/${TransactionId.generate()}").with(jwt()))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.status").value(404))
    }

    @Test
    fun `should return 400 for a malformed transaction id without calling the service`() {
        mockMvc
            .perform(get("/v1/transactions/tx_zzz").with(jwt()))
            .andExpect(status().isBadRequest)

        verify(exactly = 0) { transactionService.get(any()) }
    }

    private fun reverse(
        id: String,
        withKey: Boolean = true,
        authenticated: Boolean = true,
    ) = mockMvc.perform(
        post("/v1/transactions/$id/reverse")
            .apply { if (authenticated) with(jwt().jwt { it.subject("user-123") }) }
            .apply { if (withKey) header(IdempotencyAttributes.HEADER, key) }
            .contentType(MediaType.APPLICATION_JSON),
    )

    @Test
    fun `should reverse a transaction and return 201 with location and the compensating transaction`() {
        val compensatingId = TransactionId.generate()
        every { transactionService.reverse(any(), "user-123", any()) } returns
            PostedTransaction(compensatingId, "reversal-of-tx", TransactionStatus.POSTED, Instant.parse("2026-06-13T10:00:00Z"))

        reverse(TransactionId.generate().toString())
            .andExpect(status().isCreated)
            .andExpect(header().string("Location", "/v1/transactions/$compensatingId"))
            .andExpect(jsonPath("$.id").value(compensatingId.toString()))
            .andExpect(jsonPath("$.status").value("POSTED"))
    }

    @Test
    fun `should return 404 when reversing an unknown transaction`() {
        every { transactionService.reverse(any(), any(), any()) } throws TransactionNotFoundException(TransactionId.generate())

        reverse(TransactionId.generate().toString()).andExpect(status().isNotFound)
    }

    @Test
    fun `should return 409 when reversing an already reversed transaction`() {
        every { transactionService.reverse(any(), any(), any()) } throws
            TransactionAlreadyReversedException(TransactionId.generate())

        reverse(TransactionId.generate().toString()).andExpect(status().isConflict)
    }

    @Test
    fun `should reject a reverse without an idempotency key with 400`() {
        reverse(TransactionId.generate().toString(), withKey = false).andExpect(status().isBadRequest)

        verify(exactly = 0) { transactionService.reverse(any(), any(), any()) }
    }

    @Test
    fun `should reject an unauthenticated reverse with 401`() {
        reverse(TransactionId.generate().toString(), authenticated = false).andExpect(status().isUnauthorized)
    }
}
