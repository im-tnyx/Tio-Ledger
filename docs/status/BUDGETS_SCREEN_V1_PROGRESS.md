# Budgets Screen v1 Progress

Status: Complete — implemented, reviewed, and locally validated
Issue: #10
Branch: `feat/budgets-screen-v1`
PR: #11

## Objective

Replace the budget-management navigation gap with a production Budgets screen backed by the Domain, Application, budget-engine, Data, SQLDelight, Bootstrap, and shared UI layers.

## Completed Audit

- `shared:budget-engine` existed only as an empty configured module.
- The frozen SQLDelight schema already contained `budgets` and `budget_periods` tables.
- No Budget Domain model, repository contract, Application use case, SQLDelight query file, Data adapter, Koin registration, or production UI path existed.
- `TioIconToken.Budget` existed, but `MainRoute.Budgets` did not.
- Existing Categories and Transaction History paths provided the category scope and immutable expense records required for spend aggregation.
- No database schema change was required for Budgets Screen v1.
- V1 supports weekly, monthly, and yearly recurring budgets. Custom periods remain unsupported until explicit start/end editing is designed.

## Completed CRUD Foundation

- Added `Budget`, `BudgetPeriodType`, and `BudgetRepository` Domain contracts.
- Added budget-created and budget-updated Domain events.
- Added a typed `BudgetNotFound` repository error.
- Added deterministic SQLDelight budget insert, update, list, and lookup queries.
- Added SQLDelight-to-Domain budget mapping and `SQLDelightBudgetRepository`.
- Added list, create, and update Application use cases.
- Added positive amount, currency, category type, supported period, and duplicate scope validation.
- Registered the budget repository and use cases in Bootstrap/Koin diagnostics.
- Added focused Application and SQLDelight integration tests.

## Completed Budget Summary Foundation

- Added timezone-aware weekly, monthly, and yearly current-period calculation in `shared:budget-engine`.
- Added deterministic category-scoped expense aggregation from immutable transaction-history records.
- Added fixed-integer utilization permille, remaining amount, and on-track/warning/reached/exceeded status calculation without floating-point money math.
- Added `BudgetSummary` and `ListBudgetSummariesUseCase` in the Application layer.
- Added repository-failure, invalid-time-zone, period-boundary, category, currency, and status tests.
- Registered budget calculators and summary orchestration in Bootstrap/Koin diagnostics.

## Completed Production UI

- Added the official Google Play fallback reference note, original Tio UI specification, navigation definition, intentional deviations, accessibility plan, and acceptance checklist.
- Added immutable `BudgetsUiState`, row/editor/category models, and typed actions.
- Added `BudgetsViewModel` backed only by Application use cases and the shared `IdGenerator`.
- Added loading, empty, repository-error, populated, create, edit, validation, persistence-error, and success states.
- Added deterministic money-text parsing without `Float` or `Double` money calculations.
- Added accessible progress cards showing target, spent, remaining, date range, utilization, and textual status.
- Added all-expenses and expense-category scope selection with weekly, monthly, and yearly editing.
- Added `MainRoute.Budgets`, primary bottom-navigation entry, root-route wiring, and Koin registration.
- Kept Reports registered in the main graph while Budgets occupies the fifth primary-navigation slot for v1.
- Added light, dark, and editor previews, ViewModel tests, and navigation tests.

## Final Review Fixes

- Distinguish a missing or archived category scope from the real all-expenses scope by displaying `Unavailable category`.
- Prevent the editor and category picker from being composed as overlapping dialogs.
- Added focused ViewModel regression coverage for unavailable category scope.

## Final Local Validation

```text
./gradlew :shared:core:compileKotlinMetadata :shared:domain:compileKotlinMetadata :shared:budget-engine:compileKotlinMetadata :shared:application:compileKotlinMetadata :shared:database:compileKotlinMetadata :shared:data:compileKotlinMetadata :shared:bootstrap:compileKotlinMetadata :shared:ui:compileKotlinMetadata --no-daemon --console=plain --stacktrace
BUILD SUCCESSFUL in 21s
19 actionable tasks: 9 executed, 10 up-to-date

./gradlew :shared:budget-engine:test :shared:application:test :shared:data:test :shared:ui:test --no-daemon --console=plain --stacktrace
BUILD SUCCESSFUL in 38s
257 actionable tasks: 15 executed, 242 up-to-date

./gradlew ktlintCheck detekt --no-daemon --console=plain --stacktrace
BUILD SUCCESSFUL in 37s
70 actionable tasks: 4 executed, 66 up-to-date

./gradlew :shared:database:verifyCommonMainTioLedgerDatabaseMigration --no-daemon --no-parallel --max-workers=1 --console=plain --stacktrace
BUILD SUCCESSFUL in 18s
10 actionable tasks: 1 executed, 9 up-to-date
```

## Post-Review Validation

```text
./gradlew :shared:application:compileKotlinMetadata :shared:application:test :shared:ui:compileKotlinMetadata :shared:ui:test --no-daemon --console=plain --stacktrace
BUILD SUCCESSFUL in 47s
237 actionable tasks: 33 executed, 204 up-to-date

./gradlew ktlintCheck detekt --no-daemon --console=plain --stacktrace
BUILD SUCCESSFUL in 16s
70 actionable tasks: 3 executed, 67 up-to-date

git diff --check
(no output)

git status
On branch feat/budgets-screen-v1
Your branch is up to date with 'origin/feat/budgets-screen-v1'.
nothing to commit, working tree clean
```

## Architecture Constraints Preserved

- No repository, SQLDelight, or engine access from UI.
- No monetary or spend aggregation calculations inside Composables.
- Immutable ledger and transaction history remain the source for spend derivation.
- No schema change.
- Kotlin Multiplatform `commonMain` compatibility is preserved.
- Custom periods remain explicitly unsupported in v1.

## Outcome

Budgets Screen v1 is implementation-complete, final-review fixes are validated, and PR #11 is ready for explicit merge approval.