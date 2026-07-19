# Budgets Screen v1 Progress

Status: In progress — cross-layer audit pending
Issue: #10
Branch: `feat/budgets-screen-v1`
Draft PR: pending

## Objective

Replace the budget-management navigation gap with a production Budgets screen backed by the existing Domain, Application, budget-engine, Data, SQLDelight, Bootstrap, and shared UI layers.

## Audit Checklist

- Budget domain models and repository contracts.
- Application use cases and validation rules.
- Budget-engine ownership and deterministic calculation boundaries.
- SQLDelight schema, queries, migrations, and repository adapters.
- Bootstrap/Koin registrations.
- Existing category and transaction-history integration points.
- Navigation route and placeholder state.
- Existing shared UI components and approved reference constraints.

## Planned Implementation Slice

- Add only verified missing read/write contracts.
- Add deterministic budget list/summary models outside UI.
- Add `BudgetsViewModel` and immutable UI state/actions.
- Add loading, empty, error, populated, create/edit, validation, and feedback states.
- Wire the production route into navigation.
- Add focused Application, Data/integration, ViewModel, navigation, and presentation tests.

## Architecture Constraints

- No repository, SQLDelight, or engine access from UI.
- No monetary or spend aggregation calculations inside Composables.
- Preserve immutable ledger history and existing category/transaction paths.
- Avoid schema changes unless the audit proves a blocker.
- Preserve Kotlin Multiplatform commonMain compatibility.

## Validation Pending

- Shared metadata compilation.
- Focused Application, Data, budget-engine, Bootstrap, and UI tests.
- `ktlintCheck`.
- `detekt`.
- SQLDelight migration verification.
- `git diff --check` and clean working tree.
