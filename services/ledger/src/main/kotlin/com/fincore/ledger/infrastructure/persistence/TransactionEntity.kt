// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.infrastructure.persistence

import com.fincore.ledger.domain.enum.TransactionStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Immutable
@Table(name = "transactions", schema = "ledger")
@Suppress("LongParameterList")
class TransactionEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID,
    @Column(name = "reference", nullable = false, updatable = false)
    var reference: String,
    @Column(name = "description", updatable = false)
    var description: String?,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: TransactionStatus,
    @Column(name = "reverses_id", updatable = false)
    var reversesId: UUID?,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false)
    var metadata: String,
    @Column(name = "posted_at", nullable = false, updatable = false)
    var postedAt: Instant,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant,
    @Column(name = "created_by", nullable = false, updatable = false)
    var createdBy: String,
)
