// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.kyc

import com.fincore.compliance.domain.KycSession
import com.fincore.core.KycSessionId
import org.springframework.stereotype.Service

/**
 * NOT transactional: each persisted transition is its own short transaction on [KycService], and the external
 * [KycProvider.check] runs between them, never inside a transaction. A [KycProviderException], Pending, or
 * InsufficientData leaves the session in SCREENING for a later re-screen; concurrent calls resolve via the entity's
 * optimistic lock.
 */
@Service
class KycOrchestrator(
    private val kycService: KycService,
    private val kycProvider: KycProvider,
) {
    fun process(id: KycSessionId): KycSession {
        val screening = kycService.beginScreening(id)
        return when (kycProvider.check(KycCheckRequest(screening.subjectReference))) {
            is KycCheckResult.Approved -> kycService.approve(id)
            is KycCheckResult.Rejected -> kycService.reject(id)
            is KycCheckResult.Pending, is KycCheckResult.InsufficientData -> screening
        }
    }
}
