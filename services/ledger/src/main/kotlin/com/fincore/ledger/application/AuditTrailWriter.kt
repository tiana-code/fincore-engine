// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import com.fincore.ledger.domain.enum.AuditAction
import com.fincore.ledger.domain.enum.AuditResourceType
import com.fincore.ledger.domain.enum.AuditResult

@Suppress("LongParameterList")
data class AuditRecord(
    val actorId: String,
    val action: AuditAction,
    val resourceType: AuditResourceType,
    val resourceId: String,
    val requestHash: String?,
    val payload: Map<String, String>? = null,
)

interface AuditTrailWriter {
    fun record(record: AuditRecord)

    fun recordOutcome(
        record: AuditRecord,
        result: AuditResult,
    )
}
