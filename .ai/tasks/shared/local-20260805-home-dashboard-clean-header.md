# Home Dashboard Clean Header

Status: In Progress
Objective: Remove the branded app-name top bar from the Home/Dashboard template so the screen remains visually clean.
Branch: `fix/home-dashboard-clean-header`
Scope: `shared/ui`, Dashboard reference documentation
Created: `2026-08-05`
Last Updated: `2026-08-05`

## Required Context

- `.ai/core/ui-rules.md`
- `.ai/core/workflow-rules.md`
- `docs/references/notes/dashboard.md`
- `shared/ui/src/commonMain/kotlin/com/tioledger/ui/templates/TioTemplates.kt`

## Constraints

- Keep this change isolated from Loan payoff analytics PR #30.
- Do not redesign Dashboard content, navigation, bottom navigation, or the floating action button.
- Do not change app bars on Accounts, Transactions, Reports, Loans, or other screens.
- No financial, persistence, schema, architecture, or automation changes.

## Decisions

- User approved removing the branded `Tio Ledger` header from Home/Dashboard on 2026-08-05.
- Preserve the existing Scaffold, bottom navigation, FAB, content padding, and summary hierarchy.

## Progress

- [x] Inspect repository state and current open PRs.
- [x] Locate the hard-coded Dashboard app bar.
- [x] Create issue #31 and isolated branch.
- [x] Remove the Dashboard top app bar.
- [x] Update the Dashboard reference note.
- [ ] Run required validation and review the final diff.
- [ ] Open a focused pull request.

## Validation

- Branch is 0 commits behind `main`.
- Focused diff contains one Compose deletion plus AI task and Dashboard reference-note updates.
- Automated CI not run yet.

## Changed Files

- `.ai/current.md`
- `.ai/tasks/shared/local-20260805-home-dashboard-clean-header.md`
- `docs/references/notes/dashboard.md`
- `shared/ui/src/commonMain/kotlin/com/tioledger/ui/templates/TioTemplates.kt`

## Next Action

Open a focused draft pull request and verify exact-head CI.
