// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.case

import com.fincore.compliance.domain.ComplianceCase
import com.fincore.compliance.domain.enum.CaseStatus
import com.fincore.compliance.infrastructure.persistence.ComplianceCasePersistenceAdapter
import com.fincore.compliance.infrastructure.persistence.ComplianceCaseRepository
import com.fincore.core.ComplianceCaseId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class ComplianceCaseServiceImpl(
    private val repository: ComplianceCaseRepository,
    private val adapter: ComplianceCasePersistenceAdapter,
) : ComplianceCaseService {
    @Transactional
    override fun open(command: OpenCaseCommand): ComplianceCase {
        val case = ComplianceCase(ComplianceCaseId.generate(), command.reference)
        repository.saveAndFlush(adapter.toNewEntity(case, Instant.now()))
        return case
    }

    @Transactional(readOnly = true)
    override fun get(id: ComplianceCaseId): ComplianceCase =
        adapter.toDomain(repository.findById(id.value).orElseThrow { CaseNotFoundException(id) })

    @Transactional
    override fun claim(id: ComplianceCaseId): ComplianceCase = transition(id, CaseStatus.CLAIMED)

    @Transactional
    override fun resolve(id: ComplianceCaseId): ComplianceCase = transition(id, CaseStatus.RESOLVED)

    @Transactional
    override fun escalate(id: ComplianceCaseId): ComplianceCase = transition(id, CaseStatus.ESCALATED)

    @Transactional(readOnly = true)
    override fun list(status: CaseStatus): List<ComplianceCase> = repository.findByStatus(status).map(adapter::toDomain)

    private fun transition(
        id: ComplianceCaseId,
        target: CaseStatus,
    ): ComplianceCase {
        val entity = repository.findById(id.value).orElseThrow { CaseNotFoundException(id) }
        val case = adapter.toDomain(entity)
        case.transitionTo(target)
        entity.status = case.status
        repository.saveAndFlush(entity)
        return case
    }
}
