// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.eventbus.consumer

import org.springframework.jdbc.core.JdbcTemplate
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

/**
 * Persistent dedup store over a `processed_events` table (see db/processed-events.sql). The claim is
 * an INSERT ... ON CONFLICT DO NOTHING that participates in the ambient transaction, so a failing
 * handler rolls it back and the event is retried.
 */
class JdbcProcessedEventStore(
    private val jdbcTemplate: JdbcTemplate,
    private val tableName: String = "processed_events",
) : ProcessedEventStore {
    init {
        require(tableName.matches(SAFE_IDENTIFIER)) { "tableName must be a simple SQL identifier" }
    }

    override fun markIfFirstSeen(
        envelopeId: UUID,
        consumerGroup: String,
    ): Boolean =
        jdbcTemplate.update(
            "INSERT INTO $tableName (envelope_id, consumer_group, processed_at) VALUES (?, ?, ?) " +
                "ON CONFLICT (envelope_id, consumer_group) DO NOTHING",
            envelopeId,
            consumerGroup,
            Timestamp.from(Instant.now()),
        ) == 1

    private companion object {
        val SAFE_IDENTIFIER = Regex("^[A-Za-z_][A-Za-z0-9_]*$")
    }
}
