// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.infrastructure.persistence

import com.fincore.core.Currency
import com.fincore.core.Money
import com.fincore.core.PaymentId
import com.fincore.payments.domain.Payment
import org.springframework.stereotype.Component
import java.time.Instant

// Hand-written because MapStruct cannot construct the value-class domain aggregate; the domain stays pure.
@Component
class PaymentPersistenceAdapter {
    fun toDomain(entity: PaymentEntity): Payment =
        Payment(
            id = PaymentId(entity.id),
            amount = Money(entity.amount, Currency.of(entity.currency.trim())),
            reference = entity.reference,
            status = entity.status,
        )

    fun toNewEntity(
        payment: Payment,
        now: Instant,
    ): PaymentEntity =
        PaymentEntity(
            id = payment.id.value,
            reference = payment.reference,
            amount = payment.amount.amount,
            currency = payment.amount.currency.code,
            status = payment.status,
            createdAt = now,
            version = 0,
        )
}
