// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.payments.infrastructure.persistence

import com.fincore.events.OutboxStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "outbox_events", schema = "payments")
@Suppress("LongParameterList")
class PaymentOutboxEventEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID,
    @Column(name = "aggregate_type", nullable = false, updatable = false)
    var aggregateType: String,
    @Column(name = "aggregate_id", nullable = false, updatable = false)
    var aggregateId: String,
    @Column(name = "event_type", nullable = false, updatable = false)
    var eventType: String,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, updatable = false)
    var payload: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: OutboxStatus,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant,
    @Column(name = "published_at")
    var publishedAt: Instant?,
    @Column(name = "attempts", nullable = false)
    var attempts: Int,
    @Column(name = "last_error")
    var lastError: String?,
    @Column(name = "leased_at")
    var leasedAt: Instant? = null,
)
