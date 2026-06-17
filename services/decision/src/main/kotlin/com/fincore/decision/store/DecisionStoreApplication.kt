// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DecisionStoreApplication

fun main(args: Array<String>) {
    runApplication<DecisionStoreApplication>(*args)
}
