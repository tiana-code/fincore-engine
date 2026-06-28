// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.store.application

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.transaction.annotation.Transactional
import kotlin.reflect.jvm.javaMethod

class EvaluationTransactionBoundaryTest {
    @Test
    fun `should not wrap evaluate in a transaction so the bounded eval holds no connection`() {
        val method = requireNotNull(EvaluationServiceImpl::evaluate.javaMethod)

        AnnotatedElementUtils.findMergedAnnotation(method, Transactional::class.java).shouldBeNull()
        AnnotatedElementUtils
            .findMergedAnnotation(EvaluationServiceImpl::class.java, Transactional::class.java)
            .shouldBeNull()
    }

    @Test
    fun `should write the audit log in its own transaction`() {
        val method = requireNotNull(DecisionLogWriter::write.javaMethod)

        AnnotatedElementUtils.findMergedAnnotation(method, Transactional::class.java).shouldNotBeNull()
    }

    @Test
    fun `should not wrap replay in a transaction so it holds no connection across the bounded evals`() {
        val method = requireNotNull(ReplayServiceImpl::replay.javaMethod)

        AnnotatedElementUtils.findMergedAnnotation(method, Transactional::class.java).shouldBeNull()
        AnnotatedElementUtils
            .findMergedAnnotation(ReplayServiceImpl::class.java, Transactional::class.java)
            .shouldBeNull()
    }
}
