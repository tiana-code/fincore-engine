// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.test.containers

import dasniko.testcontainers.keycloak.KeycloakContainer
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext

class KeycloakContainerExtension : BeforeAllCallback {
    override fun beforeAll(context: ExtensionContext?) {
        ensureStarted()
    }

    companion object {
        private const val IMAGE = "quay.io/keycloak/keycloak:26.6.1"

        @Volatile
        private var container: KeycloakContainer? = null

        @Synchronized
        fun ensureStarted(): KeycloakContainer {
            val existing = container
            if (existing != null && existing.isRunning) return existing
            val fresh =
                KeycloakContainer(IMAGE)
                    .withAdminUsername("admin")
                    .withAdminPassword("admin")
                    .withReuse(true)
            fresh.start()
            container = fresh
            return fresh
        }

        val authServerUrl: String get() = ensureStarted().authServerUrl
        val adminUsername: String get() = ensureStarted().adminUsername
        val adminPassword: String get() = ensureStarted().adminPassword
    }
}
