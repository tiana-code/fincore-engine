// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.api.security

import org.springframework.security.core.context.SecurityContextHolder

object CurrentActor {
    private const val ANONYMOUS = "anonymousUser"

    fun resolveOrNull(): String? {
        val authentication = SecurityContextHolder.getContext().authentication ?: return null
        if (!authentication.isAuthenticated || authentication.name == ANONYMOUS) return null
        return authentication.name
    }
}
