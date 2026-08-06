# Shared Reminder Planner V1

Status: In Progress
Objective: Implement the deterministic shared EMI and budget reminder planner plus Application read orchestration from issue #42.
Branch: `feat/shared-reminder-planner-v1`
Scope: `shared:notifications`, `shared:application`, `shared:bootstrap`, focused tests, CI coverage, and applicable architecture documentation
Created: `2026-08-06`
Last Updated: `2026-08-06`
Issue: `https://github.com/im-tnyx/Tio-Ledger/issues/42`
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

## Progress

- [x] Merge and close out the canonical specification task.
- [x] Inspect current module dependencies, loan schedule models, budget summaries, and Bootstrap graph.
- [ ] Implement shared immutable contracts and deterministic planner.
- [ ] Add focused shared planner tests.
- [ ] Implement Application orchestration and DTO mapping.
- [ ] Register planner/use case through Bootstrap/Koin and add resolution coverage.
- [ ] Add direct notification module CI coverage.
- [ ] Update module/architecture documentation where runtime dependency direction changes.
- [ ] Run exact-head CI and complete PR review.

## Validation

Not run yet.

## Changed Files

- This task file.
- `.ai/current.md`.

## Next Action

Implement shared notification contracts and deterministic planner with focused common tests.
