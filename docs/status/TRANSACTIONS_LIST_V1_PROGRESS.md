# Transactions List v1 Progress

Status: In progress
Issue: #6
Branch: `feat/transactions-list-v1`

## Objective

Replace the `MainRoute.Transactions` placeholder with a production transaction-history screen backed by the existing Application, Repository, Data, SQLDelight, and Ledger layers.

## Verified Starting Gaps

- `MainRoute.Transactions` currently falls through to `MainPlaceholderDestination`.
- The write repository exposes only `record(...)`; no transaction-history read contract existed.
- No `ListTransactionsUseCase`, `TransactionsViewModel`, or production Transactions screen existed.
- README and roadmap status text still point to already-completed milestones.

## Completed In Read-Path Slice

- Added immutable transaction-history record and split models in the Domain layer.
- Added a separate read-only `TransactionHistoryRepository` contract so existing write-repository fakes remain source-compatible.
- Added deterministic SQLDelight retrieval of active transactions with split, account, category, currency, and ledger-entry context.
- Implemented the read contract in `SQLDelightTransactionRepository`.
- Added `ListTransactionsUseCase` and UI-ready `TransactionSummary` mapping.
- Added transfer source/destination resolution from persisted ledger direction and account type.
- Registered the read repository and use case in Koin.
- Added focused Application and SQLDelight integration tests.

## Remaining Work

- Validate the read-path slice locally and resolve generated SQLDelight/formatting issues, if any.
- Implement Transactions UI states, ViewModel, navigation, previews, and focused tests.
- Wire add transaction and refresh-after-save flows.
- Update stale roadmap/status documentation and final validation evidence.

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
