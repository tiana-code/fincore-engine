# Changelog

All notable changes to FinCore Engine are documented in this file.

Format: [Keep a Changelog 1.1.0](https://keepachangelog.com/en/1.1.0/)
Versioning: [Semantic Versioning 2.0.0](https://semver.org/spec/v2.0.0.html)
Release tooling: [release-please](https://github.com/googleapis/release-please) (Conventional Commits → automated release PRs)

> **Pre-1.0 notice.** All `0.x` releases are pre-stable. Breaking changes can land in any minor bump. SemVer guarantees apply from `1.0.0` onward.

---

## [Unreleased]

### Added

- Initial repository scaffold: `LICENSE` (BSL 1.1), `README.md`, `CONTRIBUTING.md`, `SECURITY.md`, `CODE_OF_CONDUCT.md`.
- Editor configuration: `.editorconfig`, `.gitattributes`, `.dockerignore`.
- Gradle 8.14 wrapper, Kotlin 2.0.21, Spring Boot 3.5 LTS stack pinned in `gradle/libs.versions.toml`.
- Detekt 1.x configuration in `detekt.yml`.
- OpenAPI 3.1 specification scaffold in `api/openapi.yaml`.
- Wiki content under `wiki/` (architecture, ADRs 0001-0009, code rules, epics, roadmap).
- AI agent configuration: `CLAUDE.md`, `.claude/` settings.
- GitHub configuration: CODEOWNERS, FUNDING.yml, issue and PR templates, labels, Dependabot, release-please config.

### Documentation

- ADR-0001 - Modular monolith over microservices for OSS core.
- ADR-0002 - BSL 1.1 license selection, change date 2030-04-25, change license Apache 2.0.
- ADR-0003 - Transactional outbox pattern over direct Kafka publishing.
- ADR-0004 - Hibernate ORM 6.6 over jOOQ / Exposed.
- ADR-0005 - Keycloak 26.6 as IdP and authorization server.
- ADR-0006 - Redpanda as default development broker, Apache Kafka as production option.
- ADR-0007 - Double-entry invariant enforced at the application layer (sum-of-postings = 0).
- ADR-0008 - Decision Engine uses deterministic JSON DSL, no embedded scripting languages.
- ADR-0009 - BSL 1.1 chosen over AGPL 3.0 for commercial-usage clarity.

---

## [0.0.0] - TBD

First tagged scaffold release.

[Unreleased]: https://github.com/tiana-code/fincore-engine/compare/main...HEAD
