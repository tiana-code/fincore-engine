// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.api.mapper

import com.fincore.compliance.api.dto.request.CreateKycSessionRequest
import com.fincore.compliance.api.dto.response.KycSessionResponse
import com.fincore.compliance.application.kyc.InitiateKycSessionCommand
import com.fincore.compliance.domain.KycSession
import org.springframework.stereotype.Component

// Hand-written: the value-class KycSessionId crosses the wire as a prefixed-ULID string.
@Component
class KycApiMapper {
    fun toCommand(
        request: CreateKycSessionRequest,
        idempotencyKey: String,
    ): InitiateKycSessionCommand = InitiateKycSessionCommand(idempotencyKey = idempotencyKey, subjectReference = request.subjectReference)

    fun toResponse(session: KycSession): KycSessionResponse =
        KycSessionResponse(
            id = session.id.toString(),
            subjectReference = session.subjectReference,
            status = session.status.name,
        )
}
