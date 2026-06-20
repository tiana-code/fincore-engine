<!-- SPDX-License-Identifier: BUSL-1.1 -->

# Plugging in your own providers

FinCore Engine ships a KYC/AML **orchestration framework**, not turnkey integrations. The compliance service owns the
workflow (sessions, cases, screening, audit) and delegates every call to an external system through a small set of
plug-in ports. The only in-tree implementations are deterministic **sandbox** providers, off by default, so the
service boots and demos end to end with no third-party account. Real adapters live out of tree: you implement a port,
register it as a Spring bean, and the orchestration layer calls it.

This is deliberate. The open-source repository contains no real provider protocol, no sanctions list, and no business
threshold (see the OSS boundary in `CLAUDE.md`). The ports are generic by construction, so your adapter is the only
place provider-specific detail lives.

## The ports

| Port | Service | Package | Method |
|------|---------|---------|--------|
| `KycProvider` | compliance | `com.fincore.compliance.application.kyc` | `check(KycCheckRequest): KycCheckResult` |
| `SanctionsProvider` | compliance | `com.fincore.compliance.application.sanctions` | `screen(SanctionsScreeningRequest): SanctionsScreeningResult` |
| `AmlCopilot` | compliance | `com.fincore.compliance.application.copilot` | `assist(CopilotRequest): CopilotResponse` |
| `BankProvider` | payments | `com.fincore.payments.application.bank` | `submit(BankPaymentRequest): BankSubmissionResult` |

Every port follows the same three rules:

1. **No persistence.** A provider implementation reads its request and returns a result. It never writes to the
   database. The caller decides what to do with the outcome.
2. **Called outside any transaction.** Network calls to a third party must not hold a database transaction open, so the
   orchestration layer invokes ports outside transactional boundaries.
3. **Business outcome versus technical failure.** A decided outcome is returned as a result type. A technical or
   transient failure (timeout, 5xx, malformed response) is thrown as the port's exception, which the orchestration
   layer treats as retryable rather than as a verdict.

### `KycProvider`

```kotlin
interface KycProvider {
    fun check(request: KycCheckRequest): KycCheckResult
}
```

`KycCheckRequest` carries a single `subjectReference`: an opaque token that identifies the subject under check, never
raw PII. Your adapter resolves the real subject data out of tree from that reference. `KycCheckResult` is a sealed type:

- `Approved(providerReference)`
- `Rejected(reason)` where `reason` is a generic code, not PII
- `Pending(providerReference)` when a final state arrives asynchronously
- `InsufficientData(missing)` as a first-class outcome listing the attribute keys that were missing

A technical failure is thrown as `KycProviderException`.

### `SanctionsProvider`

```kotlin
interface SanctionsProvider {
    fun screen(request: SanctionsScreeningRequest): SanctionsScreeningResult
}
```

`SanctionsScreeningRequest` supports a configurable m-of-n partial match: `attributes` is the set of dimension keys to
screen ("n"), and `requiredMatches` is the minimum that must match for a potential hit ("m"), with `1 <= m <= n`.
`SanctionsScreeningResult` is `Clear`, `PotentialMatch(matchedAttributes, score)` where `score` is a provider-reported
confidence in `[0, 1]`, or `InsufficientData(missing)`. A technical failure is thrown as `SanctionsProviderException`.

### `AmlCopilot`

```kotlin
interface AmlCopilot {
    fun assist(request: CopilotRequest): CopilotResponse
}
```

This port is **advisory**. `CopilotResponse(summary, recommendations)` is guidance a human reviewer weighs; it is never
an authoritative decision, and the contract makes no determinism or correctness promise. An LLM-backed adapter is a
natural fit. A technical failure is thrown as `AmlCopilotException`.

## Writing an adapter

Implement the port and register it as a Spring bean. The compliance service injects whichever `KycProvider` bean is on
the context, so your adapter replaces the sandbox simply by being present (and the sandbox staying off).

```kotlin
package com.example.fincore.kyc

import com.fincore.compliance.application.kyc.KycCheckRequest
import com.fincore.compliance.application.kyc.KycCheckResult
import com.fincore.compliance.application.kyc.KycProvider
import com.fincore.compliance.application.kyc.KycProviderException
import org.springframework.stereotype.Component

@Component
class AcmeKycProvider(
    private val client: AcmeKycClient,
) : KycProvider {
    override fun check(request: KycCheckRequest): KycCheckResult =
        try {
            when (val verdict = client.verify(request.subjectReference)) {
                AcmeVerdict.PASS -> KycCheckResult.Approved(verdict.reference)
                AcmeVerdict.FAIL -> KycCheckResult.Rejected("acme.fail")
                AcmeVerdict.REVIEW -> KycCheckResult.Pending(verdict.reference)
            }
        } catch (e: AcmeTransportException) {
            throw KycProviderException("acme KYC call failed", e)
        }
}
```

Two things to keep in mind:

- Return business outcomes, throw on technical failures. Mapping a timeout to `Rejected` would turn an outage into a
  wrongful decline.
- Resolve PII inside the adapter only. The port request carries an opaque reference, so subject data never crosses the
  orchestration layer.

### The sandbox pattern

The in-tree sandbox providers are gated by `@ConditionalOnProperty`, off unless explicitly enabled:

```kotlin
@Component
@ConditionalOnProperty(
    prefix = "fincore.compliance.kyc.sandbox",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = false,
)
class SandboxKycProvider : KycProvider { /* ... */ }
```

For local exploration you can run the sandbox by setting `fincore.compliance.kyc.sandbox.enabled=true` (and the
matching `fincore.compliance.sanctions.sandbox.enabled=true`). In a deployment that wires real adapters, leave those
off so your beans are the only providers on the context. If you want a fallback toggle on your own adapter, the same
`@ConditionalOnProperty` pattern applies.

## Payments: `BankProvider`

The payments service uses the identical model for bank submission. `BankProvider.submit` returns a `BankSubmissionResult`
or throws `BankProviderException`, and the in-tree `SandboxBankProvider` is gated by
`fincore.payments.bank.sandbox.enabled`. Like the compliance sandbox, the binary default is off (`matchIfMissing = false`);
the sandbox Helm chart sets the property to `true` because the sandbox is the only open-source bank provider, so the
service has a bean to boot against. Outside that chart, enable it explicitly or supply your own adapter.

## Follow-up

A standalone, buildable example adapter module is a planned addition to `examples/`. Until then, the snippet above plus
the in-tree sandbox providers under `services/compliance/.../infrastructure/` are the reference implementations.
