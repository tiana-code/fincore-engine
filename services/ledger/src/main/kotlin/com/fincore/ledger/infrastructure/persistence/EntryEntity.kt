// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.infrastructure.persistence

import com.fincore.ledger.domain.enum.EntryDirection
import jakarta.persistence.Column
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Immutable
@Table(name = "entries", schema = "ledger")
@Suppress("LongParameterList")
class EntryEntity(
    @EmbeddedId
    var key: EntryKey,
    @Column(name = "transaction_id", nullable = false, updatable = false)
    var transactionId: UUID,
    @Column(name = "account_id", nullable = false, updatable = false)
    var accountId: UUID,
    @Column(name = "amount", nullable = false, updatable = false, precision = 38, scale = 18)
    var amount: BigDecimal,
    @Column(name = "currency", nullable = false, updatable = false)
    var currency: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, updatable = false)
    var direction: EntryDirection,
    @Column(name = "posted_at", nullable = false, updatable = false)
    var postedAt: Instant,
)
