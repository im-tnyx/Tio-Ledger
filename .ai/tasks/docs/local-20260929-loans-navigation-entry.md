# Loans Navigation Entry Reference

Status: In Progress
Objective: Define an approved, reference-backed navigation entry to `MainRoute.Loans` for issue #61 before any production navigation change.
Branch: `docs/loans-navigation-entry`
Scope: `docs/references/notes/loan.md` and `.ai` continuity only; no production Kotlin
Created: `2026-09-29`
Last Updated: `2026-09-29`
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

## Decisions (proposed in the docs PR)

- Entry: `Loans` icon action (`TioIconToken.Loan`) in the Accounts top app bar, before Settings.
- System back from Loans → Accounts (main entry), matching Settings; Loan Details → Loans unchanged.

## Progress

- [x] Repository and reference audit.
- [x] `loan.md` Loans Entry Point amendment.
- [ ] Docs PR merged (needs explicit authorization).
- [ ] Separate production implementation task after merge.

## Validation

- `git diff --check`.

## Changed Files

- `docs/references/notes/loan.md`
- `.ai/current.md`, this task, `.ai/archive/2026/local-20260929-prevent-ai-coauthor-attribution.md`

## Next Action

Get the #61 reference PR reviewed and merge-authorized; then implement the Accounts app-bar Loans action, the system-back change, and tests in a separate PR.
