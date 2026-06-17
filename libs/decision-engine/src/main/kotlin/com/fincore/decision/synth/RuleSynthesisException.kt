// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.decision.synth

/** Signals that a [RuleSynthesizer] could not produce a candidate rule. */
class RuleSynthesisException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
