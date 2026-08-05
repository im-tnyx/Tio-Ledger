# Navigation Validation Report

Date: 2026-08-05
Outcome: PASS
Milestone: Primary Bottom Navigation Wiring

## Scope

Validated production primary bottom-navigation wiring in `shared:ui`.
The change preserves the existing typed route hierarchy, canonical destination
set, screen content, visual structure, and shared `TioBottomNavigation`
component. No financial, persistence, schema, analytics, loan, notification, or
automation behavior changed.

## Canonical Primary Destinations

`TioNavigationGraphs.main.bottomNavigationRoutes` remains the single route
source and defines this order:

| Order | Destination | Path |
| --- | --- | --- |
| 1 | Dashboard | `dashboard` |
| 2 | Accounts | `accounts` |
| 3 | Transactions | `transactions` |
| 4 | Categories | `categories` |
| 5 | Budgets | `budgets` |

Reports, Loans, Settings, detail routes, transaction entry, and SMS review
remain non-primary destinations.

## Production Wiring

- `MainBottomNavigationModel` derives labels, icons, selected state, and typed
  route mapping from the canonical route list.
- `AccountsRoute` and `AccountsScreen` now forward bottom-navigation selection
  through a typed `(MainRoute) -> Unit` callback.
- Accounts no longer owns a hard-coded or unrelated navigation item list.
- `RootNavigationHost` converts Accounts and placeholder selections to
  `RootRoute.Main(target)`.
- Dashboard remains the existing placeholder destination; only its ignored
  navigation callback was repaired. No Dashboard content was invented or
  redesigned.
- Existing Transactions, Categories, Budgets, Reports, Loans, detail, add,
  SMS-review, and back-navigation flows remain unchanged.
- Non-primary screens produce no selected primary item while retaining the
  ability to navigate to a primary destination.
- A navigation item outside the canonical model is ignored instead of being
  mapped to an incorrect route.

## Validation

| Check | Result |
| --- | --- |
| Canonical five-route order retained | PASS |
| Labels and icons derived from typed routes | PASS |
| Exactly one item selected on each primary destination | PASS |
| No primary item selected for Reports, Loans, or Settings | PASS |
| Each canonical item maps to its corresponding typed `MainRoute` | PASS |
| Unknown item is ignored | PASS |
| Accounts bottom-navigation callback is wired | PASS |
| Dashboard placeholder callback is wired | PASS |
| Existing shared UI tests remain green | PASS |
| Shared metadata compilation | PASS |
| Critical tests | PASS |
| SQLDelight migration verification | PASS |
| `ktlintCheck` | PASS |
| `detekt` | PASS |

## Evidence

- Issue: #34.
- Pull request: #36.
- Validated implementation head: `5f059f97628674de399857ddfdf616b325832de0`.
- GitHub Actions CI run #345 passed all required jobs.
- Focused coverage:
  `shared/ui/src/commonTest/kotlin/com/tioledger/ui/navigation/MainBottomNavigationModelTest.kt`.
- The implementation-introduced constructor warning was removed. Compiler logs
  retain unrelated pre-existing warnings outside this scope.

Manual device interaction, pixel comparison, or responsive-layout review is not
claimed because this change does not alter visual structure or component styling.
The existing bottom-navigation component and accessibility behavior are reused.

## Decision

Primary bottom navigation is consistently wired for Accounts and the Dashboard
placeholder, while existing working destinations and non-primary route behavior
remain preserved. The change is ready for final exact-head review and merge.
