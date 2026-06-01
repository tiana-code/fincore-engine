// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.domain.exception

open class DomainException(
    message: String,
) : RuntimeException(message) {
    init {
        require(message.isNotBlank()) { "DomainException message must not be blank" }
    }
}
