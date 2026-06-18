// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.eventbus.retry

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class RetryTopicNamingTest {
    private val naming = RetryTopicNaming(retrySuffix = "-retry", deadLetterSuffix = "-dlt")

    @Test
    fun `should append the dead-letter suffix to the base topic`() {
        naming.deadLetterTopic("fincore.transaction") shouldBe "fincore.transaction-dlt"
    }

    @Test
    fun `should build a tiered retry topic name from the base topic and tier`() {
        naming.retryTopic("fincore.transaction", 1) shouldBe "fincore.transaction-retry-1"
    }

    @Test
    fun `should reject a blank base topic`() {
        shouldThrow<IllegalArgumentException> { naming.deadLetterTopic(" ") }
        shouldThrow<IllegalArgumentException> { naming.retryTopic(" ", 0) }
    }
}
