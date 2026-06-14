// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.api

import com.fincore.core.AccountId
import com.fincore.core.TransactionId
import com.fincore.ledger.api.idempotency.IdempotencyAttributes
import com.fincore.ledger.api.idempotency.IdempotencyFilter
import com.fincore.ledger.api.mapper.LedgerApiMapper
import com.fincore.ledger.application.PostTransactionCommand
import com.fincore.ledger.application.PostedTransaction
import com.fincore.ledger.application.TransactionService
import com.fincore.ledger.config.SecurityConfig
import com.fincore.ledger.domain.enum.EntryDirection
import com.fincore.ledger.domain.exception.AccountNotFoundException
import com.fincore.ledger.domain.exception.ConcurrencyConflictException
import com.fincore.ledger.domain.exception.CurrencyConsistencyViolationException
import com.fincore.ledger.domain.exception.DoubleEntryViolationException
import com.fincore.ledger.domain.exception.DuplicateTransactionException
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
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
            PostedTransaction(TransactionId.generate(), "tx-ref-1", Instant.parse("2026-06-13T10:00:00Z"))

        postBalanced(correlationId = "corr-1")
            .andExpect(status().isCreated)
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
}
