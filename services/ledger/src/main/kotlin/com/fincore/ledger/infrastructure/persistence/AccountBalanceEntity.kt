// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "account_balances", schema = "ledger")
class AccountBalanceEntity(
    @EmbeddedId
    var key: AccountBalanceKey,
    @Column(name = "balance", nullable = false, precision = 38, scale = 18)
    var balance: BigDecimal,
    @Column(name = "last_posted_at", nullable = false)
    var lastPostedAt: Instant,
    @Version
    @Column(name = "version", nullable = false)
    var version: Long,
)
