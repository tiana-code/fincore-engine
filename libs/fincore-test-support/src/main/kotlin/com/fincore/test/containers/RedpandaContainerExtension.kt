// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.test.containers

import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.testcontainers.redpanda.RedpandaContainer

class RedpandaContainerExtension : BeforeAllCallback {
    override fun beforeAll(context: ExtensionContext?) {
        ensureStarted()
    }

    companion object {
        private const val IMAGE = "redpandadata/redpanda:v24.3.1"

        @Volatile
        private var container: RedpandaContainer? = null

        @Synchronized
        fun ensureStarted(): RedpandaContainer {
            val existing = container
            if (existing != null && existing.isRunning) return existing
            val fresh = RedpandaContainer(IMAGE).withReuse(true)
            fresh.start()
            container = fresh
            return fresh
        }

        val bootstrapServers: String get() = ensureStarted().bootstrapServers
    }
}
