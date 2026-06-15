// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.api.error

import com.fincore.ledger.domain.enum.AuditAction
import com.fincore.ledger.domain.enum.AuditResourceType
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class AuditEndpointResolverTest {
    private val resolver = AuditEndpointResolver()

    @Test
    fun `should map POST accounts to ACCOUNT_CREATE with sentinel resource id`() {
        resolver.resolve("POST", "/v1/accounts") shouldBe
            AuditedEndpoint(AuditAction.ACCOUNT_CREATE, AuditResourceType.ACCOUNT, "unknown")
    }

    @Test
    fun `should map POST transactions to TRANSACTION_POST with sentinel resource id`() {
        resolver.resolve("POST", "/v1/transactions") shouldBe
            AuditedEndpoint(AuditAction.TRANSACTION_POST, AuditResourceType.TRANSACTION, "unknown")
    }

    @Test
    fun `should map POST reverse to TRANSACTION_REVERSE with the path transaction id`() {
        resolver.resolve("POST", "/v1/transactions/tx_0001/reverse") shouldBe
            AuditedEndpoint(AuditAction.TRANSACTION_REVERSE, AuditResourceType.TRANSACTION, "tx_0001")
    }

    @Test
    fun `should return null for a GET transaction read`() {
        resolver.resolve("GET", "/v1/transactions/tx_0001").shouldBeNull()
    }

    @Test
    fun `should return null for a GET account read`() {
        resolver.resolve("GET", "/v1/accounts/acc_0001").shouldBeNull()
    }

    @Test
    fun `should return null for an unknown path`() {
        resolver.resolve("POST", "/v1/other").shouldBeNull()
    }
}
