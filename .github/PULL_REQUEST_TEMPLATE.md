## Summary

<!-- One or two sentences describing what this PR does and why. -->

## Related Issues

<!-- Link every related issue. Use "Closes #N" to auto-close on merge. -->

Closes #

## Type of Change

<!-- Mark all that apply with an [x]. -->

- [ ] `fix` - bug fix (non-breaking)
- [ ] `feat` - new feature (non-breaking)
- [ ] `feat!` / `fix!` - breaking change
- [ ] `refactor` - code restructuring without behavior change
- [ ] `perf` - performance improvement
- [ ] `test` - tests only
- [ ] `docs` - documentation only
- [ ] `chore` - build, CI, config, deps

## Testing Done

<!-- Describe how you tested this change. -->

- [ ] Unit tests added / updated (`./gradlew test`)
- [ ] Integration tests added / updated (`./gradlew integrationTest`)
- [ ] Tested manually - describe steps:

## Screenshots / Logs

<!-- If this change affects API responses, UI, or observable behavior, paste relevant output. -->

## Checklist

- [ ] All new and existing tests pass (`./gradlew build`)
- [ ] ktlint and detekt pass (`./gradlew ktlintCheck detekt`)
- [ ] New public APIs are documented (Javadoc / KDoc + OpenAPI spec updated if applicable)
- [ ] Wiki updated if architecture, ADRs, or developer guides are affected
- [ ] CHANGELOG entry added (or release-please will handle via Conventional Commits)
- [ ] No PII, credentials, or secrets included
- [ ] SPDX license header present on all new Kotlin/Java source files
- [ ] No prohibited terms in code or comments (internal or private-domain terms, specific partner or third-party company names)
- [ ] Idempotency-Key handling included for new POST/PUT/DELETE endpoints
- [ ] `@Transactional` not placed on controllers; no external HTTP calls inside `@Transactional`
- [ ] Money values use `BigDecimal` - no `Float` or `Double`
