# Notification Reminders V1 Specification

Status: Complete
Objective: Define the approved EMI and budget reminders v1 product rules, shared planner boundary, Android adapter boundary, and implementation split before production code.
Branch: `docs/notifications-reminders-v1-spec`
Scope: `docs`, issue #41, and AI continuity only
Created: `2026-08-06`
Completed: `2026-08-06`
Issue: `https://github.com/im-tnyx/Tio-Ledger/issues/41`
Pull Request: `https://github.com/im-tnyx/Tio-Ledger/pull/44`
Merge Commit: `95c56f1888032ff33ce3b52bc390b3948d59ed64`
Shared Planner Issue: `https://github.com/im-tnyx/Tio-Ledger/issues/42`
Android Adapter Issue: `https://github.com/im-tnyx/Tio-Ledger/issues/43`

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
- `docs/adr/README.md`
- `shared/notifications/build.gradle.kts`
- `shared/domain/src/commonMain/kotlin/com/tioledger/domain/model/LoanModels.kt`
- `shared/application/src/commonMain/kotlin/com/tioledger/application/usecase/budget/BudgetSummaryUseCases.kt`

## Constraints

- No production Kotlin, Gradle, SQLDelight, UI, platform scheduling, or notification permission changes.
- Preserve the frozen financial schema and immutable ledger history.
- Shared rules must be deterministic, timezone-explicit, repository-free, and testable.
- Platform adapters own permissions, scheduling, cancellation, local preferences, delivery receipts, and localized copy.
- No notification action may mutate financial data.
- Recurring-transaction prompts, iOS delivery, and independent Wear scheduling remain out of scope.

## Decisions

- EMI reminders: 3 local-calendar days before and on due day at 09:00 local time for pending installments of active loans.
- Budget reminders: transition-only for existing `WARNING`, `REACHED`, and `EXCEEDED` states.
- Global EMI and budget toggles only in v1.
- Android-first production delivery with iOS-compatible contracts and phone-mirrored Wear behavior.
- No exact-alarm permission solely for precise 09:00 delivery.
- Reminder settings and receipts are non-financial platform-local state; no financial-schema migration.

## Outcome

- [x] Repository, issues, PRs, and remote branches audited.
- [x] Conservative v1 product decisions approved in issue #41.
- [x] Canonical specification added at `docs/emi-budget-reminders-v1.md`.
- [x] Documentation index and Phase 7 roadmap updated.
- [x] Shared planner issue #42 and Android adapter issue #43 created.
- [x] Duplicate issue #45 closed as duplicate of #42.
- [x] PR #44 final diff reviewed and limited to five documentation/AI continuity files.
- [x] Final exact-head CI passed.
- [x] PR #44 squash-merged.
- [x] Issue #41 closed as completed.

## Validation

GitHub Actions CI run #360 on exact head `9476807be8f1e612adcad5530db9d8d5a180a8e6` passed:

- Shared metadata compilation.
- Critical tests.
- SQLDelight migration verification.
- `ktlintCheck`.
- `detekt`.

The branch was 0 commits behind `main`, contained only five documentation and AI continuity files, and had no unresolved review threads or submitted reviews. Local `git diff --check` was not run because the session used the GitHub connector.

## Changed Files

- `.ai/current.md`
- `.ai/tasks/docs/local-20260806-notification-reminders-v1-spec.md`
- `docs/README.md`
- `docs/emi-budget-reminders-v1.md`
- `docs/implementation-roadmap.md`

## Next Action

Start issue #42 from updated `main` after this post-merge closeout is merged and synchronized.
