// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.api

import com.fincore.core.AccountId
import com.fincore.core.Currency
import com.fincore.core.Money
import com.fincore.ledger.api.idempotency.IdempotencyAttributes
import com.fincore.ledger.api.idempotency.IdempotencyFilter
import com.fincore.ledger.api.mapper.LedgerApiMapper
import com.fincore.ledger.application.AccountBalance
import com.fincore.ledger.application.AccountPage
import com.fincore.ledger.application.AccountService
import com.fincore.ledger.application.BalanceService
import com.fincore.ledger.application.CreateAccountCommand
import com.fincore.ledger.application.IdempotentResult
import com.fincore.ledger.config.SecurityConfig
import com.fincore.ledger.domain.Account
import com.fincore.ledger.domain.enum.AccountType
import com.fincore.ledger.domain.exception.AccountNotFoundException
import com.fincore.ledger.domain.exception.IdempotencyConflictException
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.matchesPattern
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.startsWith
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

@WebMvcTest(AccountController::class)
@Import(SecurityConfig::class, LedgerApiMapper::class, IdempotencyFilter::class, AccountControllerTest.Mocks::class)
class AccountControllerTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val accountService: AccountService,
    @Autowired private val balanceService: BalanceService,
    @Autowired private val idempotencyService: FakeIdempotencyService,
) {
    private val key = "k".repeat(40)

    @TestConfiguration
    class Mocks {
        @Bean fun accountService(): AccountService = mockk()

        @Bean fun balanceService(): BalanceService = mockk()

        @Bean fun idempotencyService(): FakeIdempotencyService = FakeIdempotencyService()

        @Bean fun jwtDecoder(): JwtDecoder = mockk()
    }

    @BeforeEach
    fun resetMocks() {
        idempotencyService.reset()
        clearMocks(accountService, balanceService)
    }

    @Test
    fun `should create an account and return 201 with prefixed id and location`() {
        val commandSlot = slot<CreateAccountCommand>()
        every { accountService.create(capture(commandSlot)) } returns
            Account(AccountId.generate(), "Operating cash", AccountType.USER_WALLET, Currency.EUR)

        mockMvc
            .perform(
                post("/v1/accounts")
                    .with(jwt().jwt { it.subject("user-123") })
                    .header(IdempotencyAttributes.HEADER, key)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"Operating cash","type":"USER_WALLET","currency":"EUR"}"""),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(matchesPattern("^acc_[0-9A-HJKMNP-TV-Z]{26}$")))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.currency").value("EUR"))
            .andExpect(header().string("Location", startsWith("/v1/accounts/acc_")))

        commandSlot.captured.actor shouldBe "user-123"
    }

    @Test
    fun `should return 404 when account is not found`() {
        val id = AccountId.generate()
        every { accountService.get(any()) } throws AccountNotFoundException(id)

        mockMvc
            .perform(get("/v1/accounts/$id").with(jwt()))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.detail", not(containsString("Exception"))))
    }

    @Test
    fun `should return 400 for a malformed account id without calling the service`() {
        mockMvc
            .perform(get("/v1/accounts/acc_zzz").with(jwt()))
            .andExpect(status().isBadRequest)

        verify(exactly = 0) { accountService.get(any()) }
    }

    @Test
    fun `should return the current balance in the account currency`() {
        val id = AccountId.generate()
        every { accountService.get(id) } returns Account(id, "Wallet", AccountType.USER_WALLET, Currency.EUR)
        every { balanceService.current(id, Currency.EUR) } returns
            AccountBalance(id, Money.of(BigDecimal("100.00"), Currency.EUR), Instant.parse("2026-06-13T10:00:00Z"))

        mockMvc
            .perform(get("/v1/accounts/$id/balance").with(jwt()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accountId").value(id.toString()))
            .andExpect(jsonPath("$.currency").value("EUR"))
            .andExpect(jsonPath("$.amount").value(100.00))
    }

    @Test
    fun `should replay the stored response without re-invoking the service`() {
        idempotencyService.handler = { _, _, _ -> IdempotentResult(201, """{"id":"acc_replayed"}""", replayed = true) }

        mockMvc
            .perform(
                post("/v1/accounts")
                    .with(jwt().jwt { it.subject("user-123") })
                    .header(IdempotencyAttributes.HEADER, key)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"Operating cash","type":"USER_WALLET","currency":"EUR"}"""),
            ).andExpect(status().isCreated)
            .andExpect(header().doesNotExist("Location"))

        verify(exactly = 0) { accountService.create(any()) }
    }

    @Test
    fun `should return 409 on idempotency key conflict`() {
        idempotencyService.handler = { _, _, _ -> throw IdempotencyConflictException() }

        mockMvc
            .perform(
                post("/v1/accounts")
                    .with(jwt().jwt { it.subject("user-123") })
                    .header(IdempotencyAttributes.HEADER, key)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"Operating cash","type":"USER_WALLET","currency":"EUR"}"""),
            ).andExpect(status().isConflict)
    }

    @Test
    fun `should return 400 when the idempotency key header is missing`() {
        mockMvc
            .perform(
                post("/v1/accounts")
                    .with(jwt().jwt { it.subject("user-123") })
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"Operating cash","type":"USER_WALLET","currency":"EUR"}"""),
            ).andExpect(status().isBadRequest)

        verify(exactly = 0) { accountService.create(any()) }
    }

    @Test
    fun `should reject an unauthenticated request with 401`() {
        mockMvc
            .perform(get("/v1/accounts/${AccountId.generate()}"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should return 400 for an invalid currency code`() {
        mockMvc
            .perform(
                post("/v1/accounts")
                    .with(jwt().jwt { it.subject("user-123") })
                    .header(IdempotencyAttributes.HEADER, key)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"Operating cash","type":"USER_WALLET","currency":"euro"}"""),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `should list accounts as a page`() {
        every { accountService.list(0, 20) } returns
            AccountPage(
                items =
                    listOf(
                        Account(AccountId.generate(), "Cash", AccountType.ASSET, Currency.EUR),
                        Account(AccountId.generate(), "Wallet", AccountType.USER_WALLET, Currency.USD),
                    ),
                page = 0,
                size = 20,
                totalElements = 2,
                totalPages = 1,
            )

        mockMvc
            .perform(get("/v1/accounts?page=0&size=20").with(jwt()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].id").value(matchesPattern("^acc_[0-9A-HJKMNP-TV-Z]{26}$")))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.totalElements").value(2))
    }

    @Test
    fun `should reject an oversized page request with 400`() {
        mockMvc
            .perform(get("/v1/accounts?size=101").with(jwt()))
            .andExpect(status().isBadRequest)

        verify(exactly = 0) { accountService.list(any(), any()) }
    }

    @Test
    fun `should reject a negative page index with 400`() {
        mockMvc
            .perform(get("/v1/accounts?page=-1").with(jwt()))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should reject an unauthenticated list request with 401`() {
        mockMvc.perform(get("/v1/accounts")).andExpect(status().isUnauthorized)
    }
}
