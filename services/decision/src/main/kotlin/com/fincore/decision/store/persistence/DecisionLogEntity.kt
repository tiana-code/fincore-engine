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
@Table(name = "decision_logs", schema = "decision")
@Suppress("LongParameterList")
class DecisionLogEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    val id: UUID,
    @Column(name = "evaluated_at", nullable = false, updatable = false)
    val evaluatedAt: Instant,
    @Column(name = "rule_version_id", nullable = false, updatable = false)
    val ruleVersionId: UUID,
    @Column(name = "input_hash", nullable = false, updatable = false)
    val inputHash: String,
    @Column(name = "matched", nullable = false, updatable = false)
    val matched: Boolean,
    @Column(name = "outcome_label", updatable = false)
    val outcomeLabel: String? = null,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reason_codes", updatable = false)
    val reasonCodes: String? = null,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "trace", nullable = false, updatable = false)
    val trace: String,
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null,
)
