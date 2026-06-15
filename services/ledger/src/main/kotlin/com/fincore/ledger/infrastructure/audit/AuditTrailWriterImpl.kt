// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.infrastructure.audit

import com.fasterxml.jackson.databind.ObjectMapper
import com.fincore.ledger.api.observability.CorrelationIdAttributes
import com.fincore.ledger.application.AuditRecord
import com.fincore.ledger.application.AuditTrailWriter
import com.fincore.ledger.domain.enum.AuditResult
import com.fincore.ledger.infrastructure.persistence.AuditEventEntity
import com.fincore.ledger.infrastructure.persistence.AuditEventRepository
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Instant
import java.util.UUID

@Component
class AuditTrailWriterImpl(
    private val auditRepository: AuditEventRepository,
    private val objectMapper: ObjectMapper,
) : AuditTrailWriter {
    override fun record(record: AuditRecord) {
        check(TransactionSynchronizationManager.isActualTransactionActive()) {
            "AuditTrailWriter.record must be called within an active transaction"
        }
        auditRepository.saveAndFlush(
            AuditEventEntity(
                id = UUID.randomUUID(),
                actorId = record.actorId,
                correlationId = resolveCorrelationId(),
                action = record.action.name,
                resourceType = record.resourceType.name,
                resourceId = record.resourceId,
                result = AuditResult.SUCCESS,
                requestHash = record.requestHash,
                createdAt = Instant.now(),
                payload = record.payload?.let { objectMapper.writeValueAsString(it) },
            ),
        )
    }

    private fun resolveCorrelationId(): String {
        val fromMdc = MDC.get(CorrelationIdAttributes.MDC_KEY)
        return if (fromMdc.isNullOrBlank()) UUID.randomUUID().toString() else fromMdc
    }
}
