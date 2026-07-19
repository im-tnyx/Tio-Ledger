# Transactions List v1 Progress

Status: In progress
Issue: #6
Branch: `feat/transactions-list-v1`

## Objective

Replace the `MainRoute.Transactions` placeholder with a production transaction-history screen backed by the existing Application, Repository, Data, SQLDelight, and Ledger layers.

## Verified Starting Gaps

- `MainRoute.Transactions` currently falls through to `MainPlaceholderDestination`.
- `TransactionRepository` exposes only `record(...)`; no transaction-history read contract exists.
- No `ListTransactionsUseCase`, `TransactionsViewModel`, or production Transactions screen exists.
- README and roadmap status text still point to already-completed milestones.

## Planned Work

- Define a narrow transaction-summary read model and repository contract.
- Implement deterministic newest-first SQLDelight retrieval with account/category context.
- Add Application use case and Data-layer mapping/tests.
- Implement Transactions UI states, ViewModel, navigation, previews, and focused tests.
- Wire add transaction and refresh-after-save flows.
- Update status documentation and validation evidence.

## Validation Gate

- Shared metadata compilation
- Critical shared tests
- `ktlintCheck`
- `detekt`
- SQLDelight migration verification
- `git diff --check`

## Architecture Constraints

- No direct repository or SQLDelight access from UI.
- No financial calculations or ledger posting in UI/ViewModel.
- No schema change unless a verified read-path blocker requires one.
- Preserve immutable ledger history and atomic transaction persistence.
