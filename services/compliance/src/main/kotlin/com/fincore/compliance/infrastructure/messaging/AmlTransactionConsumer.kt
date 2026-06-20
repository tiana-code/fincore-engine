// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.infrastructure.messaging

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fincore.compliance.application.aml.AmlEventHandler
import com.fincore.events.EventEnvelope
import com.fincore.events.LedgerEvents
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

// The ledger transaction topic carries several event types; only TransactionPosted is forwarded to the AML handler.
@Component
class AmlTransactionConsumer(
    private val objectMapper: ObjectMapper,
    private val handler: AmlEventHandler,
) {
    @KafkaListener(topics = ["fincore.transaction"], groupId = "compliance-aml")
    fun onMessage(payload: String) {
        val envelope = objectMapper.readValue(payload, ENVELOPE_TYPE)
        if (envelope.type == LedgerEvents.TransactionPosted.fullType) {
            handler.handle(envelope)
        }
    }

    private companion object {
        val ENVELOPE_TYPE = object : TypeReference<EventEnvelope<LedgerTransactionPosted>>() {}
    }
}
