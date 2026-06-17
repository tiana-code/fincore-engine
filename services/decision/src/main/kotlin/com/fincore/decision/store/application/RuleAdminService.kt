// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.application

import java.util.UUID

interface RuleAdminService {
    fun createRule(ruleKey: String): RuleView

    fun publishVersion(
        ruleKey: String,
        dslJson: String,
    ): VersionView

    fun getRule(ruleKey: String): RuleDetailView
}

data class RuleView(
    val id: UUID,
    val ruleKey: String,
)

data class VersionView(
    val id: UUID,
    val versionNo: Int,
)

data class RuleDetailView(
    val id: UUID,
    val ruleKey: String,
    val activeVersion: ActiveVersionView?,
)

data class ActiveVersionView(
    val versionNo: Int,
    val dsl: String,
)
