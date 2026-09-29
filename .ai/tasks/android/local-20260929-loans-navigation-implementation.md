# Loans Navigation Entry Implementation

Status: In Progress
Objective: Implement the approved #61 Loans entry (PR #69): an Accounts app-bar `Loans` action to `MainRoute.Loans` and Android system back from Loans to Accounts.
Branch: `fix/loans-navigation-entry`
Scope: `shared/ui` Accounts app bar + `apps/android` system-back rule and tests; no financial, route, or bottom-navigation change
Created: `2026-09-29`
Last Updated: `2026-09-29`
Issue: `#61`
Related: `#56`, `#43`

## Required Context

- `.ai/core/ui-rules.md`
- `.ai/core/workflow-rules.md`
- `docs/references/notes/loan.md` (Loans Entry Point)
- `shared/ui/src/commonMain/kotlin/com/tioledger/ui/accounts/AccountsScreen.kt`
- `apps/android/src/main/kotlin/com/tioledger/apps/android/MainActivity.kt`

## Constraints

- Five primary bottom destinations unchanged; no Dashboard, overflow system, or Accounts redesign.
- Do not change `Routes.kt`, `RootNavigationHost.kt`, `TioAppShell.kt`, `LoansScreen.kt`, loan/reminder/financial code, or SQLDelight.
- Leave the existing `Account statistics` and Settings actions unchanged.
- No DB injection for validation; do not merge without explicit authorization.

## Decisions

- Loans action: `Box` with `TioDimensions.minTouchTarget`, `clickable(role = Role.Button)`, content description `Loans`, `TioIconToken.Loan`, placed before Settings.
- System back: `MainRoute.Loans` → `TioNavigationGraphs.root.mainEntry` (Accounts).
- `shared:ui` has no Compose UI test infrastructure; the click wiring is verified on device rather than by adding a framework or refactoring for testability.

## Progress

- [ ] Implementation.
- [ ] Tests.
- [ ] Local validation.
- [ ] Device validation.
- [ ] PR with exact-head CI.

## Validation

- Not run yet.

## Changed Files

- None yet.

## Next Action

Implement the Accounts Loans action and system-back rule.
