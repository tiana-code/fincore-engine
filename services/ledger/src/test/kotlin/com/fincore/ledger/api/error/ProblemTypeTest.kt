// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.api.error

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class ProblemTypeTest {
    @Test
    fun `every type uri derives from the base uri and slug`() {
        ProblemType.entries.forEach { it.type.toString() shouldBe ProblemType.BASE_URI + it.slug }
    }

    @Test
    fun `every code is non-blank screaming snake`() {
        ProblemType.entries.forEach { it.code shouldMatch SCREAMING_SNAKE }
    }

    @Test
    fun `slugs and codes are unique across the catalog`() {
        ProblemType.entries.map { it.slug }.let { it.size shouldBe it.toSet().size }
        ProblemType.entries.map { it.code }.let { it.size shouldBe it.toSet().size }
    }

    @Test
    fun `no urn scheme remains in the main source set`() {
        val offenders =
            Files.walk(Path.of("src/main/kotlin")).use { stream ->
                stream
                    .filter { it.toString().endsWith(".kt") }
                    .filter { Files.readString(it).contains("urn:fincore") }
                    .map { it.toString() }
                    .toList()
            }
        offenders shouldBe emptyList()
    }

    private companion object {
        val SCREAMING_SNAKE = Regex("^[A-Z][A-Z0-9_]*$")
    }
}
