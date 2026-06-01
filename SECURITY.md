<!-- SPDX-License-Identifier: BUSL-1.1 -->

# Security Policy

FinCore Engine handles financial transactions and compliance data. Security is treated as a correctness requirement, not an afterthought. This document describes how to report vulnerabilities and what to expect in response.

---

## Supported versions

| Version | Supported |
|---------|-----------|
| `main` branch (pre-release) | Yes - active development |
| `0.x` releases | Yes - patch releases for HIGH and CRITICAL findings |
| `< 0.1.0` | No - pre-release snapshots, no backport commitment |

Once `1.0.0` is released, this table will be updated to reflect the N and N-1 minor version support window.

---

## Reporting a vulnerability

**Do not open a public GitHub issue for security vulnerabilities.** Public disclosure before a fix is available puts all users at risk.

### Preferred channel: GitHub Security Advisories

Use [GitHub Private Security Advisories](https://github.com/tiana-code/fincore-engine/security/advisories/new) to report confidentially. This is the fastest path to triage.

### Alternative: email

Send a detailed report to `hello@itiana.dev` with the subject line `[SECURITY] FinCore Engine - <brief description>`.

> **Note:** This email address is active. Set up a PGP-encrypted channel if you need to share sensitive payloads - request the maintainer's public key via email first.

### What to include

- **Description** - what the vulnerability is and which component is affected.
- **Impact** - what an attacker can do by exploiting it (confidentiality, integrity, availability).
- **Steps to reproduce** - minimal, deterministic reproduction steps.
- **Environment** - version/commit SHA, OS, Java version, relevant configuration.
- **Suggested fix** - if you have one (optional but appreciated).

---

## Response SLA

| Stage | Target |
|-------|--------|
| Acknowledgement | Within 48 hours of receipt |
| Initial triage (severity classification) | Within 5 business days |
| Fix or mitigation plan for CRITICAL/HIGH | Within 30 days |
| Fix or mitigation plan for MEDIUM/LOW | Within 90 days |
| Public disclosure | After fix is released, coordinated with reporter |

These are targets, not guarantees. Complex vulnerabilities may take longer. The maintainer will communicate status if a deadline cannot be met.

---

## Coordinated disclosure

FinCore Engine follows a **90-day coordinated disclosure** policy:

1. Reporter submits via the private channel above.
2. Maintainer acknowledges, triages, and begins working on a fix.
3. A CVE is requested if the finding meets the threshold.
4. A fix is released in a patch version.
5. A public security advisory is published on GitHub.
6. If 90 days pass without a fix, the reporter may disclose at their discretion after notifying the maintainer.

Credit is given in the advisory unless the reporter prefers to remain anonymous.

---

## Out of scope

The following are **not** considered in-scope vulnerabilities for this project:

- Denial-of-service attacks requiring physical access or insider credentials.
- Social engineering of maintainers or users.
- Vulnerabilities in dependencies - report those upstream to the respective project; we will update the dependency promptly when a fix is available.
- Findings in `docs/private/` (gitignored, not part of the public repository).
- Issues in forks or unofficial distributions.

---

## Bounty policy

FinCore Engine is a pre-revenue OSS project. There is no formal bug bounty program. Researchers who make a significant contribution to improving security may be recognized via [GitHub Sponsors](https://github.com/sponsors/tiana-code) at the maintainer's discretion. Ask if you want to discuss this before reporting.

---

## Security contact

| Purpose | Contact |
|---------|---------|
| Vulnerability reports | `hello@itiana.dev` or GitHub Security Advisories |
| Security policy questions | [GitHub Discussions - Security category](https://github.com/tiana-code/fincore-engine/discussions) |
| Maintainer | [@tiana-code](https://github.com/tiana-code) |
