// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.kyc

import com.fincore.compliance.domain.KycSession
import com.fincore.core.KycSessionId

interface KycService {
    fun initiate(command: InitiateKycSessionCommand): KycSession

    fun get(id: KycSessionId): KycSession

    fun beginScreening(id: KycSessionId): KycSession

    fun approve(id: KycSessionId): KycSession

    fun reject(id: KycSessionId): KycSession
}
