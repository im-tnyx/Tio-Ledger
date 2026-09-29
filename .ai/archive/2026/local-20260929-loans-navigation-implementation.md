# Loans Navigation Entry Implementation

Status: Complete
Objective: Implement the approved #61 Loans entry (PR #69): an Accounts app-bar `Loans` action to `MainRoute.Loans` and Android system back from Loans to Accounts.
Branch: `fix/loans-navigation-entry`
Scope: `shared/ui` Accounts app bar + `apps/android` system-back rule and tests; no financial, route, or bottom-navigation change
Created: `2026-09-29`
Completed: `2026-09-29`
Pull Request: `https://github.com/im-tnyx/Tio-Ledger/pull/70`
Merge Commit: `3a986a0c94759b76d055dc7846e56a0d499366fc`
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

- [x] Implementation (commit `6719220`).
- [x] Tests.
- [x] Local validation.
- [x] Device validation.
- [x] PR #70 CI run #428 green; squash-merged as `3a986a0` with a clean message; #61 closed.

## Validation

- Local on `6719220`: shared metadata compile, critical shared tests, `:apps:android:compileDebugKotlin`/`testDebugUnitTest`/`assembleDebug`, migration verification, `ktlintCheck`, `detekt`, `git diff --check` — all pass (`MainActivityNavigationTest` 7/7, shared navigation tests pass).
- Device `TioLedger_Android11` (API 30), APK from `6719220`, existing data with 0 loans (no injection), 2026-09-29:
  - Accounts app bar shows `A` (statistics), `Loans`, `Settings` in that order; Loans clickable node `[805,88][937,220]` (132 px ≈ 48 dp) with content-desc `Loans`; a `Button`-role semantics child.
  - Tap → Loans screen (`No loans` empty state, `Add loan`), five bottom destinations unchanged, 0 selected (Accounts screen: Accounts selected).
  - System back → Accounts, app stays foreground; repeated twice. No crash.
  - DB read-only before/after identical: 0 loans, 0 EMI schedules, 1 account, 3 transactions, 3 splits, 6 ledger entries, DEBIT = CREDIT = 10500.
  - Accessibility note: Compose 1.7 exposes the label/role as same-bounds children of the clickable node, the same structure as the approved Settings action and Material bottom-navigation items. TalkBack is not installed on this `google_apis` image, so spoken output was not verified.
  - Loan Details back is covered by `systemBackFromLoanDetailsReturnsLoans`; not exercised on device (no loan exists; none injected).

## Changed Files

- `shared/ui/src/commonMain/kotlin/com/tioledger/ui/accounts/AccountsScreen.kt`
- `apps/android/src/main/kotlin/com/tioledger/apps/android/MainActivity.kt`
- `apps/android/src/test/kotlin/com/tioledger/apps/android/MainActivityNavigationTest.kt`

## Next Action

None. EMI validation continues in `.ai/tasks/android/local-20260812-reminder-validation-followup.md`.
