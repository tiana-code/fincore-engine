// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.kyc

import com.fincore.compliance.domain.KycSession
import com.fincore.core.KycSessionId
import org.springframework.stereotype.Service

/**
 * Drives an initiated KYC session through screening. NOT transactional: each persisted transition is its own short
 * transaction on [KycService], and the external [KycProvider.check] runs between them, never inside a transaction
 * (CLAUDE.md 8.10). A [KycProviderException] propagates and leaves the session in SCREENING for a future re-attempt.
 *
 * Concurrent process calls on the same session resolve via the entity's optimistic lock: the loser's
 * OptimisticLockingFailureException propagates. Pending and InsufficientData leave the session in SCREENING (there is
 * no PENDING status); a later re-screen advances it. The provider reference is not persisted.
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
