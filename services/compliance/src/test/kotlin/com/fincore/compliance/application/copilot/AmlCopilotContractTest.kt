// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.copilot

import com.fincore.compliance.domain.KycSession
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

private class FixedAmlCopilot(
    private val response: CopilotResponse,
) : AmlCopilot {
    override fun assist(request: CopilotRequest): CopilotResponse = response
}

class AmlCopilotContractTest {
    @Test
    fun `should expose a single assist method when inspected`() {
        AmlCopilot::class.java.isInterface shouldBe true

        val method =
            AmlCopilot::class.java.declaredMethods
                .filter { !it.isBridge && !it.isSynthetic }
                .single { it.name == "assist" }

        method.returnType shouldBe CopilotResponse::class.java
        method.parameterTypes.toList() shouldBe listOf(CopilotRequest::class.java)
    }

    @Test
    fun `should return the advisory response from the implementation`() {
        val provider = FixedAmlCopilot(CopilotResponse("summary", listOf("review-id-docs")))

        val result = provider.assist(CopilotRequest("case-1", listOf("note-a")))

        result.shouldBeInstanceOf<CopilotResponse>().recommendations shouldBe listOf("review-id-docs")
    }

    @Test
    fun `should reject a blank case reference`() {
        shouldThrow<IllegalArgumentException> { CopilotRequest(" ") }
    }

    @Test
    fun `should reject a case reference over the length limit`() {
        shouldThrow<IllegalArgumentException> { CopilotRequest("x".repeat(KycSession.MAX_SUBJECT_REFERENCE_LENGTH + 1)) }
    }
}
