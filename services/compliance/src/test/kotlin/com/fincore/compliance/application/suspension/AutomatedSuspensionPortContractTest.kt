// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.suspension

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

private class FixedAutomatedSuspensionPort(
    private val result: SuspensionResult,
) : AutomatedSuspensionPort {
    override fun requestSuspension(request: SuspensionRequest): SuspensionResult = result
}

class AutomatedSuspensionPortContractTest {
    private val request = SuspensionRequest("subject-1", "signal-a")

    @Test
    fun `should expose a single request suspension method when inspected`() {
        AutomatedSuspensionPort::class.java.isInterface shouldBe true

        val method =
            AutomatedSuspensionPort::class.java.declaredMethods
                .filter { !it.isBridge && !it.isSynthetic }
                .single { it.name == "requestSuspension" }

        method.returnType shouldBe SuspensionResult::class.java
        method.parameterTypes.toList() shouldBe listOf(SuspensionRequest::class.java)
    }

    @Test
    fun `should return suspended when the implementation suspends`() {
        FixedAutomatedSuspensionPort(SuspensionResult.Suspended("ref-1"))
            .requestSuspension(request)
            .shouldBeInstanceOf<SuspensionResult.Suspended>()
    }

    @Test
    fun `should return already suspended for a replayed signal`() {
        FixedAutomatedSuspensionPort(SuspensionResult.AlreadySuspended)
            .requestSuspension(request)
            .shouldBeInstanceOf<SuspensionResult.AlreadySuspended>()
    }

    @Test
    fun `should return rejected when the implementation refuses`() {
        FixedAutomatedSuspensionPort(SuspensionResult.Rejected("not eligible"))
            .requestSuspension(request)
            .shouldBeInstanceOf<SuspensionResult.Rejected>()
    }

    @Test
    fun `should reject a blank subject reference`() {
        shouldThrow<IllegalArgumentException> { SuspensionRequest(" ", "signal-a") }
    }

    @Test
    fun `should reject a blank reason`() {
        shouldThrow<IllegalArgumentException> { SuspensionRequest("subject-1", " ") }
    }

    @Test
    fun `should reject a reason over the length limit`() {
        shouldThrow<IllegalArgumentException> {
            SuspensionRequest("subject-1", "a".repeat(SuspensionRequest.MAX_REASON_LENGTH + 1))
        }
    }
}
