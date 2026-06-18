// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import java.time.Instant

@Entity
@Immutable
@Table(name = "processed_webhooks", schema = "payments")
class ProcessedWebhookEntity(
    @Id
    @Column(name = "delivery_id", nullable = false, updatable = false)
    var deliveryId: String,
    @Column(name = "received_at", nullable = false, updatable = false)
    var receivedAt: Instant,
)
