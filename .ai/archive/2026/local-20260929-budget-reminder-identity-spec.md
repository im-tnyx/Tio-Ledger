# Budget Reminder Identity Spec (Time-Zone Stable)

Status: Complete
Objective: Get an explicit, approved canonical definition of a time-zone-stable budget reminder identity in `docs/emi-budget-reminders-v1.md` for issue #64, before any production change.
Branch: `docs/budget-reminder-identity-spec`
Scope: `docs` specification and `.ai` continuity only; no production Kotlin
Created: `2026-09-29`
Completed: `2026-09-29`
Pull Request: `https://github.com/im-tnyx/Tio-Ledger/pull/65`
Merge Commit: `7d5cbf0ad5605c1fa8e6326b3d675d95df161b97`
Issue: `#64`
Related: `#56`, `#43`, `#41` (approved contract), `#42` (shared planner)

## Required Context

- `.ai/core/workflow-rules.md`
- `.ai/core/financial-rules.md`
- `docs/emi-budget-reminders-v1.md`
- `docs/references/notes/budgets.md`
- `shared/budget-engine/src/commonMain/kotlin/com/tioledger/budget/engine/BudgetCalculators.kt`
- `shared/notifications/src/commonMain/kotlin/com/tioledger/notifications/ReminderModels.kt`
- `shared/application/src/commonMain/kotlin/com/tioledger/application/usecase/notification/PlanRemindersUseCase.kt`

## Constraints

- Spec-first: no production code until the identity semantics are explicitly approved on #64.
- No SQLDelight schema/migration; receipts and scheduled snapshots stay platform-local, non-financial metadata.
- No financial history, ledger, budget, or balance mutation; no spend/threshold/period rule duplication outside shared engines.
- `apps/android` keeps treating the identity key as opaque.
- Keep PR #62 scope (merged) and #60/#61 out of this task.

## Findings

- Budget periods are recurring local-calendar windows derived by `BudgetPeriodCalculator` from the local date in an explicit time zone (weekly = ISO Monday start, monthly = 1st, yearly = 1 Jan); they are not persisted (`budgets.md`).
- The calculator discards the local start date and exposes only instants; `BudgetSummary.periodStartInclusive` → `ReminderIdentity.Budget.key` embeds that instant, so the same period gets a different key per time zone.
- #41/#42 approved `(budgetId, periodStartInclusive, status)` plus "no recurring notification while the same state remains active", but never typed the field or covered time-zone changes; #41 left "no unresolved timezone ambiguity" unchecked.
- No existing test covers budget identity across time zones.

## Decisions (approved on #64, 2026-09-29)

- Identity `(budgetId, periodType, periodStartDate, status)`; `periodStartDate` = local-calendar `kotlinx.datetime.LocalDate` resolved by the budget engine, rendered as ISO text (`budget|<id>|MONTHLY|2026-09-01|EXCEEDED`); no UTC-midnight `Long` encoding.
- `periodType` participates (MONTHLY and YEARLY both start on 1 January).
- Timezone: same resolved period keeps the identity; crossing a real period boundary legitimately changes it; no notification-side timezone special cases; spend windows unchanged.
- Superseded: the earlier shared-side legacy-alias suppression proposal. Pre-canonical instant keys are pre-v1 non-financial platform metadata; the implementation must define a scoped Android-local metadata transition (receipts/scheduled work only; never preferences or financial state). No SQLDelight migration.

## Progress

- [x] Source-of-truth audit (spec, #41, #42, budget engine, notifications, application, Android consumers, tests).
- [x] Spec amendment proposal posted on #64.
- [x] Explicit approval of the identity semantics (decision comment on #64).
- [x] Canonical amendment to `docs/emi-budget-reminders-v1.md`.
- [x] Docs-only spec PR #65 validated (CI run #420) and merged as `7d5cbf0`.
- [x] Implementation handed off to `.ai/tasks/shared/local-20260929-budget-reminder-timezone-identity.md`.

## Validation

- Documentation/continuity only: `git diff --check`; no Kotlin/Gradle/schema/UI change.

## Changed Files

- `docs/emi-budget-reminders-v1.md`
- `.ai/current.md`
- `.ai/tasks/docs/local-20260929-budget-reminder-identity-spec.md`
- `.ai/tasks/android/local-20260812-reminder-validation-followup.md`

## Next Action

None. Implementation continues in the shared #64 implementation task.
