// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.infrastructure.persistence

import com.fincore.ledger.domain.enum.AccountStatus
import com.fincore.ledger.domain.enum.AccountType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "accounts", schema = "ledger")
@Suppress("LongParameterList")
class AccountEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID,
    @Column(name = "name", nullable = false)
    var name: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, updatable = false)
    var type: AccountType,
    @Column(name = "currency", nullable = false, updatable = false)
    var currency: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: AccountStatus,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false)
    var metadata: String,
    @Version
    @Column(name = "version", nullable = false)
    var version: Long,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant,
    @Column(name = "created_by", nullable = false, updatable = false)
    var createdBy: String,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
    @Column(name = "updated_by", nullable = false)
    var updatedBy: String,
)
