# Primary Bottom Navigation Wiring

Status: Merged
Objective: Wire every visible production primary bottom-navigation item to its typed `MainRoute` while preserving canonical routes and selected-state behavior.
Branch: `fix/primary-bottom-navigation-wiring`
Scope: `shared/ui` navigation shell and affected screen route boundaries, focused navigation tests, and navigation validation documentation
Created: `2026-08-05`
Completed: `2026-08-05`
Issue: `https://github.com/im-tnyx/Tio-Ledger/issues/34`
Pull Request: `https://github.com/im-tnyx/Tio-Ledger/pull/36`
Merge Commit: `4ea7f907f7b1d9b7b4fa530a4d7938838cf6eba6`

## Constraints Preserved

- Kept `TioNavigationGraphs.main.bottomNavigationRoutes` as the canonical route source.
- Preserved Dashboard, Accounts, Transactions, Categories, and Budgets as the five primary destinations.
- Did not redesign Dashboard content or introduce a new screen hierarchy.
- Kept Reports, Loans, and Settings non-primary without falsely selecting a tab.
- Preserved detail, add, SMS-review, and back-navigation flows.
- Added no financial, persistence, schema, analytics, loan, notification, or automation changes.
- Reused existing `TioBottomNavigation` and typed route contracts.

## Completed Work

- [x] Added a shared, testable `MainBottomNavigationModel` derived from the canonical route graph.
- [x] Wired `AccountsRoute` and `AccountsScreen` through typed navigation callbacks.
- [x] Removed the hard-coded Accounts navigation list and ignored tap handler.
- [x] Wired Dashboard/placeholder navigation taps through `RootNavigationHost`.
- [x] Preserved existing working destination callbacks without speculative refactoring.
- [x] Added route-order, metadata, selected-state, typed-forwarding, and unknown-item tests.
- [x] Updated the navigation validation report to describe production wiring.
- [x] Removed the implementation-introduced constructor warning.
- [x] Completed final diff, branch-drift, review-thread, and scope audits.
- [x] Merged PR #36 and closed issue #34.

## Validation

Final head `879a8b230768abc6138337436f2978cb088d8d52` passed GitHub Actions CI run #348:

- Shared metadata compilation.
- Critical tests, including `MainBottomNavigationModelTest` and existing UI suites.
- SQLDelight migration verification.
- `ktlintCheck`.
- `detekt`.

Implementation head run #345 also passed before the documentation-complete commit. The branch was 0 commits behind `main`, the changed-file audit contained eight scoped files, and no review threads were unresolved.

## Outcome

Primary bottom navigation is consistently wired for Accounts and the Dashboard placeholder. Existing primary and non-primary route behavior remains preserved. PR #36 is merged into `main`, and issue #34 is closed as completed.
