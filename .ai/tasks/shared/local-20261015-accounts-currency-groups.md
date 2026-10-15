# Accounts Per-Currency Group Subtotals (#77)

Status: In Progress
Objective: Prevent same-type mixed-currency Accounts aggregation crashes without combining currencies or calculating balances in UI.
Branch: `tnyx/fix-accounts-currency-groups`
Base: `main@d937437be9c481ad48c2d0bc8e366a2c6230d015` (CI #459 SUCCESS)
Created: `2026-10-15`
Last Updated: `2026-10-15`
Issue: `#77`
Pull Request: `#82` (https://github.com/im-tnyx/Tio-Ledger/pull/82)

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
- Owner subsequently authorized one new isolated API35 AVD, a test-only database-free display harness/APK and TalkBack enabling on that target only. Owner separately approved the bounded responsive group-header fix after a rendered overlap was established. These approvals do not authorize existing-target/physical-phone mutation, financial records, notification permission selection, issue edits or merge.

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
- Published code commit `0c56eb380d906db57e0905c03f209e259a9816a0` and opened PR #82 against main on October 15. Raw author/committer: `Santosh Jangid <im-tnyx@users.noreply.github.com>`, no AI trailer/footer. Initial CI #460 belongs exactly to that code commit and was still running at the first snapshot; review threads were zero. This publication-note update requires a new exact-head CI/review check, not reuse of the earlier snapshot. No merge/device/issue operation occurred.

## October 15 Merge-Gate Preparation (Pre-publication History)

- Owner requested progress to the merge gate, not merge. Fresh published PR snapshot: `7f758d21eb28afccd1322fdb745ccb3ecc649d2d`, base `d937437`, OPEN/CLEAN/MERGEABLE, exact-head CI #462 SUCCESS, zero review threads/reviews. GitHub mergeability is not full UI DoD acceptance.
- Added display-only mixed-currency, zero and long signed amount fixtures; eight preview functions cover light/dark, 360dp normal text and 320dp linear 2x font stress. Callbacks are no-ops; no ViewModel, repository or persisted account/ledger fixture. The preview density override is not Android platform nonlinear-font or TalkBack evidence. Dark preview validates the shared composable only, not a dark-theme app rollout or #60.
- Three fixture tests verify separate INR/USD amounts, type/row consistency, zero/sign preservation and unchanged base fixture. Initial validation failed on two overlong fixture lines; corrected. The interrupted retry was not executed because automatic approval review hit the usage limit. After the owner reported reset, the same command succeeded: `:shared:ui:testDebugUnitTest :shared:ui:testReleaseUnitTest :apps:android:compileDebugKotlin :shared:ui:ktlintCheck --no-daemon --console=plain --stacktrace`, 1m23s, 340 tasks (26 executed/314 up-to-date). XML: 78 tests in each UI variant, zero failures/errors/skips. Existing unrelated Budgets/SMS and Gradle warnings remain.
- `:shared:ui:tasks --all --no-daemon --console=plain` succeeded in 15s. Standard Android instrumentation task scaffolding exists, but no configured preview/screenshot rendering task or instrumentation test source was found. Native Android Studio UI control is unavailable in the current session; no new renderer, framework or tool was installed. No successful preview render, screenshot, contrast measurement or TalkBack observation is claimed.
- Safety finding for the next route: `TioAndroidApplication.onCreate` initializes bootstrap, creates notification channels and enqueues APP_START reminder reconciliation. Launching the production app is not a display-only validation mechanism. An owner-approved isolated harness must bypass that startup path, render `AccountsScreen` directly from display state and keep callbacks inert; no production failure hook, database edits or financial record creation.

| Merge gate | Evidence/state |
| --- | --- |
| Published source/CI/reviews | CI #462 SUCCESS at 7f758d2; zero threads, no reviewer approval |
| Local preview/header source/tests | Compile + ktlint pass; UI debug/release 78 each; not published/hosted-CI validated |
| Light/dark/width/large-font pixels | Eight before/after cases plus scrolled Credit Card light/dark inspected; changed headers show complete headings/subtotals without overlap; no whole-screen PASS |
| Accessibility | Enabled/bound service verified; native node probe is not actual TalkBack speech/traversal; Cash caption uncorroborated |
| Contrast/performance | Changed subtotal text pairs measured below; no whole-screen contrast or measured performance acceptance |
| Full merge decision | NO-GO until applicable DoD evidence and a separate owner merge authorization |

## October 15 Isolated Harness And Header Follow-Up (Local)

- Created `TioLedger_PR82_UI_API35_Isolated` from the installed API35 Google Play x86_64 image (revision 9), Pixel 7 definition, 2GB RAM/2 cores. Verified `emulator-5556` by AVD name before writes. Later normal boot resumed the stopped target without wipe/reset; serial must be rechecked. Existing `Pixel_9_API35` on 5554, API30, PR81 AVD and an attached physical phone were not modified.
- Built `:shared:ui:assembleDebugAndroidTest` with a platform-only safety probe and a test-source Activity; no new screenshot framework/dependency, app module or production startup hook. Test package `com.tioledger.shared.ui.test`, minSdk 26, testOptions targetSdk 35, versionCode 0/no versionName. Manifest uses plain `android.app.Application`, has no POST_NOTIFICATIONS permission, production receiver or WorkManager initializer. Production `com.tioledger.apps.android` is absent on this target.
- Owner enabled TalkBack `15.0.0.639625893` and dismissed the Suite notification dialog. Agent did not select Allow/Don't allow. Enabled/bound service and touch exploration verified. Font scale was recorded as 1.0, temporarily set to 2.0 on this new target for actual platform renders and restored to 1.0 in finally blocks. No time/timezone change or persisted financial/reminder preference was made.
- Initial test APK SHA-256 `ddfe3a5e67ad243d7745a1becfa3a4bcd77790cb5b9b5e31d98453a1161ecedb` matched its installed base.apk. Native startup probe returned displayOnlyStartup=true/isolatedPackage=true; database listing was empty before and after rendering. Debug signing certificate SHA-256 `8e4eaa1bc1cc53bd5d33ead3345db8d060b20e84601e4b10ea1267eb1fb7b273`.
- Actual pre-fix `signed-large-light.png`/`signed-large-dark.png` at 320dp/font_scale=2.0 showed the Cash heading breaking vertically/overlapping the long signed subtotal. Owner approved a header-only fix: active text measurement chooses inline versus stacked layout; full header amounts can wrap. Financial aggregation, account rows and top summary remain unchanged. This is a local runtime change, not part of published head 7f758d2 yet.
- Header-fix validation succeeded: `:shared:ui:assembleDebugAndroidTest :shared:ui:testDebugUnitTest :shared:ui:testReleaseUnitTest :apps:android:compileDebugKotlin :shared:ui:ktlintCheck --no-daemon --console=plain --stacktrace`, 2m03s, 432 tasks (30 executed/402 up-to-date), UI debug/release 78 each/zero failures. Existing unrelated warnings remain. Post-fix render APK SHA-256 `30de96369cf89c9291c86e2aa50dc4a3a575cfa90b1ede8a80e57b1c6d4790b7` matched the installed artifact; native DB-free startup probe passed again.
- Eight post-fix captures are local ignored files under `shared/ui/build/pr82-validation/post-*.png` (1080x2400): mixed/zero light+dark at 360dp/1.0, mixed/signed light+dark at 320dp/2.0. All inspected. Cash heading/full signed subtotals no longer overlap; normal group layout is retained. Large-font Credit Card subtotals fall below the initial viewport and require a scrolled capture before acceptance. Existing row/top-summary ellipsis, truncated bottom labels and dark summary coloring remain outside this fix; no whole-screen UI PASS. Critical post-fix signed light/dark SHA-256: `bc84435b2161e0611d323d769d19e2e3612507eafc85923c7f99ec7a37e9cb92` / `35997dbcc1879721c1efc94d50507a452c87b875abde33bf3de4128551674beb`. PNGs/APKs are not committed or repository-persisted evidence.
- Numeric subtotal contrast cross-check uses original PNG interior pixels (not antialias edges) and matching source colors. Cash/Card regions sampled at x=100..998 step 2, y=810..948 / 1320..1448 respectively. sRGB linearization: c/12.92 for c<=0.04045, otherwise ((c+0.055)/1.055)^2.4; L=0.2126R+0.7152G+0.0722B; ratio=(Lmax+0.05)/(Lmin+0.05). Light neutral `#111827/#F8FAFC` 16.95:1, liability `#B91C1C/#F8FAFC` 6.18:1; dark neutral `#F9FAFB/#0B1120` 18.02:1, liability `#FCA5A5/#0B1120` 9.92:1. These pairs exceed the conservative normal-text 4.5:1 threshold; no whole-screen/non-text/focus compliance claim. Method/threshold: https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html.
- Native inspect-focus mode uses UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES; TalkBack stays bound. Its node dump/focus action is explicitly NOT TalkBack navigation or speech evidence. Owner reported Display speech output ON and Cash focus, but captured isolated-target screenshots still showed Summary focus with no caption. Initial probe found Summary accessibility focus and no TalkBack overlay text; this mismatch must be reconciled, not silently accepted. No audible recording or proven screen-reader order yet.
- Source boundary: artifacts above were built from base HEAD 7f758d2 plus the local tracked UI patch and untracked test sources, not an exact committed PR head. Render snapshot tracked-UI patch digest was `6b723522fcf1caf67617a3227e5762881b67028209273e384287feff8abbc591` (PowerShell git diff --binary HEAD -- shared/ui, Out-String, UTF-8 SHA-256); initial three test-source file hashes are locally auditable. A later probe-only rebuild changes the test APK; record its separate hash/result. Publication and exact-head CI/source pinning remain separate gates.

## Latest isolated-target recheck

- Owner authorized commit/push of this follow-up. Publication preflight: main `d937437`, PR #82 head `7f758d2`, OPEN/CLEAN/MERGEABLE; configured author/committer Santosh Jangid <im-tnyx@users.noreply.github.com>. Combined PUSH_TEMPLATE metadata/critical-test/migration/ktlint/detekt selections plus Android compile and test-APK assembly succeeded in 22s (564 tasks: 14 executed, 550 up-to-date). Metadata selections SKIPPED; shared detekt NO-SOURCE and cached tasks are not fresh execution evidence. Previous UI debug/release results remain 78 each/zero failures. No device operation occurred during publication checks.
- Reverified `TioLedger_PR82_UI_API35_Isolated` on `emulator-5556`; installed test APK hash `e86eeeecd1673fd4237b40159f17cf4d8a55276e0b5d028cafb636da0c2c6100` matches the latest local probe build. No exact committed-source/hosted-CI claim for this local artifact.
- Owner's latest Cash-focus confirmation was not corroborated: `owner-caption-now.png` showed TalkBack Customize gestures with Back focused and no speech caption. Earlier missing captions remain unresolved; no actual speech or traversal PASS.
- Explicit `am instrument --user 0` without a whitespace-bearing header argument succeeded in both light/dark 320dp/font_scale=2.0 runs: DB-free startup=true, scrollAction=true. Earlier `Invalid userId -2` invocations were rejected, not acceptance evidence; exact parsing cause is unproven. `credit-scrolled-light.png` and `credit-scrolled-dark.png` now show the complete Credit Card heading and INR 180.00 / USD 80.00 subtotals without header clipping/overlap. Native scroll/node inspection is not TalkBack traversal; overlay text remained empty. Font scale restored to 1.0. Existing top-summary/row/bottom-nav limitations remain out of scope.

## Changed Files

- `shared/application/src/commonMain/kotlin/com/tioledger/application/usecase/account/AccountUseCases.kt`
- `shared/application/src/commonTest/kotlin/com/tioledger/application/usecase/ApplicationUseCaseTest.kt`
- `shared/ui/src/commonMain/kotlin/com/tioledger/ui/accounts/AccountsModels.kt`
- `shared/ui/src/commonMain/kotlin/com/tioledger/ui/accounts/AccountsViewModel.kt`
- `shared/ui/src/commonMain/kotlin/com/tioledger/ui/accounts/AccountsScreen.kt`
- `shared/ui/src/commonMain/kotlin/com/tioledger/ui/accounts/AccountsPreviewData.kt`
- `shared/ui/src/commonMain/kotlin/com/tioledger/ui/accounts/AccountsPreviews.kt` (local merge-gate follow-up)
- `shared/ui/src/commonTest/kotlin/com/tioledger/ui/accounts/AccountsViewModelTest.kt`
- `shared/ui/src/commonTest/kotlin/com/tioledger/ui/templates/TioTemplatePreviewDataTest.kt` (local merge-gate follow-up)
- `shared/ui/build.gradle.kts`, `shared/ui/src/androidInstrumentedTest/AndroidManifest.xml`, `shared/ui/src/androidInstrumentedTest/kotlin/com/tioledger/ui/validation/AccountsValidationActivity.kt`, `shared/ui/src/androidInstrumentedTest/kotlin/com/tioledger/ui/validation/AccountsValidationInstrumentation.kt` (authorized test-only harness)
- `docs/references/notes/account.md`, `.ai/current.md`, this task and `.ai/tasks/android/local-20260812-reminder-validation-followup.md` (handoff metadata only).

## Next Action

Owner explicitly authorized commit/push of this bounded follow-up on October 15. Publish only the reviewed header/previews/test-only harness/canonical and continuity changes to existing PR #82 after push checks; require new exact-head CI/review, not reuse of #462. Rendered changed-header review is complete for the captured cases, not the whole screen. Obtain directly visible Cash/credit-header TalkBack speech output and actual service traversal; owner confirmations without matching capture are insufficient. Restore temporary Display speech output OFF while keeping TalkBack enabled after capture. Native node/focus probes are not real speech/navigation. Final committed-source device pinning remains separate; previous local artifact hashes do not become exact-commit provenance retrospectively. No existing-target/physical-phone mutation, merge, issue edits or expansion into row/top-summary/#74 behavior is authorized.
