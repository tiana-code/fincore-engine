// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.api

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fincore.payments.api.dto.request.WebhookRequest
import com.fincore.payments.application.webhook.MalformedWebhookException
import com.fincore.payments.application.webhook.PaymentWebhookHandler
import com.fincore.payments.application.webhook.PaymentWebhookNotification
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Payment webhooks", description = "Inbound provider callbacks, authenticated by an HMAC signature")
@RestController
@RequestMapping("/v1/payments/webhooks")
class PaymentWebhookController(
    private val handler: PaymentWebhookHandler,
    private val objectMapper: ObjectMapper,
) {
    @Operation(
        summary = "Receive a payment webhook",
        description = "Verifies the signature over the raw body, then settles or fails the payment.",
    )
    @PostMapping
    fun receive(
        @RequestBody rawBody: String,
        @RequestHeader(SIGNATURE_HEADER) signature: String,
    ): ResponseEntity<Unit> {
        val request = parse(rawBody)
        handler.handle(rawBody, signature, PaymentWebhookNotification(request.deliveryId, request.providerReference, request.outcome))
        return ResponseEntity.ok().build()
    }

    private fun parse(rawBody: String): WebhookRequest =
        try {
            objectMapper.readValue(rawBody, WebhookRequest::class.java)
        } catch (ex: JsonProcessingException) {
            throw MalformedWebhookException(ex)
        }

    private companion object {
        const val SIGNATURE_HEADER = "X-Webhook-Signature"
    }
}
