# Budget Reminder Time-Zone-Stable Identity

Status: In Progress
Objective: Implement the canonical budget reminder identity `(budgetId, periodType, periodStartDate, status)` approved in PR #65 so a time-zone change within the same local budget period never re-delivers a reminder.
Branch: `fix/budget-reminder-timezone-identity`
Scope: `shared/budget-engine`, `shared/application`, `shared/notifications`, Android reconciliation tests; no Android production change
Created: `2026-09-29`
Last Updated: `2026-09-29`
Issue: `#64`
Related: `#56`, `#43`

## Required Context

- `.ai/core/workflow-rules.md`
- `.ai/core/financial-rules.md`
- `docs/emi-budget-reminders-v1.md` (Budget Stable Identity, Pre-Canonical Budget Reminder Metadata, Testing Requirements)
- `shared/budget-engine/src/commonMain/kotlin/com/tioledger/budget/engine/BudgetCalculators.kt`
- `shared/application/src/commonMain/kotlin/com/tioledger/application/usecase/budget/BudgetSummaryUseCases.kt`
- `shared/application/src/commonMain/kotlin/com/tioledger/application/usecase/notification/PlanRemindersUseCase.kt`
- `shared/notifications/src/commonMain/kotlin/com/tioledger/notifications/`
- `apps/android/src/main/kotlin/com/tioledger/apps/android/reminders/ReminderReconciliationPlanner.kt`

## Constraints

- Budget engine stays authoritative: expose the already-computed local start date only; no change to period instants, spend, utilization, threshold, or status.
- No SQLDelight schema/migration; no financial write; no shared legacy-key aliasing; no Android identity parsing/re-keying.
- Option A pre-canonical transition (approved): no explicit Android metadata migration/reset/versioning. Stale scheduled budget identities reconcile away through the existing `Cancel` path; old instant-keyed receipts stay inert and age out through bounded pruning; one canonical re-delivery of a currently active state after upgrade is accepted; preferences and financial state untouched.
- Do not merge the implementation PR without explicit authorization.

## Decisions

- `BudgetPeriodWindow.startDate: LocalDate` (engine-computed); `BudgetSummary.periodStartDate` copied from it.
- Key: `budget|<budgetId>|<PERIOD_TYPE>|<YYYY-MM-DD>|<STATUS>`.

## Progress

- [ ] Shared identity implementation and tests.
- [ ] Android Option A reconciliation regression test.
- [ ] Documentation (changelog, spec transition note).
- [ ] Full local validation.
- [ ] API 30 time-zone device revalidation.
- [ ] Implementation PR with exact-head CI.

## Validation

- Not run yet.

## Changed Files

- None yet.

## Next Action

Implement the shared identity change and focused tests.
