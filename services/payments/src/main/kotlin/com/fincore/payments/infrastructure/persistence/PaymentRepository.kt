// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.infrastructure.persistence

import com.fincore.payments.domain.enum.PaymentStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

interface PaymentRepository : JpaRepository<PaymentEntity, UUID> {
    fun findByProviderReference(providerReference: String): PaymentEntity?

    fun findByStatusAndCreatedAtBefore(
        status: PaymentStatus,
        createdAt: Instant,
    ): List<PaymentEntity>
}
