// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable
import java.time.Instant
import java.util.UUID

@Embeddable
data class EntryKey(
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
