# Shared Reminder Planner V1

Status: Ready for Review
Objective: Implement the deterministic shared EMI and budget reminder planner plus Application read orchestration from issue #42.
Branch: `feat/shared-reminder-planner-v1`
Scope: `shared:notifications`, `shared:application`, `shared:bootstrap`, focused tests, CI coverage, and applicable architecture documentation
Created: `2026-08-06`
Last Updated: `2026-08-06`
Issue: `https://github.com/im-tnyx/Tio-Ledger/issues/42`
Pull Request: `https://github.com/im-tnyx/Tio-Ledger/pull/47`
Specification: `docs/emi-budget-reminders-v1.md`

## Required Context

- `.ai/core/architecture.md`
- `.ai/core/workflow-rules.md`
- `AGENTS.md`
- `README.md`
- `docs/README.md`
- `docs/product-requirements.md`
- `docs/architecture.md`
- `docs/module-design.md`
- `docs/engineering-guidelines.md`
- `docs/definition-of-done.md`
- `docs/implementation-roadmap.md`
- `docs/emi-budget-reminders-v1.md`
- `shared/notifications/build.gradle.kts`
- `shared/application/src/commonMain/kotlin/com/tioledger/application/usecase/budget/BudgetSummaryUseCases.kt`
- `shared/application/src/commonMain/kotlin/com/tioledger/application/usecase/loan/LoanUseCases.kt`
- `shared/bootstrap/src/commonMain/kotlin/com/tioledger/bootstrap/di/TioModules.kt`

## Constraints

- Planner remains pure, deterministic, repository-free, timezone-explicit, and platform-neutral.
- Application owns repository orchestration and immutable DTO mapping.
- Persisted loan schedules and existing budget summaries remain authoritative.
- No Android/iOS/Wear scheduling, permissions, settings UI, delivery receipts, or localized notification copy.
- No SQLDelight schema/migration or financial write path.
- No `Float` or `Double` money arithmetic.

## Decisions

- Persisted loan due dates are interpreted as UTC calendar dates, then scheduled at 09:00 in the requested timezone.
- Budget eligible-state plans are immediate at the injected current timestamp and suppressed by delivered stable identity keys.
- Shared plans expose semantic destinations/content; Application maps them to Application-owned immutable views.
- Existing LoanRepository and ListBudgetSummariesUseCase read paths are reused; no new repository contract is introduced.
- Android permission, scheduling, cancellation, delivery receipts, lifecycle reconciliation, and destination delivery remain isolated to issue #43.

## Progress

- [x] Merge and close out the canonical specification task.
- [x] Inspect current module dependencies, loan schedule models, budget summaries, and Bootstrap graph.
- [x] Implement shared immutable contracts and deterministic planner.
- [x] Add focused shared planner tests.
- [x] Implement Application orchestration and DTO mapping.
- [x] Register planner/use case through Bootstrap/Koin and add resolution coverage.
- [x] Add direct notification module CI coverage.
- [x] Update module/architecture documentation for the validated dependency direction.
- [ ] Verify final documentation/status head in CI, complete PR review, and merge.

## Validation

Implementation head `37e281be59a4c8a2cf7e9a7cc9d33072e81fbcc6` passed GitHub Actions CI run #370 (`31102661195`):

- SQLDelight migration verification: pass.
- Shared metadata compilation: pass.
- Critical shared-notifications, Application, and Bootstrap tests: pass.
- ktlint: pass.
- detekt: pass.

Final documentation/status head requires one exact-head CI run before merge.

## Changed Areas

- `shared:notifications`: immutable contracts, deterministic planner, validation, and tests.
- `shared:application`: read-only candidate orchestration and Application-owned DTO mapping.
- `shared:bootstrap`: Koin registration and graph-resolution coverage.
- `.github`: direct notifications compile/test validation commands.
- `README.md`, `docs/README.md`, `docs/module-design.md`, `docs/implementation-roadmap.md`, and `docs/architecture-changelog.md`.
- `.ai/current.md` and this task file.

## Safety Review

- No SQLDelight schema or migration changes.
- No transaction, ledger, balance, loan-payment, or budget write path.
- Money remains in existing precise `Money` values; no floating-point arithmetic.
- Planner is repository-free and platform-neutral.
- Disabled reminder types skip unnecessary repository reads.
- Platform schedulers must consume semantic plans without duplicating business rules.

## Next Action

Verify final exact-head CI, review PR #47 diff and discussions, then mark ready and squash merge.
