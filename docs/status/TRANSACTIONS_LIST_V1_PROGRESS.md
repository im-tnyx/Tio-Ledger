# Transactions List v1 Progress

Status: In progress
Issue: #6
Branch: `feat/transactions-list-v1`
Draft PR: #7

## Objective

Replace the `MainRoute.Transactions` placeholder with a production transaction-history screen backed by the existing Application, Repository, Data, SQLDelight, and Ledger layers.

## Completed Read Path

- Added immutable transaction-history record and split models in Domain.
- Added a separate read-only `TransactionHistoryRepository` contract without breaking the existing write contract.
- Added deterministic newest-first SQLDelight retrieval with split, account, category, currency, and ledger-entry context.
- Implemented `ListTransactionsUseCase` and UI-ready transaction summaries.
- Added Application and SQLDelight integration tests for income, expense, and transfer history.
- Registered the history repository and use case in Koin.

## Completed UI Slice

- Added `TransactionsUiState`, row models, and retry actions.
- Added `TransactionsViewModel` backed only by `ListTransactionsUseCase`.
- Added production loading, empty, error, and populated screen states.
- Added stable UTC calendar-date labels and fixed-minor-unit amount formatting.
- Added income, expense, transfer, loan, repayment, and adjustment presentation mapping.
- Added the add-transaction FAB and real bottom-navigation callbacks.
- Replaced the `MainRoute.Transactions` placeholder with `TransactionsRoute`.
- Added light/dark previews plus focused ViewModel and navigation tests.

## Validation Evidence

Completed locally for the read-path slice:

- Shared Domain, Application, Database, Data, and Bootstrap metadata compilation: passed.
- `:shared:application:test :shared:data:test`: passed.
- `ktlintCheck`: passed after replacing the generated-query mapper lambda with a named function reference.
- `detekt`: passed.

Pending after the UI slice:

- `:shared:ui:compileKotlinMetadata`
- `:shared:ui:test`
- Full targeted metadata compilation and critical shared tests
- SQLDelight migration verification
- Final `ktlintCheck`, `detekt`, and `git diff --check`

## Remaining Work

- Resolve any UI compilation, formatting, or test issues found by local validation.
- Confirm persisted transactions refresh after returning from Transaction Entry.
- Refresh stale README and roadmap milestone text.
- Record final validation evidence and mark PR ready only after all gates pass.

## Architecture Constraints

- No direct repository or SQLDelight access from UI.
- No financial calculations or ledger posting in UI/ViewModel.
- No database schema change.
- Preserve immutable ledger history and atomic transaction persistence.
