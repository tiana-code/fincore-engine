# AGENTS — FinCore Engine

> Ростер агентов, конвейер и правила хэндоффа. Поведенческие правила → `CLAUDE.md`. Структурная память → `.claude/memory/`.
> Агенты общаются ТОЛЬКО через файлы в `docs/plans/<feature-slug>/` (шаблон `docs/plans/_TEMPLATE_feature/`). Прогресс - в `docs/plans/progress/`.

## Ростер (16 агентов)

| Агент | Модель | Роль | Читает → пишет |
|-------|--------|------|----------------|
| **scout** | haiku | discovery «где X», read-only | запрос → ответ (без записи) |
| **analyst** | opus | ТЗ → spec.md с тестируемыми acceptance criteria | ТЗ → `spec.md` |
| **architect** | opus | дизайн + декомпозиция + ADR + trade-offs | `spec.md` → `plan.md`, ADR |
| **critic** | opus | адверсариальный gate: pre-mortem плана + финальный вердикт GO/NO-GO (read-only) | `plan.md` / деливерабл → вердикт |
| **tester** | sonnet | TDD: падающие тесты RED first, evidence | `spec.md`/`plan.md` → тесты |
| **coder** | sonnet | минимальная реализация Kotlin/Spring под план | `plan.md`/`tasks.md` → код |
| **db-expert** | sonnet | Liquibase идемпотентные миграции, индексы, N+1 | `plan.md` → миграции/схема |
| **frontend-react** | sonnet | React 19+Vite+TS+shadcn/ui (Phase E sandbox UI) | `plan.md`/дизайн → UI-код |
| **designer** | sonnet | domain-aware UI-дизайн fintech-дашборда, a11y, 3 viewport | требования UI → дизайн → frontend-react |
| **verifier** | sonnet | evidence-гейт: «готово» = свежий `./gradlew test/build` (read-only) | код + клеймы → VERIFIED/NOT-VERIFIED |
| **code-reviewer** | sonnet | per-slice ревью: соответствие плану ДО качества (read-only) | per-slice diff → вердикт + severity |
| **code-simplifier** | sonnet | анти-слоп: упрощение Kotlin без смены поведения | код → упрощённый код |
| **evaluator** | sonnet | ECC/EDD: рубрика+threshold 0.80, eval-loop, regression-evals | деливерабл → Eval Report + `.claude/evals/` |
| **security-auditor** | opus | merge-gate ОДИН раз: security + покрытие требований + OSS-boundary (read-only) | финал → `audit.md` PASS/BLOCK |
| **writer** | opus | personal-brand контент: ADR-as-blog, deep-dive, talks, Wiki | код/ADR/epic → `content/`, `wiki/` |
| **tracer** | sonnet | отладка через конкурирующие гипотезы (3 лейна) | баг → root cause + фикс → coder |

## Конвейер (Dev) с eval-loop
```
analyst(spec.md)
  → architect(plan.md + ADR)
  → critic(pre-mortem gate плана: GO/GO-WITH-CHANGES/NO-GO)   ← STOP-гейт до кода
  → tester(падающие тесты RED)
  → coder / db-expert / frontend-react(GREEN)
  → verifier(свежий ./gradlew test/build = доказательство)
  → code-reviewer(per-slice: соответствие плану ДО качества)
  → code-simplifier(анти-слоп проход)
  → evaluator(рубрика ≥0.80; FAIL → возврат coder раунд N+1 с gap-листом; PASS → дальше)
  → security-auditor(merge-gate: security + требования + OSS-boundary → PASS/BLOCK)
```
Боковые: **designer** → frontend-react для UI-срезов. **writer** - контент/Wiki/ADR-as-blog (отдельный трек, цель #1). **tracer** - отладка по требованию. **scout** - discovery в любой момент.

**Двухуровневый ревью:** дешёвый per-slice (`code-reviewer`, sonnet, часто) + глубокий gate в конце (`security-auditor`, opus, один раз). Плюс автоматический Stop-хук Kotlin/Spring+security ревью при завершении сессии.

**Eval-loop (ECC/EDD):** автор = generator, `evaluator` = ruthless judge. Рубрика FinCore dev: correctness 0.30 / OSS-boundary+clean-room 0.15 / test-coverage+TDD 0.20 / idempotency+concurrency 0.15 / code-rules conformance 0.20. Threshold 0.80. Стоп: PASS либо 3 раунда без прогресса → эскалация. Каждый дефект → regression-eval в `.claude/evals/`.

## Правила хэндоффа
- Вход-файл не готов → стадия не стартует.
- Выход самодостаточен (следующий агент не лезет в чат за контекстом).
- Read-only агенты (scout, critic, verifier, code-reviewer, security-auditor, evaluator) имеют `disallowedTools: Write, Edit` - не правят артефакт, выносят вердикт.
- Автор не аппрувит свой же проход (authoring ≠ review).

## Память (защита от краша)
- Структурная: `.claude/memory/` (MEMORY.md индекс + architecture/decisions/patterns). Читается на SessionStart.
- Сессионная: `docs/plans/progress/<agent>-<date>.md` - после каждого Write/Edit строка в `## Done`; при блокировке `## Blocked`.
- PreCompact/SessionStart хуки напоминают критические правила и сохраняют прогресс.

## Заимствованные скиллы (`.claude/skills/`, префиксы без коллизий)
- **mbx-skill-review** - ревью качества скиллов/обвязки.
- **omc-deep-interview** - извлечение требований (analyst/architect при неясном ТЗ).
- **omc-skillify** - оформление повторяемого процесса в скилл.
- **omc-ai-slop-cleaner** - чистка AI-слопа (coder/code-simplifier).
- **ecc-eval-harness** - EDD: capability/regression evals, pass@k (опора evaluator/tester).
- **ecc-verification-loop** - 6-фазная верификация build/type/lint/test/security/diff (verifier).
- **omc-ultraqa** - QA-цикл до зелёного. **omc-verify** - быстрая верификация. **omc-visual-verdict** - визуальная проверка UI (designer). **omc-self-improve** - саморефлексия обвязки.

## MCP (наследуется глобально, ставить отдельно не надо)
- **context7** - проверка актуальных версий библиотек ПЕРЕД правкой `gradle/libs.versions.toml` (CLAUDE.md §7). Global user-level.
- **postgres** - инспекция dev-схемы PostgreSQL 17 (db-expert). Global.
- Принцип: не плодить MCP, lazy-load. firecrawl/magic/playwright доступны глобально по необходимости (writer-research, UI).
