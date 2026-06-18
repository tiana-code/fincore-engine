// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.api

import com.fincore.core.PaymentId
import com.fincore.payments.api.dto.request.InitiatePaymentRequest
import com.fincore.payments.api.dto.response.PaymentResponse
import com.fincore.payments.api.mapper.PaymentApiMapper
import com.fincore.payments.application.PaymentService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@Tag(name = "Payments", description = "Initiate, read, and cancel payments")
@RestController
@RequestMapping("/v1/payments")
class PaymentController(
    private val paymentService: PaymentService,
    private val mapper: PaymentApiMapper,
) {
    @Operation(summary = "Initiate a payment", description = "Creates a payment; idempotent on the Idempotency-Key header.")
    @PostMapping
    fun initiate(
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @Valid @RequestBody request: InitiatePaymentRequest,
    ): ResponseEntity<PaymentResponse> {
        val response = mapper.toResponse(paymentService.initiate(mapper.toCommand(request, idempotencyKey)))
        return ResponseEntity.created(URI.create("/v1/payments/${response.id}")).body(response)
    }

    @Operation(summary = "Get a payment", description = "Returns the payment or 404.")
    @GetMapping("/{id}")
    fun get(
        @PathVariable id: String,
    ): PaymentResponse = mapper.toResponse(paymentService.get(PaymentId.fromString(id)))

    @Operation(summary = "Cancel a payment", description = "Cancels a non-terminal payment, or 409 if illegal.")
    @PostMapping("/{id}/cancel")
    fun cancel(
        @PathVariable id: String,
    ): PaymentResponse = mapper.toResponse(paymentService.cancel(PaymentId.fromString(id)))
}
