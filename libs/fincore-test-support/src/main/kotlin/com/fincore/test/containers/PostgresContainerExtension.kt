// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.test.containers

import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.testcontainers.containers.PostgreSQLContainer

class PostgresContainerExtension : BeforeAllCallback {
    override fun beforeAll(context: ExtensionContext?) {
        ensureStarted()
    }

    companion object {
        private const val IMAGE = "postgres:17-alpine"

        @Volatile
        private var container: PostgreSQLContainer<*>? = null

        @Synchronized
        fun ensureStarted(): PostgreSQLContainer<*> {
            val existing = container
            if (existing != null && existing.isRunning) return existing
            val fresh =
                PostgreSQLContainer(IMAGE)
                    .withDatabaseName("fincore_test")
                    .withUsername("fincore")
                    .withPassword("fincore")
                    .withReuse(true)
            fresh.start()
            container = fresh
            return fresh
        }

        val jdbcUrl: String get() = ensureStarted().jdbcUrl
        val username: String get() = ensureStarted().username
        val password: String get() = ensureStarted().password
        val host: String get() = ensureStarted().host
        val port: Int get() = ensureStarted().firstMappedPort
    }
}
