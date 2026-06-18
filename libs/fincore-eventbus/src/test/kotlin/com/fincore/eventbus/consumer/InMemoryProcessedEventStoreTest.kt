// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.eventbus.consumer

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class InMemoryProcessedEventStoreTest {
    private val store = InMemoryProcessedEventStore()

    @Test
    fun `should return first-seen then duplicate for the same key`() {
        val id = UUID.randomUUID()

        store.markIfFirstSeen(id, "group") shouldBe true
        store.markIfFirstSeen(id, "group") shouldBe false
    }

    @Test
    fun `should grant first-seen to exactly one thread when racing on the same key`() {
        val id = UUID.randomUUID()
        val successes = AtomicInteger(0)
        val start = CountDownLatch(1)
        val done = CountDownLatch(THREADS)
        val pool = Executors.newFixedThreadPool(THREADS)
        repeat(THREADS) {
            pool.submit {
                start.await()
                if (store.markIfFirstSeen(id, "group")) successes.incrementAndGet()
                done.countDown()
            }
        }

        start.countDown()
        done.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        pool.shutdown()

        successes.get() shouldBe 1
    }

    private companion object {
        const val THREADS = 16
        const val TIMEOUT_SECONDS = 10L
    }
}
