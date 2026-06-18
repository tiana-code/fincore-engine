// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.infrastructure.persistence

import com.fincore.payments.domain.enum.PaymentStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "payments", schema = "payments")
@Suppress("LongParameterList")
class PaymentEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID,
    @Column(name = "reference", nullable = false, updatable = false)
    var reference: String,
    @Column(name = "amount", nullable = false, updatable = false)
    var amount: BigDecimal,
    @Column(name = "currency", nullable = false, updatable = false)
    var currency: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: PaymentStatus,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant,
    @Version
    @Column(name = "version", nullable = false)
    var version: Long,
)
