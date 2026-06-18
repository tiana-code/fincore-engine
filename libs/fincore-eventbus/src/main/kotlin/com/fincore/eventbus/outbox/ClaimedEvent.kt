// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.eventbus.outbox

import java.util.UUID

data class ClaimedEvent(
    val id: UUID,
    val aggregateType: String,
    val aggregateId: String,
    val eventType: String,
    val payload: String,
    val attempts: Int,
)

data class DispatchSummary(
    val published: Int,
    val failed: Int,
)
