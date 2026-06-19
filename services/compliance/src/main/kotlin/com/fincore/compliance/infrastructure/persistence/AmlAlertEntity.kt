// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.infrastructure.persistence

import com.fincore.compliance.domain.enum.AmlAlertStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "aml_alerts", schema = "compliance")
class AmlAlertEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID,
    @Column(name = "rule_key", nullable = false, updatable = false)
    var ruleKey: String,
    @Column(name = "subject_reference", nullable = false, updatable = false)
    var subjectReference: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: AmlAlertStatus,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant,
    @Version
    @Column(name = "version", nullable = false)
    var version: Long,
)
