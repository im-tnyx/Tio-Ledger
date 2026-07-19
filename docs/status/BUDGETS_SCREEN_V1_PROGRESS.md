# Budgets Screen v1 Progress

Status: In progress — budget summary foundation ready for local validation
Issue: #10
Branch: `feat/budgets-screen-v1`
Draft PR: #11

## Objective

Replace the budget-management navigation gap with a production Budgets screen backed by the existing Domain, Application, budget-engine, Data, SQLDelight, Bootstrap, and shared UI layers.

## Completed Audit

- `shared:budget-engine` existed only as an empty configured module.
- The frozen SQLDelight schema already contained `budgets` and `budget_periods` tables.
- No Budget Domain model, repository contract, Application use case, SQLDelight query file, Data adapter, Koin registration, or production UI path existed.
- `TioIconToken.Budget` existed, but `MainRoute.Budgets` did not.
- Existing Categories and Transaction History paths provide the category scope and immutable expense records required for spend aggregation.
- No database schema change is required for Budgets Screen v1.
- V1 supports weekly, monthly, and yearly recurring budgets. Custom periods remain unsupported until explicit start/end editing is designed.

## Completed CRUD Foundation Slice

- Added `Budget`, `BudgetPeriodType`, and `BudgetRepository` Domain contracts.
- Added budget-created and budget-updated Domain events.
- Added a typed `BudgetNotFound` repository error.
- Added deterministic SQLDelight budget insert, update, list, and lookup queries.
- Added SQLDelight-to-Domain budget mapping and `SQLDelightBudgetRepository`.
- Added list, create, and update Application use cases.
- Added positive amount, currency, category type, supported period, and duplicate scope validation.
- Registered the budget repository and use cases in Bootstrap/Koin diagnostics.
- Added focused Application and SQLDelight integration tests.

## CRUD Foundation Validation Passed

```text
./gradlew :shared:core:compileKotlinMetadata :shared:domain:compileKotlinMetadata :shared:database:compileKotlinMetadata :shared:data:compileKotlinMetadata :shared:application:compileKotlinMetadata :shared:bootstrap:compileKotlinMetadata --no-daemon --console=plain --stacktrace
BUILD SUCCESSFUL in 15s
17 actionable tasks: 8 executed, 9 up-to-date

./gradlew :shared:application:test :shared:data:test --no-daemon --console=plain --stacktrace
BUILD SUCCESSFUL in 40s
191 actionable tasks: 55 executed, 136 up-to-date
```

## Completed Budget Summary Slice

- Added timezone-aware weekly, monthly, and yearly current-period calculation in `shared:budget-engine`.
- Added deterministic category-scoped expense aggregation from immutable transaction-history records.
- Added fixed-integer utilization permille, remaining amount, and on-track/warning/reached/exceeded status calculation without floating-point money math.
- Added `BudgetSummary` and `ListBudgetSummariesUseCase` in the Application layer.
- Added repository-failure, invalid-time-zone, period-boundary, category, currency, and status tests.
- Registered budget calculators and summary use case in Bootstrap/Koin diagnostics.

## Remaining Implementation Slices

- Add `BudgetsViewModel`, immutable UI state/actions, and production loading/empty/error/list/create/edit states.
- Add `MainRoute.Budgets`, route wiring, navigation entry, previews, and UI tests.
- Finalize roadmap/status documentation and validation evidence.

## Architecture Constraints

- No repository, SQLDelight, or engine access from UI.
- No monetary or spend aggregation calculations inside Composables.
- Preserve immutable ledger history and existing category/transaction paths.
- No schema change.
- Preserve Kotlin Multiplatform commonMain compatibility.

## Validation Pending

- Budget-engine, Application summary, and Bootstrap metadata compilation.
- Focused budget-engine and Application tests.
- UI compile/tests after the production screen slice lands.
- `ktlintCheck`.
- `detekt`.
- SQLDelight migration verification.
- `git diff --check` and clean working tree.
