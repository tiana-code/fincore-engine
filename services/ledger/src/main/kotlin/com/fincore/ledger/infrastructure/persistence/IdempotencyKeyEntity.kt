// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(name = "idempotency_keys", schema = "platform")
class IdempotencyKeyEntity(
    @Id
    @Column(name = "key_hash", nullable = false, updatable = false)
    var keyHash: String,
    @Column(name = "request_hash", nullable = false)
    var requestHash: String,
    @Column(name = "status_code")
    var statusCode: Int?,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_body")
    var responseBody: String?,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,
    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant,
)
