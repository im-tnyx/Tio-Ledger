# Budget Reminder Time-Zone-Stable Identity

Status: Complete
Objective: Implement the canonical budget reminder identity `(budgetId, periodType, periodStartDate, status)` approved in PR #65 so a time-zone change within the same local budget period never re-delivers a reminder.
Branch: `fix/budget-reminder-timezone-identity`
Scope: `shared/budget-engine`, `shared/application`, `shared/notifications`, Android reconciliation tests; no Android production change
Created: `2026-09-29`
Completed: `2026-09-29`
Pull Request: `https://github.com/im-tnyx/Tio-Ledger/pull/67`
Merge Commit: `743474485b2055d86eccfd90b36eca5d9193db3d`
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

- [x] Shared identity implementation and tests (commit `515c054`).
- [x] Android Option A reconciliation regression test.
- [x] Documentation (changelog, spec transition note).
- [x] Full local validation.
- [x] API 30 time-zone device revalidation.
- [x] PR #67 exact-head CI run #422 green; squash-merged as `7434744`; #64 closed; #56/#43 synced.

## Validation

- Local on `515c054` (production code identical to the tested APK): shared metadata compile, critical shared tests + `:shared:budget-engine:test`, `:apps:android:compileDebugKotlin`/`testDebugUnitTest`/`assembleDebug`, migration verification, `ktlintCheck`, `detekt`, `git diff --check` — all pass.
- Device `TioLedger_Android11` (API 30), existing production-UI data (budget Groceries MONTHLY INR 100.00, spent INR 105.00, EXCEEDED; no injection), 2026-09-29:
  - Upgrade-install over pre-canonical build: reconcile SUCCESS, canonical `EXCEEDED` delivered once (accepted Option A transition), receipt `budget|<id>|MONTHLY|2026-09-01|EXCEEDED` recorded; legacy receipts and `budget_enabled` untouched.
  - `Asia/Kolkata → America/New_York` (same September period): reconcile SUCCESS, no delivery worker, no new receipt — PASS (previously duplicated).
  - Back to `Asia/Kolkata`: reconcile SUCCESS, no delivery — PASS. Force-stop + relaunch: no delivery — PASS.
  - DB read-only: 3 transactions / 3 splits / 6 ledger entries / 1 budget / 1 account, DEBIT = CREDIT = 10500, budget `updated_at` unchanged.
  - Not run on device: period-boundary crossing (needs emulator clock at a month boundary; covered by engine/application tests); legacy scheduled-record cancellation (no scheduled records existed on device; covered by unit test).

## Changed Files

- `shared/budget-engine/.../BudgetCalculators.kt`, `shared/application/.../BudgetSummaryUseCases.kt`, `.../PlanRemindersUseCase.kt`, `shared/notifications/.../ReminderModels.kt`, `.../ReminderPlanner.kt`
- Tests: `BudgetCalculatorsTest`, `BudgetSummaryUseCaseTest`, `PlanRemindersUseCaseTest`, `ReminderPlannerTest`, Android `ReminderReconciliationPlannerTest`
- Docs: `docs/architecture-changelog.md`, `docs/emi-budget-reminders-v1.md` (Android transition decision)

## Next Action

None. #56 remaining work continues in the Android reminder validation follow-up task.
