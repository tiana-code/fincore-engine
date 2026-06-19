// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.domain

import com.fincore.core.AmlRuleId

class AmlRule(
    val id: AmlRuleId,
    ruleKey: String,
    val enabled: Boolean = true,
) {
    val ruleKey: String = RuleKey.validate(ruleKey, "AML ruleKey")
}
