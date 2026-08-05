# Home Dashboard Clean Header

Status: Ready for Review
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
- [x] Run required validation and review the final diff.
- [x] Open focused draft PR #32.

## Validation

CI run #324 passed on implementation head `b465a5414bf70e09dee7ff4ff08626eec91be6a6`:

- Shared metadata compilation: passed.
- Critical tests: passed.
- SQLDelight migration verification: passed.
- `ktlintCheck`: passed.
- `detekt`: passed.
- Branch was 0 commits behind `main` during final implementation diff review.
- Production behavior diff is one Compose deletion: removal of the Dashboard `topBar`.

## Changed Files

- `.ai/current.md`
- `.ai/tasks/shared/local-20260805-home-dashboard-clean-header.md`
- `docs/references/notes/dashboard.md`
- `shared/ui/src/commonMain/kotlin/com/tioledger/ui/templates/TioTemplates.kt`

## Next Action

Verify exact-head CI after this state-only documentation update, then mark PR #32 ready for review and merge.
