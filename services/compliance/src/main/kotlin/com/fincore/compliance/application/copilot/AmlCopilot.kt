// SPDX-License-Identifier: BUSL-1.1
// SPDX-FileCopyrightText: 2026 FinCore Engine Authors

package com.fincore.compliance.application.copilot

/**
 * Plug-in port for an LLM-backed assistant that helps a human reviewer work an AML case.
 *
 * Implementations are provided out of tree (for example an LLM-backed adapter) and are NOT part of this open-source
 * service. The contract is deliberately generative and makes no determinism or correctness promise: the returned
 * [CopilotResponse] is ADVISORY guidance the reviewer weighs, never an authoritative decision. This port performs no
 * persistence and encodes no business prompt. A technical or transient failure is thrown as an [AmlCopilotException].
 */
interface AmlCopilot {
    fun assist(request: CopilotRequest): CopilotResponse
}
