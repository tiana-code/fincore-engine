// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.infrastructure.persistence

import com.fincore.compliance.domain.KycSession
import com.fincore.core.KycSessionId
import org.springframework.stereotype.Component
import java.time.Instant

// Hand-written because MapStruct cannot construct the value-class domain aggregate; the domain stays pure.
@Component
class KycSessionPersistenceAdapter {
    fun toDomain(entity: KycSessionEntity): KycSession =
        KycSession(
            id = KycSessionId(entity.id),
            subjectReference = entity.subjectReference,
            status = entity.status,
        )

    fun toNewEntity(
        session: KycSession,
        now: Instant,
    ): KycSessionEntity =
        KycSessionEntity(
            id = session.id.value,
            subjectReference = session.subjectReference,
            status = session.status,
            createdAt = now,
            version = 0,
        )
}
