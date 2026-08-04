# Database SQLDelight Test Driver Scope

Status: Ready for Review
Objective: Scope the SQLDelight SQLite test driver in `shared/database` to Android unit tests without changing production database behavior.
Branch: `fix/database-sqlite-driver-test-scope`
Scope: `shared/database` build configuration and focused validation
Issue: `https://github.com/im-tnyx/Tio-Ledger/issues/23`
Pull Request: `https://github.com/im-tnyx/Tio-Ledger/pull/24`
Created: `2026-08-04`
Last Updated: `2026-08-04`

## Required Context

- `.ai/core/workflow-rules.md`
- `.ai/core/architecture.md`
- `.github/PUSH_TEMPLATE.md`
- `docs/engineering-guidelines.md`
- `docs/definition-of-done.md`
- `shared/database/build.gradle.kts`
- `shared/data/build.gradle.kts`
- `shared/bootstrap/build.gradle.kts`

## Constraints

- Do not change SQLDelight schema or migrations.
- Do not change production database, repository, ledger, financial, UI, or navigation behavior.
- Keep the change limited to test dependency source-set scope and required task-state documentation.
- Record only validation that actually runs.

## Decisions

- Use `androidUnitTest.dependencies` for `libs.sqldelight.sqlite.driver`, matching the established `shared/data` and `shared/bootstrap` source-set pattern.
- Keep `libs.kotlin.test` in `commonTest.dependencies`.
- Treat this as a focused build/tooling correction, not a product or architecture change.

## Progress

- [x] Confirm PR #22 merged and `main` contains an idle task pointer.
- [x] Create the dedicated branch from merged `main`.
- [x] Inspect issue #23, repository rules, and established module patterns.
- [x] Apply the source-set dependency correction.
- [x] Open draft PR #24 for focused review and CI validation.
- [x] Confirm final-head CI validation passes.
- [x] Record validation evidence and prepare the PR for review.

## Validation

GitHub Actions run `30908098644`:

- `Targeted KMP validation`: PASS.
  - Shared metadata compilation: PASS.
  - Critical tests: PASS.
  - `ktlintCheck`: PASS.
  - `detekt`: PASS.
- `SQLDelight migration verification`: PASS.
- GitHub compare: branch started from merged `main`, 0 commits behind before final status-only commits.
- Local `git diff --check`: not available in the connected environment.

## Changed Files

- `.ai/current.md`
- `.ai/tasks/shared/local-20260804-database-sqlite-driver-test-scope.md`
- `shared/database/build.gradle.kts`

## Next Action

Confirm final status-only commit CI parity, mark PR #24 ready for review, and merge only after explicit authorization.
