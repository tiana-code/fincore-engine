// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.events

import java.time.Instant
import java.util.UUID

data class EventEnvelope<T>(
    val id: UUID,
    val source: String,
    val specversion: String,
    val type: String,
    val time: Instant,
    val subject: String?,
    val datacontenttype: String,
    val correlationid: String?,
    val causationid: String?,
    val data: T,
) {
    init {
        require(source.isNotBlank()) { "source must not be blank" }
        require(type.isNotBlank()) { "type must not be blank" }
        require(specversion == SPEC_VERSION) { "specversion must be $SPEC_VERSION" }
        require(datacontenttype.isNotBlank()) { "datacontenttype must not be blank" }
    }

    companion object {
        const val SPEC_VERSION = "1.0"
        const val DEFAULT_CONTENT_TYPE = "application/json"

        fun <T> of(
            source: String,
            type: EventType,
            data: T,
            subject: String? = null,
            correlationId: String? = null,
            causationId: String? = null,
            datacontenttype: String = DEFAULT_CONTENT_TYPE,
        ): EventEnvelope<T> =
            EventEnvelope(
                id = UUID.randomUUID(),
                source = source,
                specversion = SPEC_VERSION,
                type = type.fullType,
                time = Instant.now(),
                subject = subject,
                datacontenttype = datacontenttype,
                correlationid = correlationId,
                causationid = causationId,
                data = data,
            )
    }
}
