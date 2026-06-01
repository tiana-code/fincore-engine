<!-- SPDX-License-Identifier: BUSL-1.1 -->

# Contributing to FinCore Engine

Thank you for your interest in contributing. FinCore Engine is a financial infrastructure project with correctness as its non-negotiable constraint. This guide explains how to contribute effectively.

---

## Code of Conduct

All contributors are expected to follow the [Contributor Covenant 2.1](CODE_OF_CONDUCT.md). By participating you agree to its terms. Violations can be reported to `hello@itiana.dev`.

---

## Before you start

1. **Read the code rules.** The project follows strict coding standards documented in the [Code Rules wiki page](https://github.com/tiana-code/fincore-engine/wiki/Code-Rules). Read it before writing any code.
2. **Read the dev guide.** Git flow, branching conventions, release flow, and ADR process are in [Dev Guides](https://github.com/tiana-code/fincore-engine/wiki/Dev-Guides).
3. **Check existing issues.** Your bug or feature request may already be tracked. Search [issues](https://github.com/tiana-code/fincore-engine/issues) and [discussions](https://github.com/tiana-code/fincore-engine/discussions) first.

---

## Reporting bugs

Open a [bug report issue](https://github.com/tiana-code/fincore-engine/issues/new?template=bug_report.yml). Include:

- FinCore Engine version (or commit SHA).
- Steps to reproduce - minimal, deterministic, ideally a failing test.
- Observed behaviour vs. expected behaviour.
- Relevant logs, stack traces, or database state (redact any sensitive data).

For security vulnerabilities, **do not open a public issue** - follow [SECURITY.md](SECURITY.md) instead.

---

## Suggesting features

Open a [feature request issue](https://github.com/tiana-code/fincore-engine/issues/new?template=feature_request.yml) or start a [Discussion](https://github.com/tiana-code/fincore-engine/discussions) in the **Ideas** category.

When proposing a feature, describe:

- The problem it solves (not just the solution).
- Which use cases it serves (reference [Use Cases](https://github.com/tiana-code/fincore-engine/wiki/Use-Cases) if relevant).
- Whether it belongs in OSS core or in a plug-in interface.

Features that require embedding provider-specific logic, proprietary data, or domain-specific business rules are out of scope for the OSS core. The project uses plug-in interfaces (`RiskScorer`, `KycProvider`, `BankProvider`, `SanctionsProvider`) for such extensions.

---

## Pull request process

### 1. Fork and branch

```bash
# Fork on GitHub, then:
git clone https://github.com/<your-handle>/fincore-engine.git
cd fincore-engine
git checkout -b feat/short-description   # or fix/, docs/, chore/
```

Branch naming follows Conventional Commits prefixes: `feat/`, `fix/`, `docs/`, `chore/`, `refactor/`, `test/`.

### 2. Write a failing test first

FinCore Engine uses Test-Driven Development (TDD). For every bug fix or feature, write a failing test that captures the requirement before writing production code. See [Testing Strategy](https://github.com/tiana-code/fincore-engine/wiki/Testing-Strategy).

### 3. Implement

- Follow [Code Rules](https://github.com/tiana-code/fincore-engine/wiki/Code-Rules) exactly. Detekt and ktlint are enforced in CI - fix all violations locally before pushing.
- Match the existing code style. Do not reformat unrelated code.
- Add SPDX headers to new files: `// SPDX-License-Identifier: BUSL-1.1` (Kotlin), `<!-- SPDX-License-Identifier: BUSL-1.1 -->` (Markdown/HTML).
- No magic numbers, no `println`, no `!!` without explicit justification in a comment.

Run checks locally:

```bash
./gradlew detekt ktlintCheck test
```

### 4. Commit with Conventional Commits

```
feat(ledger): add time-travel balance query via asOf parameter
fix(payments): prevent duplicate outbox publish under REPEATABLE_READ
docs(adr): add ADR-0010 for TigerBeetle storage adapter
```

Commit messages are used by `release-please` to generate changelogs. The format is: `<type>(<scope>): <description>`.

Signed commits are encouraged. Set up GPG or SSH signing: [Dev Guides §3](https://github.com/tiana-code/fincore-engine/wiki/Dev-Guides).

### 5. Open a pull request

Use the pull request template. Fill in all sections:

- **What** - what changed and why.
- **How tested** - which tests cover the change.
- **Checklist** - code rules, tests pass, SPDX headers, no private data leaked.

A maintainer will review within five business days. Expect at least one round of feedback. Address all review comments before re-requesting review.

---

## Coding standards summary

| Area | Rule |
|------|------|
| Language | Kotlin 2.x only in production source sets. No `.java` in `src/main/`. |
| Formatting | ktlint (default ruleset), 4-space indent, max line 140 chars. |
| Complexity | detekt: cyclomatic complexity ≤ 15, method length ≤ 30 lines. |
| Money | `BigDecimal` with `DECIMAL128`, `HALF_EVEN` rounding. Never `Float`/`Double`. |
| Enums in DB | `@Enumerated(EnumType.STRING)`. Never `ORDINAL`. |
| Transactions | `@Transactional` only in `*ServiceImpl`. No external calls inside a transaction. |
| Tests | JUnit 5 + Kotest assertions + MockK for unit; Testcontainers for integration. Coverage > 70%; critical paths > 90%. |

Full rules: [Code Rules](https://github.com/tiana-code/fincore-engine/wiki/Code-Rules)

---

## Contributor License Agreement

For substantial pull requests (new features, significant refactors), you certify that:

- Your contribution is your original work, or you have the rights to submit it.
- You grant the project the right to use and distribute your contribution under the terms of the [Business Source License 1.1](LICENSE).
- Once the Change Date passes, your contribution will be redistributable under Apache License 2.0.

This is a lightweight DCO-style certification, not a formal CLA. For questions, open a Discussion.

---

## First-time contributors

Look for issues tagged [`good first issue`](https://github.com/tiana-code/fincore-engine/issues?q=is%3Aissue+label%3A%22good+first+issue%22). These are scoped, well-documented, and do not require deep domain knowledge to start.

For questions, ask in [Discussions](https://github.com/tiana-code/fincore-engine/discussions) under the **Q&A** category. The maintainer team responds within one week.

Welcome aboard.
