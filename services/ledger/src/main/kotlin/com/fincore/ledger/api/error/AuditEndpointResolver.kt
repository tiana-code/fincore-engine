// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.api.error

import com.fincore.ledger.domain.enum.AuditAction
import com.fincore.ledger.domain.enum.AuditResourceType
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component

data class AuditedEndpoint(
    val action: AuditAction,
    val resourceType: AuditResourceType,
    val resourceId: String,
)

@Component
class AuditEndpointResolver {
    fun resolve(
        method: String,
        uri: String,
    ): AuditedEndpoint? {
        if (method != HttpMethod.POST.name()) return null
        REVERSE_PATH.matchEntire(uri)?.let {
            return AuditedEndpoint(AuditAction.TRANSACTION_REVERSE, AuditResourceType.TRANSACTION, it.groupValues[1])
        }
        return when (uri) {
            ACCOUNTS_PATH -> AuditedEndpoint(AuditAction.ACCOUNT_CREATE, AuditResourceType.ACCOUNT, NOT_YET_CREATED)
            TRANSACTIONS_PATH -> AuditedEndpoint(AuditAction.TRANSACTION_POST, AuditResourceType.TRANSACTION, NOT_YET_CREATED)
            else -> null
        }
    }

    private companion object {
        const val ACCOUNTS_PATH = "/v1/accounts"
        const val TRANSACTIONS_PATH = "/v1/transactions"
        const val NOT_YET_CREATED = "unknown"
        val REVERSE_PATH = Regex("/v1/transactions/([^/]+)/reverse")
    }
}
