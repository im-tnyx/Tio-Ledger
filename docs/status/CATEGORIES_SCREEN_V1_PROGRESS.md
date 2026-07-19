# Categories Screen v1 Progress

Status: In progress
Issue: #8
Branch: `feat/categories-screen-v1`

## Objective

Replace the current category-management navigation gap with a production Categories screen backed by the existing Domain, Application, Data, SQLDelight, Bootstrap, and shared UI layers.

## Starting Audit

Pending verification before implementation:

- Existing category domain models and repository contracts.
- Existing SQLDelight category queries and schema capabilities.
- Existing Application use cases used by Transaction Entry.
- Current navigation destination or placeholder for category management.
- Existing design-system components and approved reference materials.

## Planned Slice

- Deterministic active-category read path.
- Grouped income and expense presentation.
- Loading, empty, error, create, and validation states.
- Application-layer orchestration for category creation.
- Koin and navigation wiring.
- Focused application, repository/integration, ViewModel, and navigation tests.

## Architecture Constraints

- No repository or SQLDelight access from UI.
- No financial calculation or ledger posting in UI/ViewModel.
- Avoid schema changes unless a verified blocker exists.
- Preserve Transaction Entry compatibility and commonMain behavior.

## Validation Gates

- Shared metadata compilation.
- Critical Application, Data, and UI tests.
- `ktlintCheck`.
- `detekt`.
- SQLDelight migration verification.
- `git diff --check` and clean working tree.
