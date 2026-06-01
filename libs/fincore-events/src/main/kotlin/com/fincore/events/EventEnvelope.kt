// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.events

import java.time.Instant
import java.util.UUID

data class EventEnvelope<T>(
    val id: UUID,
    val source: String,
    val type: String,
    val time: Instant,
    val subject: String?,
    val correlationId: String?,
    val causationId: String?,
    val schemaVersion: String,
    val data: T,
) {
    companion object {
        fun <T> of(
            source: String,
            type: EventType,
            data: T,
            subject: String? = null,
            correlationId: String? = null,
            causationId: String? = null,
        ): EventEnvelope<T> =
            EventEnvelope(
                id = UUID.randomUUID(),
                source = source,
                type = "${type.typeName}.${type.schemaVersion}",
                time = Instant.now(),
                subject = subject,
                correlationId = correlationId,
                causationId = causationId,
                schemaVersion = type.schemaVersion,
                data = data,
            )
    }
}
