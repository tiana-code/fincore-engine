// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import java.time.Instant
import java.util.UUID

@Entity
@Immutable
@Table(name = "idempotency_keys", schema = "compliance")
class KycIdempotencyKeyEntity(
    @Id
    @Column(name = "key_hash", nullable = false, updatable = false)
    var keyHash: String,
    @Column(name = "kyc_session_id", nullable = false, updatable = false)
    var kycSessionId: UUID,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant,
)
