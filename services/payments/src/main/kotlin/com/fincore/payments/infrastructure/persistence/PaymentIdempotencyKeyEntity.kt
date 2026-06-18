// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import java.time.Instant
import java.util.UUID

@Entity
@Immutable
@Table(name = "idempotency_keys", schema = "payments")
class PaymentIdempotencyKeyEntity(
    @Id
    @Column(name = "key_hash", nullable = false, updatable = false)
    var keyHash: String,
    @Column(name = "payment_id", nullable = false, updatable = false)
    var paymentId: UUID,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant,
)
