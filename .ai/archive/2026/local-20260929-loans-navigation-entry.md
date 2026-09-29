# Loans Navigation Entry Reference

Status: Complete
Objective: Define an approved, reference-backed navigation entry to `MainRoute.Loans` for issue #61 before any production navigation change.
Branch: `docs/loans-navigation-entry`
Scope: `docs/references/notes/loan.md` and `.ai` continuity only; no production Kotlin
Created: `2026-09-29`
Completed: `2026-09-29`
Pull Request: `https://github.com/im-tnyx/Tio-Ledger/pull/69`
Merge Commit: `ba5fac5c52a0330c6e1c75fdb567eb76b17e5f37`
Issue: `#61`
Related: `#56`, `#43` (EMI device checks blocked by #61)

## Required Context

- `.ai/core/ui-rules.md`
- `.ai/core/workflow-rules.md`
- `docs/references/README.md`
- `docs/references/notes/loan.md`
- `docs/references/notes/settings-reminders.md` (Navigation Definition)
- `docs/references/notes/account.md`, `docs/references/accounts/`
- `shared/ui/src/commonMain/kotlin/com/tioledger/ui/navigation/Routes.kt`
- `shared/ui/src/commonMain/kotlin/com/tioledger/ui/accounts/AccountsScreen.kt`
- `apps/android/src/main/kotlin/com/tioledger/apps/android/MainActivity.kt` (`systemBackTargetOrNull`)

## Constraints

- Keep the five primary bottom destinations; no sixth item.
- No Dashboard build, no new overflow system, no financial/database/loan-engine change.
- No production navigation code until the reference decision is merged.

## Findings

- `MainRoute.Loans` is registered in the main graph and `RootNavigationHost`, but no screen emits it; only `LoanDetails` is reachable via reminder deep links.
- `MainRoute.Accounts` is the main-graph start route. Its app bar already hosts the approved non-primary Settings action.
- Android system back from Loans currently returns `null` (exits), covered by `MainActivityNavigationTest.systemBackExitsFromTopLevelNonPrimaryRoutes`.
- Reference: Money Manager keeps loans as an Accounts group with no separate Loans destination; there is no Loans entry in its tabs or More/settings; the JADX structure has no loan screen.

## Decisions (approved by merging PR #69)

- Entry: `Loans` icon action (`TioIconToken.Loan`) in the Accounts top app bar, before Settings.
- System back from Loans → Accounts (main entry), matching Settings; Loan Details → Loans unchanged.

## Progress

- [x] Repository and reference audit.
- [x] `loan.md` Loans Entry Point amendment.
- [x] PR #69 CI run #426 green; squash-merged as `ba5fac5` with a clean message.
- [x] Implementation handed off to `.ai/tasks/android/local-20260929-loans-navigation-implementation.md`.

## Validation

- `git diff --check`.

## Changed Files

- `docs/references/notes/loan.md`
- `.ai/current.md`, this task, `.ai/archive/2026/local-20260929-prevent-ai-coauthor-attribution.md`

## Next Action

None.
