// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.observability

import ch.qos.logback.classic.spi.ILoggingEvent
import org.springframework.boot.json.JsonWriter
import org.springframework.boot.logging.structured.StructuredLoggingJsonMembersCustomizer
import java.util.function.UnaryOperator

class PiiMaskingMembersCustomizer : StructuredLoggingJsonMembersCustomizer<ILoggingEvent> {
    override fun customize(members: JsonWriter.Members<ILoggingEvent>) {
        members.applyingValueProcessor(
            JsonWriter.ValueProcessor
                .of(String::class.java, UnaryOperator { PiiMasker.mask(it) })
                .whenHasPath { it.name() !in SKIP_KEYS },
        )
    }

    private companion object {
        val SKIP_KEYS = setOf("correlation_id", "trace_id", "span_id")
    }
}
