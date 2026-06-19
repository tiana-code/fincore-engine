// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.api.mapper

import com.fincore.core.Currency
import com.fincore.core.Money
import com.fincore.payments.api.dto.request.InitiatePaymentRequest
import com.fincore.payments.api.dto.response.PageResponse
import com.fincore.payments.api.dto.response.PaymentResponse
import com.fincore.payments.application.InitiatePaymentCommand
import com.fincore.payments.application.PaymentPage
import com.fincore.payments.domain.Payment
import org.springframework.stereotype.Component

// Hand-written: MapStruct cannot construct the value-class Money/PaymentId; ids cross the wire as prefixed strings.
@Component
class PaymentApiMapper {
    fun toCommand(
        request: InitiatePaymentRequest,
        idempotencyKey: String,
    ): InitiatePaymentCommand =
        InitiatePaymentCommand(
            idempotencyKey = idempotencyKey,
            amount = Money(request.amount, Currency.of(request.currency)),
            reference = request.reference,
        )

    fun toResponse(payment: Payment): PaymentResponse =
        PaymentResponse(
            id = payment.id.toString(),
            reference = payment.reference,
            amount = payment.amount.amount,
            currency = payment.amount.currency.code,
            status = payment.status.name,
        )

    fun toPageResponse(page: PaymentPage): PageResponse<PaymentResponse> =
        PageResponse(
            items = page.items.map(::toResponse),
            page = page.page,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
        )
}
