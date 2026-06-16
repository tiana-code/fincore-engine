// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger

import com.fincore.ledger.config.CleanupProperties
import com.fincore.ledger.config.IdempotencyProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(IdempotencyProperties::class, CleanupProperties::class)
class LedgerApplication

fun main(args: Array<String>) {
    runApplication<LedgerApplication>(*args)
}
