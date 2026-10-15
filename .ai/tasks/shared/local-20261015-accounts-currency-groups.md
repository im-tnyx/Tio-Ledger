# Accounts Per-Currency Group Subtotals (#77)

Status: Ready for Review
Objective: Prevent same-type mixed-currency Accounts aggregation crashes without combining currencies or calculating balances in UI.
Branch: `tnyx/fix-accounts-currency-groups`
Base: `main@d937437be9c481ad48c2d0bc8e366a2c6230d015` (CI #459 SUCCESS)
Created: `2026-10-15`
Last Updated: `2026-10-15`
Issue: `#77`

## Required Context

- `.ai/core/ui-rules.md`
- `.ai/core/workflow-rules.md`
- `docs/engineering-guidelines.md`
- `docs/definition-of-done.md`
- `docs/references/notes/account.md`
- `shared/application/src/commonMain/kotlin/com/tioledger/application/usecase/account/AccountUseCases.kt`
- `shared/ui/src/commonMain/kotlin/com/tioledger/ui/accounts/`

## Approved Contract And Boundaries

- Owner approved one AccountType heading with separate currency-labeled subtotals; no FX conversion or selected-currency fallback. Preserve account rows and stable type-header keys.
- Application owns immutable per-type/per-currency aggregation. UI only formats/renders supplied totals. Search subtotals describe visible matching accounts; global currency totals retain their existing unfiltered behavior.
- Preserve Money currency checks, ledger-derived account balances, integer minor units, immutable history and frozen schema. No #74 credit-card payable/outstanding specialization.
- The implementation slice allowed no device actions, real financial fixtures/records, database writes, permission/preferences/clock changes, issue edits or publication. The owner separately authorized commit/push/new PR on October 15; merge and all device/data/issue changes remain unauthorized. Deterministic test fixtures exist only in memory.
- #56/#43 remain open in the separate reminder follow-up task. No accessibility acceptance advance is implied by #77.

## Progress And Validation

- Fresh main and clean worktree verified; branch created from synchronized main after PR #81 merge.
- RED confirmed before production edits: `:shared:ui:testDebugUnitTest --tests com.tioledger.ui.accounts.AccountsViewModelTest.sameTypeMixedCurrencyAccountsLoadWithoutCrashing --no-daemon --console=plain` failed (1 test/1 failure, 56s). Exact error: `IllegalArgumentException: Currency mismatch: USD vs INR`, Money.plus -> AccountsViewModel.totalBalanceFor -> toUiState. Both same-type balances were zero; no device/financial fixture.
- Owner explicitly confirmed the seven-code/test-file cross-layer scope plus canonical/continuity docs. Application-owned groups now reuse currency totals; ViewModel monetary folding was removed. Header renders a list of currency-prefixed amounts under one stable type key. No shared core/domain/finance/data/database/Android source changes.
- GREEN: `:shared:application:testDebugUnitTest :shared:ui:testDebugUnitTest --no-daemon --console=plain` succeeded in 33s (153 tasks, 19 executed). XML confirms 59 Application and 75 UI tests, zero failures/errors/skips. The original regression is a passing testcase. A preceding test compile error used nonexistent CurrencyCode.value; corrected to normalized before this successful run.
- Broad validation initially stopped at formatting violations in the new Application tests. Scoped `:shared:application:ktlintFormat :shared:ui:ktlintFormat` succeeded (22s); diff inspection confirmed no unrelated file changes.
- Final combined command succeeded in 53s (520 actionable tasks: 32 executed, 488 up-to-date): Android compile/JVM tests; core/domain/finance-engine/loan-engine/budget-engine/analytics/notifications/application/data/bootstrap/UI `:test` selections; ten metadata selections; migration verification; root `ktlintCheck detekt`. Application/UI debug and release XML each confirm 59/75 tests with zero failures/errors/skips. Cached tasks are not fresh execution.
- Coverage limits: all ten `compileKotlinMetadata` tasks were SKIPPED; explicit migration rerun also SKIPPED (command succeeded in 15s). Changed shared modules' detekt tasks were NO-SOURCE, while Android static checks were cached. No fresh commonMain detekt, native/iOS, rendered UI, theme/large-font or real TalkBack acceptance is claimed. Existing Gradle deprecation and unrelated Budgets/SMS warnings remain.
- Regression matrix covers same-type INR/USD zero and nonzero balances, same-currency summing, negative asset/liability balances, distinct type versus global totals, case-insensitive/no-match/blank/cleared search, deterministic currency/type order, archived-account eligibility, strict Money mismatch rejection, same-type currency selection on creation and in-memory read-path no-mutation snapshots.
- Canonical `account.md` records the approved contract and the separate top-summary first-currency limitation. Reminder task now records PR #81 merge/main CI and hands off without closing/archiving #56/#43. `git diff --check` PASS; no commit/push/PR/device/issue operation occurred.
- Publication preflight on October 15: remote main still `d937437`, no existing PR/remote branch for this change, human Git identity verified. Repository-required selections plus Android compile/tests succeeded in 21s (480 tasks: 13 executed, 467 up-to-date). Test tasks and ktlint were UP-TO-DATE; metadata/migration SKIPPED and shared detekt NO-SOURCE. These are command results, not fresh test executions or device acceptance. Owner authorized repository-native publication after the optional ship checklist proved unavailable.

## Changed Files

- `shared/application/src/commonMain/kotlin/com/tioledger/application/usecase/account/AccountUseCases.kt`
- `shared/application/src/commonTest/kotlin/com/tioledger/application/usecase/ApplicationUseCaseTest.kt`
- `shared/ui/src/commonMain/kotlin/com/tioledger/ui/accounts/AccountsModels.kt`
- `shared/ui/src/commonMain/kotlin/com/tioledger/ui/accounts/AccountsViewModel.kt`
- `shared/ui/src/commonMain/kotlin/com/tioledger/ui/accounts/AccountsScreen.kt`
- `shared/ui/src/commonMain/kotlin/com/tioledger/ui/accounts/AccountsPreviewData.kt`
- `shared/ui/src/commonTest/kotlin/com/tioledger/ui/accounts/AccountsViewModelTest.kt`
- `docs/references/notes/account.md`, `.ai/current.md`, this task and `.ai/tasks/android/local-20260812-reminder-validation-followup.md` (handoff metadata only).

## Next Action

Publish the reviewed eleven-file patch (seven code/test files plus four documentation/continuity files) on the existing branch using the owner's October 15 commit/push/new PR authorization. Follow repository push/PR templates directly; the optional ship skill was stopped because its review checklist is absent, and the owner authorized the repository-native workflow instead. Inspect exact-head CI/review after publication; no merge is authorized. Rendered phone-width/large-font, theme and accessibility review remain pending on a separately approved safe path; do not manufacture device financial fixtures or silently broaden this fix into the top-summary/#74 workflows.
