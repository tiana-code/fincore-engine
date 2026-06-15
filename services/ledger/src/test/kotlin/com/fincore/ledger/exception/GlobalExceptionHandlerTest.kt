// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.ledger.exception

import com.fincore.core.AccountId
import com.fincore.ledger.domain.exception.AccountNotFoundException
import com.fincore.ledger.domain.exception.ConcurrencyConflictException
import com.fincore.ledger.domain.exception.DoubleEntryViolationException
import com.fincore.ledger.domain.exception.IdempotencyConflictException
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.method.HandlerMethod

class GlobalExceptionHandlerTest {
    private val handler = GlobalExceptionHandler()
    private val request = MockHttpServletRequest("GET", "/v1/test")

    @Test
    fun `should map account not found to the catalog type and code`() {
        val problem = handler.handleAccountNotFound(AccountNotFoundException(AccountId.generate()), request)
        problem.status shouldBe 404
        problem.type.toString() shouldBe "https://fincore.dev/errors/account-not-found"
        problem.properties?.get("code") shouldBe "ACCOUNT_NOT_FOUND"
    }

    @Test
    fun `should map double-entry violation to the entries-sum-not-zero code`() {
        val problem = handler.handleDoubleEntry(DoubleEntryViolationException("net=5"), request)
        problem.status shouldBe 422
        problem.type.toString() shouldBe "https://fincore.dev/errors/double-entry-violation"
        problem.properties?.get("code") shouldBe "ENTRIES_SUM_NOT_ZERO"
    }

    @Test
    fun `should map idempotency conflict to a 409 catalog code`() {
        val problem = handler.handleIdempotencyConflict(IdempotencyConflictException(), request)
        problem.status shouldBe 409
        problem.properties?.get("code") shouldBe "IDEMPOTENCY_KEY_CONFLICT"
    }

    @Test
    fun `should set retry-after and the catalog code on a concurrency conflict`() {
        val response = handler.handleConcurrencyConflict(ConcurrencyConflictException(RuntimeException("retry")), request)
        response.statusCode.value() shouldBe 503
        response.headers.getFirst("Retry-After") shouldBe "1"
        response.body?.properties?.get("code") shouldBe "CONCURRENCY_CONFLICT"
    }

    @Test
    fun `should derive per-field codes from the failing constraint`() {
        val problem = handler.handleValidation(validationException(fieldError("currency", "NotBlank")), request)
        problem.status shouldBe 400
        problem.properties?.get("code") shouldBe "VALIDATION_FAILED"
        val errors = errorsOf(problem)
        errors.first()["field"] shouldBe "currency"
        errors.first()["code"] shouldBe "NOT_BLANK"
    }

    @Test
    fun `should derive a compound constraint code with underscores`() {
        val problem = handler.handleValidation(validationException(fieldError("type", "NotNull")), request)
        errorsOf(problem).first()["code"] shouldBe "NOT_NULL"
    }

    @Suppress("UNCHECKED_CAST")
    private fun errorsOf(problem: org.springframework.http.ProblemDetail): List<Map<String, String>> =
        problem.properties?.get("errors") as List<Map<String, String>>

    private fun fieldError(
        field: String,
        constraint: String,
    ): FieldError =
        FieldError(
            "request",
            field,
            null,
            false,
            arrayOf("$constraint.request.$field", constraint),
            null,
            "must be valid",
        )

    private fun validationException(vararg errors: FieldError): MethodArgumentNotValidException {
        val binding = BeanPropertyBindingResult(Any(), "request")
        errors.forEach(binding::addError)
        val method = HandlerMethod(this, this::class.java.getDeclaredMethod("dummy", String::class.java))
        return MethodArgumentNotValidException(method.methodParameters[0], binding)
    }

    @Suppress("unused", "UnusedPrivateMember")
    private fun dummy(value: String): String = value
}
