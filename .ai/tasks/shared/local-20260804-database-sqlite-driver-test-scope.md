# Database SQLDelight Test Driver Scope

Status: In Progress
Objective: Scope the SQLDelight SQLite test driver in `shared/database` to Android unit tests without changing production database behavior.
Branch: `fix/database-sqlite-driver-test-scope`
Scope: `shared/database` build configuration and focused validation
Issue: `https://github.com/im-tnyx/Tio-Ledger/issues/23`
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
- [ ] Apply the source-set dependency correction.
- [ ] Open a focused PR and run CI validation.
- [ ] Update validation evidence and mark ready for review when checks pass.

## Validation

- Not run yet.

## Changed Files

- `.ai/tasks/shared/local-20260804-database-sqlite-driver-test-scope.md`

## Next Action

Update `.ai/current.md`, apply the `shared/database/build.gradle.kts` correction, and open a draft PR for CI validation.
