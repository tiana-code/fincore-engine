// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.domain

import com.fincore.compliance.domain.enum.AmlAlertStatus
import com.fincore.core.AmlAlertId

class AmlAlert(
    val id: AmlAlertId,
    ruleKey: String,
    status: AmlAlertStatus = AmlAlertStatus.OPEN,
) {
    val ruleKey: String = RuleKey.validate(ruleKey, "AML alert ruleKey")

    var status: AmlAlertStatus = status
        private set

    fun transitionTo(target: AmlAlertStatus) {
        if (!status.canTransitionTo(target)) {
            throw ComplianceDomainException("Illegal AML alert status transition: $status -> $target for alert $id")
        }
        status = target
    }

    fun isTerminal(): Boolean = status.isTerminal()
}
