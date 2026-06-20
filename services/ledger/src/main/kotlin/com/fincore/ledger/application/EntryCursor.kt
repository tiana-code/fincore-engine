// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.application

import java.time.Instant
import java.util.Base64
import java.util.UUID

// Wire form is base64url; clients must treat it as opaque and never construct it manually.
data class EntryCursor(
    val postedAt: Instant,
    val id: UUID,
) {
    fun encode(): String = ENCODER.encodeToString("$postedAt$DELIMITER$id".toByteArray(Charsets.UTF_8))

    companion object {
        private const val DELIMITER = '|'
        private val ENCODER = Base64.getUrlEncoder().withoutPadding()
        private val DECODER = Base64.getUrlDecoder()

        fun decode(raw: String): EntryCursor {
            val decoded = orInvalid { String(DECODER.decode(raw), Charsets.UTF_8) }
            val separator = decoded.indexOf(DELIMITER)
            require(separator > 0) { "invalid cursor" }
            val postedAt = orInvalid { Instant.parse(decoded.substring(0, separator)) }
            val id = orInvalid { UUID.fromString(decoded.substring(separator + 1)) }
            return EntryCursor(postedAt, id)
        }

        private fun <T> orInvalid(block: () -> T): T = runCatching(block).getOrElse { throw IllegalArgumentException("invalid cursor", it) }
    }
}
