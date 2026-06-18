// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.api

import com.fincore.payments.application.webhook.PaymentWebhookHandler
import com.fincore.payments.application.webhook.PaymentWebhookNotification
import com.fincore.payments.application.webhook.WebhookOutcome
import com.fincore.payments.application.webhook.WebhookResult
import com.fincore.payments.application.webhook.WebhookSignatureException
import com.fincore.payments.config.SecurityConfig
import com.fincore.payments.exception.GlobalExceptionHandler
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(PaymentWebhookController::class)
@Import(SecurityConfig::class, GlobalExceptionHandler::class, PaymentWebhookControllerTest.Mocks::class)
class PaymentWebhookControllerTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val handler: PaymentWebhookHandler,
) {
    @TestConfiguration
    class Mocks {
        @Bean fun paymentWebhookHandler(): PaymentWebhookHandler = mockk()

        @Bean fun jwtDecoder(): JwtDecoder = mockk()
    }

    @BeforeEach
    fun resetMocks() {
        clearMocks(handler)
    }

    @Test
    fun `should accept a signed webhook without a bearer token and forward the parsed notification`() {
        val notification = slot<PaymentWebhookNotification>()
        every { handler.handle(any(), any(), capture(notification)) } returns WebhookResult.Processed

        mockMvc
            .perform(
                post("/v1/payments/webhooks")
                    .header(SIGNATURE_HEADER, "sig")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY),
            ).andExpect(status().isOk)

        verify { handler.handle(VALID_BODY, "sig", any()) }
        notification.captured.deliveryId shouldBe "d-1"
        notification.captured.providerReference shouldBe "prov-1"
        notification.captured.outcome shouldBe WebhookOutcome.SETTLED
    }

    @Test
    fun `should return 401 when the signature is invalid`() {
        every { handler.handle(any(), any(), any()) } throws WebhookSignatureException("invalid webhook signature")

        mockMvc
            .perform(
                post("/v1/payments/webhooks")
                    .header(SIGNATURE_HEADER, "bad")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_BODY),
            ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `should return 400 when the body is malformed`() {
        mockMvc
            .perform(
                post("/v1/payments/webhooks")
                    .header(SIGNATURE_HEADER, "sig")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{not-json"),
            ).andExpect(status().isBadRequest)

        verify(exactly = 0) { handler.handle(any(), any(), any()) }
    }

    @Test
    fun `should return 400 when the signature header is missing`() {
        mockMvc
            .perform(post("/v1/payments/webhooks").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
            .andExpect(status().isBadRequest)
    }

    private companion object {
        const val SIGNATURE_HEADER = "X-Webhook-Signature"
        const val VALID_BODY = """{"deliveryId":"d-1","providerReference":"prov-1","outcome":"SETTLED"}"""
    }
}
