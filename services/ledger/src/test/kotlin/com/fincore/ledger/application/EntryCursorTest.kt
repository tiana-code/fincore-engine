// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class EntryCursorTest {
    @Test
    fun `should round-trip a cursor through encode and decode`() {
        val cursor = EntryCursor(Instant.parse("2026-06-13T10:00:00.123456Z"), UUID.randomUUID())

        val decoded = EntryCursor.decode(cursor.encode())

        decoded.postedAt shouldBe cursor.postedAt
        decoded.id shouldBe cursor.id
    }

    @Test
    fun `should reject a non-base64 cursor`() {
        shouldThrow<IllegalArgumentException> { EntryCursor.decode("!!! not base64 !!!") }
    }

    @Test
    fun `should reject a base64 cursor without the delimiter`() {
        val noDelimiter =
            java.util.Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString("garbage".toByteArray())

        shouldThrow<IllegalArgumentException> { EntryCursor.decode(noDelimiter) }
    }

    @Test
    fun `should reject a cursor with a bad timestamp`() {
        val badTimestamp =
            java.util.Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString("nope|${UUID.randomUUID()}".toByteArray())

        shouldThrow<IllegalArgumentException> { EntryCursor.decode(badTimestamp) }
    }
}
