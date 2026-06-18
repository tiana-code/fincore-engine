// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.api

import com.fincore.core.Currency
import com.fincore.core.Money
import com.fincore.core.PaymentId
import com.fincore.payments.api.mapper.PaymentApiMapper
import com.fincore.payments.application.PaymentConcurrencyException
import com.fincore.payments.application.PaymentService
import com.fincore.payments.config.SecurityConfig
import com.fincore.payments.domain.Payment
import com.fincore.payments.domain.enum.PaymentStatus
import com.fincore.payments.domain.exception.PaymentDomainException
import com.fincore.payments.domain.exception.PaymentNotFoundException
import com.fincore.payments.exception.GlobalExceptionHandler
import io.kotest.matchers.shouldNotBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal

@WebMvcTest(PaymentController::class)
@Import(SecurityConfig::class, PaymentApiMapper::class, GlobalExceptionHandler::class, PaymentControllerTest.Mocks::class)
class PaymentControllerTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val paymentService: PaymentService,
) {
    @TestConfiguration
    class Mocks {
        @Bean fun paymentService(): PaymentService = mockk()

        @Bean fun jwtDecoder(): JwtDecoder = mockk()
    }

    @BeforeEach
    fun resetMocks() {
        clearMocks(paymentService)
    }

    @Test
    fun `should initiate and return 201 when authorized with write scope`() {
        every { paymentService.initiate(any()) } returns payment(PaymentStatus.INITIATED)

        mockMvc
            .perform(
                post("/v1/payments")
                    .with(jwt().authorities(SimpleGrantedAuthority(WRITE)))
                    .header("Idempotency-Key", "key-1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value("INITIATED"))
    }

    @Test
    fun `should return 401 when initiating without a token`() {
        mockMvc
            .perform(
                post("/v1/payments").header("Idempotency-Key", "key-1").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY),
            ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `should return 403 when initiating with only the read scope`() {
        mockMvc
            .perform(
                post("/v1/payments")
                    .with(jwt().authorities(SimpleGrantedAuthority(READ)))
                    .header("Idempotency-Key", "key-1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `should return 400 when the idempotency key header is missing`() {
        mockMvc
            .perform(
                post("/v1/payments")
                    .with(jwt().authorities(SimpleGrantedAuthority(WRITE)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `should return 400 when the body is invalid`() {
        mockMvc
            .perform(
                post("/v1/payments")
                    .with(jwt().authorities(SimpleGrantedAuthority(WRITE)))
                    .header("Idempotency-Key", "key-1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"amount":-1,"currency":"usd","reference":""}"""),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `should return 503 when initiation races on the idempotency key`() {
        every { paymentService.initiate(any()) } throws PaymentConcurrencyException(RuntimeException("race"))

        mockMvc
            .perform(
                post("/v1/payments")
                    .with(jwt().authorities(SimpleGrantedAuthority(WRITE)))
                    .header("Idempotency-Key", "key-1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY),
            ).andExpect(status().isServiceUnavailable)
    }

    @Test
    fun `should return the payment when getting with read scope`() {
        val payment = payment(PaymentStatus.INITIATED)
        every { paymentService.get(payment.id) } returns payment

        mockMvc
            .perform(get("/v1/payments/${payment.id}").with(jwt().authorities(SimpleGrantedAuthority(READ))))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(payment.id.toString()))
    }

    @Test
    fun `should return 404 when getting an unknown payment`() {
        val id = PaymentId.generate()
        every { paymentService.get(any()) } throws PaymentNotFoundException(id)

        mockMvc
            .perform(get("/v1/payments/$id").with(jwt().authorities(SimpleGrantedAuthority(READ))))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should return 409 when cancelling a terminal payment`() {
        every { paymentService.cancel(any()) } throws PaymentDomainException("illegal transition")

        mockMvc
            .perform(post("/v1/payments/${PaymentId.generate()}/cancel").with(jwt().authorities(SimpleGrantedAuthority(WRITE))))
            .andExpect(status().isConflict)
    }

    @Test
    fun `should not reject the webhook path as unauthorized`() {
        val result =
            mockMvc
                .perform(post("/v1/payments/webhooks").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andReturn()

        result.response.status shouldNotBe UNAUTHORIZED
    }

    private fun payment(status: PaymentStatus): Payment =
        Payment(PaymentId.generate(), Money(BigDecimal("100.00"), Currency.USD), "order-1", status)

    private companion object {
        const val READ = "SCOPE_payments:read"
        const val WRITE = "SCOPE_payments:write"
        const val UNAUTHORIZED = 401
        const val VALID_BODY = """{"amount":100.00,"currency":"USD","reference":"order-1"}"""
    }
}
