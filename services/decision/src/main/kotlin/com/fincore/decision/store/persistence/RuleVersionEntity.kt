// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.Immutable
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Immutable
@Table(name = "rule_versions", schema = "decision")
class RuleVersionEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    val id: UUID,
    @Column(name = "rule_id", nullable = false, updatable = false)
    val ruleId: UUID,
    @Column(name = "version_no", nullable = false, updatable = false)
    val versionNo: Int,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dsl", nullable = false, updatable = false)
    val dsl: String,
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null,
)
