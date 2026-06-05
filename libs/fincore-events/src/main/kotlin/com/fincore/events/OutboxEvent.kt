// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.events

import java.time.Instant
import java.util.UUID

data class OutboxEvent(
    val id: UUID,
    val aggregateType: String,
    val aggregateId: String,
    val eventType: String,
    val payload: String,
    val status: OutboxStatus,
    val createdAt: Instant,
    val publishedAt: Instant?,
    val attempts: Int,
    val lastError: String?,
)
