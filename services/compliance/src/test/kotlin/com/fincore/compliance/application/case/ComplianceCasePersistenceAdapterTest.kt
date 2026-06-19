// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.case

import com.fincore.compliance.domain.ComplianceCase
import com.fincore.compliance.domain.enum.CaseStatus
import com.fincore.compliance.infrastructure.persistence.ComplianceCasePersistenceAdapter
import com.fincore.core.ComplianceCaseId
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Instant

class ComplianceCasePersistenceAdapterTest {
    private val adapter = ComplianceCasePersistenceAdapter()

    @Test
    fun `should round-trip a case through new entity and back`() {
        val case = ComplianceCase(ComplianceCaseId.generate(), "case-ref-1")

        val domain = adapter.toDomain(adapter.toNewEntity(case, Instant.now()))

        domain.id shouldBe case.id
        domain.reference shouldBe "case-ref-1"
        domain.status shouldBe CaseStatus.OPEN
    }
}
