// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.infrastructure.persistence

import com.fincore.ledger.domain.enum.AuditResult
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import java.time.Instant
import java.util.UUID

@Entity
@Immutable
@Table(name = "audit_events", schema = "platform")
@Suppress("LongParameterList")
class AuditEventEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID,
    @Column(name = "actor_id", nullable = false)
    var actorId: String,
    @Column(name = "correlation_id", nullable = false)
    var correlationId: String,
    @Column(name = "action", nullable = false)
    var action: String,
    @Column(name = "resource_type", nullable = false)
    var resourceType: String,
    @Column(name = "resource_id", nullable = false)
    var resourceId: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false)
    var result: AuditResult,
    @Column(name = "request_hash")
    var requestHash: String?,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,
)
