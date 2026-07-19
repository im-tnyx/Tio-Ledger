# Budgets Screen v1 Progress

Status: In progress — CRUD foundation ready for local validation
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
- Existing Categories and Transaction History paths provide the category scope and immutable expense records required for later spend aggregation.
- No database schema change is required for Budgets Screen v1.
- V1 will support weekly, monthly, and yearly recurring budgets. Custom periods remain unsupported until explicit start/end editing is designed.

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

## Remaining Implementation Slices

- Add pure current-period calculation and spend/progress aggregation in `shared:budget-engine`.
- Add an Application budget-summary read path using immutable transaction history.
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

- CRUD foundation metadata compilation and focused Application/Data tests.
- Budget-engine, summary, Bootstrap, and UI validation after their slices land.
- `ktlintCheck`.
- `detekt`.
- SQLDelight migration verification.
- `git diff --check` and clean working tree.
