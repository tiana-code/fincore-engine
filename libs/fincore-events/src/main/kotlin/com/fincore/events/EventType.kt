// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.events

sealed interface EventType {
    val typeName: String
    val schemaVersion: String

    val fullType: String get() = "$typeName.$schemaVersion"
}

object LedgerEvents {
    data object TransactionPosted : EventType {
        override val typeName = "com.fincore.ledger.transaction.posted"
        override val schemaVersion = "v1"
    }

    data object TransactionReversed : EventType {
        override val typeName = "com.fincore.ledger.transaction.reversed"
        override val schemaVersion = "v1"
    }

    data object AccountCreated : EventType {
        override val typeName = "com.fincore.ledger.account.created"
        override val schemaVersion = "v1"
    }

    data object AccountFrozen : EventType {
        override val typeName = "com.fincore.ledger.account.frozen"
        override val schemaVersion = "v1"
    }

    data object AccountClosed : EventType {
        override val typeName = "com.fincore.ledger.account.closed"
        override val schemaVersion = "v1"
    }
}

object PaymentEvents {
    data object PaymentInitiated : EventType {
        override val typeName = "com.fincore.payment.initiated"
        override val schemaVersion = "v1"
    }

    data object PaymentScreened : EventType {
        override val typeName = "com.fincore.payment.screened"
        override val schemaVersion = "v1"
    }

    data object PaymentSettled : EventType {
        override val typeName = "com.fincore.payment.settled"
        override val schemaVersion = "v1"
    }

    data object PaymentFailed : EventType {
        override val typeName = "com.fincore.payment.failed"
        override val schemaVersion = "v1"
    }

    data object PaymentCancelled : EventType {
        override val typeName = "com.fincore.payment.cancelled"
        override val schemaVersion = "v1"
    }
}

object ComplianceEvents {
    data object KycSessionStarted : EventType {
        override val typeName = "com.fincore.compliance.kyc.session.started"
        override val schemaVersion = "v1"
    }

    data object KycCompleted : EventType {
        override val typeName = "com.fincore.compliance.kyc.completed"
        override val schemaVersion = "v1"
    }

    data object AmlAlertRaised : EventType {
        override val typeName = "com.fincore.compliance.aml.alert.raised"
        override val schemaVersion = "v1"
    }

    data object CaseOpened : EventType {
        override val typeName = "com.fincore.compliance.case.opened"
        override val schemaVersion = "v1"
    }

    data object CaseResolved : EventType {
        override val typeName = "com.fincore.compliance.case.resolved"
        override val schemaVersion = "v1"
    }

    data object CaseEscalated : EventType {
        override val typeName = "com.fincore.compliance.case.escalated"
        override val schemaVersion = "v1"
    }
}

object DecisionEvents {
    data object DecisionEvaluated : EventType {
        override val typeName = "com.fincore.decision.evaluated"
        override val schemaVersion = "v1"
    }

    data object RulesetPublished : EventType {
        override val typeName = "com.fincore.decision.ruleset.published"
        override val schemaVersion = "v1"
    }

    data object RulesetReplayed : EventType {
        override val typeName = "com.fincore.decision.ruleset.replayed"
        override val schemaVersion = "v1"
    }
}
