# Notification Reminders V1 Specification

Status: Ready for Review
Objective: Define the approved EMI and budget reminders v1 product rules, shared planner boundary, Android adapter boundary, and implementation split before production code.
Branch: `docs/notifications-reminders-v1-spec`
Scope: `docs`, issue #41, and AI continuity only
Created: `2026-08-06`
Last Updated: `2026-08-06`
Issue: `https://github.com/im-tnyx/Tio-Ledger/issues/41`
Pull Request: `https://github.com/im-tnyx/Tio-Ledger/pull/44`
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

## Progress

- [x] Audit current repository, open PRs/issues, and remote branches.
- [x] Approve conservative v1 decisions in issue #41 under user principal-judgment authorization.
- [x] Create dedicated documentation branch.
- [x] Add canonical reminders v1 specification.
- [x] Update documentation index and roadmap.
- [x] Create shared-planner issue #42 and Android-adapter issue #43.
- [x] Run implementation-documentation head CI and review the documentation-only diff.
- [x] Open draft PR #44.
- [ ] Run final task-state head CI and complete final PR review.
- [ ] Merge specification PR and archive this task.

## Validation

GitHub Actions CI run #358 on head `bc5884972d15ab348464ca0436dc2305721e3c77` passed:

- Shared metadata compilation.
- Critical tests.
- SQLDelight migration verification.
- `ktlintCheck`.
- `detekt`.

Branch comparison was 0 commits behind `main`. The diff was limited to five documentation and AI continuity files. Local `git diff --check` was not run because this session uses the GitHub connector rather than a repository checkout.

## Changed Files

- `.ai/current.md`
- This task file
- `docs/README.md`
- `docs/emi-budget-reminders-v1.md`
- `docs/implementation-roadmap.md`

## Next Action

Run exact-head CI for the Ready-for-Review task state, complete the final scope/review audit, and merge PR #44 if all checks remain green.
