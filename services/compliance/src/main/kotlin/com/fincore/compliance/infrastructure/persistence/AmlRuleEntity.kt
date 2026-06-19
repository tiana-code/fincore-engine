// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "aml_rules", schema = "compliance")
class AmlRuleEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID,
    @Column(name = "rule_key", nullable = false, updatable = false)
    var ruleKey: String,
    @Column(name = "enabled", nullable = false)
    var enabled: Boolean,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant,
)
