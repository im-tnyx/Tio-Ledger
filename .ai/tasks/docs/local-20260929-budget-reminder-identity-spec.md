# Budget Reminder Identity Spec (Time-Zone Stable)

Status: Blocked
Objective: Get an explicit, approved canonical definition of a time-zone-stable budget reminder identity in `docs/emi-budget-reminders-v1.md` for issue #64, before any production change.
Branch: `docs/budget-reminder-identity-spec`
Scope: `docs` specification and `.ai` continuity only; no production Kotlin
Created: `2026-09-29`
Last Updated: `2026-09-29`
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

## Proposal (awaiting approval on #64)

- Identity `(budgetId, periodType, periodStartDate, status)`, `periodStartDate` = local calendar `LocalDate` (ISO in the key); budget engine exposes the date additively.
- Legacy instant-keyed receipts: shared planner also treats the candidate's legacy key as delivered (no Android change, no migration).
- Full question-by-question analysis and test list: #64 comment.

## Progress

- [x] Source-of-truth audit (spec, #41, #42, budget engine, notifications, application, Android consumers, tests).
- [x] Spec amendment proposal posted on #64.
- [ ] Explicit approval of the identity semantics.
- [ ] Spec amendment PR (docs only).
- [ ] Separate implementation task/PR after spec merge.

## Validation

- Not run (documentation/continuity only).

## Changed Files

- `.ai/current.md`
- `.ai/tasks/docs/local-20260929-budget-reminder-identity-spec.md`
- `.ai/tasks/android/local-20260812-reminder-validation-followup.md`

## Next Action

Get an explicit decision on the #64 proposal (identity fields, date encoding, legacy-receipt strategy); then amend `docs/emi-budget-reminders-v1.md` on this branch.
