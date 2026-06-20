// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.infrastructure.kyc

import com.fincore.compliance.application.kyc.KycCheckRequest
import com.fincore.compliance.application.kyc.KycCheckResult
import com.fincore.compliance.application.kyc.KycProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

// In-tree sandbox provider, off by default; the outcome is a deterministic function of markers in the subject reference.
@Component
@ConditionalOnProperty(
    prefix = "fincore.compliance.kyc.sandbox",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = false,
)
class SandboxKycProvider : KycProvider {
    override fun check(request: KycCheckRequest): KycCheckResult {
        val reference = request.subjectReference
        val providerReference = "sbx-kyc-${reference.hashCode().toUInt()}"
        return when {
            reference.contains(REJECT_MARKER, ignoreCase = true) ->
                KycCheckResult.Rejected("sandbox rejected by subject marker")
            reference.contains(PENDING_MARKER, ignoreCase = true) ->
                KycCheckResult.Pending(providerReference)
            reference.contains(INSUFFICIENT_MARKER, ignoreCase = true) ->
                KycCheckResult.InsufficientData(listOf("sandbox.insufficient"))
            else -> KycCheckResult.Approved(providerReference)
        }
    }

    private companion object {
        const val REJECT_MARKER = "reject"
        const val PENDING_MARKER = "pending"
        const val INSUFFICIENT_MARKER = "insufficient"
    }
}
