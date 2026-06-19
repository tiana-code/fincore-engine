// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.kyc

import com.fincore.compliance.domain.KycSession
import com.fincore.compliance.domain.enum.KycStatus
import com.fincore.compliance.infrastructure.persistence.KycSessionPersistenceAdapter
import com.fincore.core.KycSessionId
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Instant

class KycSessionPersistenceAdapterTest {
    private val adapter = KycSessionPersistenceAdapter()

    @Test
    fun `should round-trip a session through new entity and back`() {
        val session = KycSession(KycSessionId.generate(), "subject-1")

        val domain = adapter.toDomain(adapter.toNewEntity(session, Instant.now()))

        domain.id shouldBe session.id
        domain.subjectReference shouldBe "subject-1"
        domain.status shouldBe KycStatus.INITIATED
    }
}
