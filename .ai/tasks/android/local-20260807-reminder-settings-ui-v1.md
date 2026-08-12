# Android Reminder Settings UI v1

Status: Merged
Objective: Implement issue #54 production Android reminder Settings and explicit notification-permission UX from the approved reference contract.
Branch: `feat/android-reminder-settings-ui-v1`
Scope: `apps/android`, minimal `shared/ui` navigation extension
Created: `2026-08-07`
Last Updated: `2026-08-12`
Issue: `#54`
Parent: `#43`

## Required Context

- `.ai/core/ui-rules.md`
- `.ai/core/workflow-rules.md`
- `docs/references/notes/settings-reminders.md`
- `docs/emi-budget-reminders-v1.md`
- `docs/architecture.md`
- `docs/definition-of-done.md`
- `shared/ui/src/commonMain/kotlin/com/tioledger/ui/navigation/Routes.kt`
- `shared/ui/src/commonMain/kotlin/com/tioledger/ui/shell/TioAppShell.kt`
- `shared/ui/src/commonMain/kotlin/com/tioledger/ui/shell/RootNavigationHost.kt`
- `apps/android/src/main/kotlin/com/tioledger/apps/android/MainActivity.kt`
- `apps/android/src/main/kotlin/com/tioledger/apps/android/reminders/AndroidReminderSettingsService.kt`
- `apps/android/src/main/kotlin/com/tioledger/apps/android/reminders/NotificationPermissionController.kt`

## Constraints

- Reuse `MainRoute.Settings`; do not add a sixth primary bottom destination.
- Keep Android permission, lifecycle, settings Intent, platform enum, and preference service in `apps/android`.
- Opening Settings must never request runtime notification permission.
- First Android 13+ permission request must be an explicit user action and record the request attempt before launching the OS dialog.
- Denied/revoked states use non-blocking Android settings guidance and never repeatedly prompt.
- Preference state remains independent from effective delivery permission.
- Do not duplicate shared reminder eligibility, schedule, budget threshold, spend, loan, balance, interest, or money rules.
- No SQLDelight schema/migration or financial mutation.

## Decisions

- Render the Settings destination with Android-owned Compose content injected through a minimal shared navigation destination slot.
- Reuse existing Tio design components and primary bottom-navigation model.
- Use Android string resources for Settings/reminder copy.
- Use Activity Result permission APIs and an Activity resume refresh token rather than leaking Android lifecycle observers into shared UI.
- Reuse the existing Settings icon affordance on Accounts as the non-primary entry.

## Progress

- [x] Repository/docs/runtime audit completed on merged PR #53 main.
- [x] Issue #54 created from parent #43.
- [x] Focused branch created from updated `main`.
- [x] Add minimal shared navigation destination injection and Settings entry wiring.
- [x] Implement Android Settings/reminder Compose screen and localized copy.
- [x] Implement explicit permission request/settings guidance/resume refresh bridge.
- [x] Add focused permission-action and canonical-navigation tests.
- [x] Harden reminder toggle accessibility by making the full row behave as the switch target.
- [x] Harden TalkBack semantics so reminder rows expose explicit state and notification status avoids decorative icon announcements.
- [x] Add Android-owned preview support for phone-width, dark-theme, and large-text reminder Settings review.
- [x] Create follow-up issue `#57` for Settings system-back behavior and duplicate top-title removal while the branch remains open.
- [x] Implement Android-owned route history and NoActionBar theme wiring so Settings can return via system back and no duplicate shell title consumes vertical space.
- [x] Review architecture and financial safety; no shared reminder or financial behavior changed.
- [x] Exact-head Android/shared CI validation.
- [x] Phone-width light/dark, large-text, and TalkBack/accessibility review.
- [x] Final PR review and merge readiness.

## Validation

- Branch comparison confirms 0 commits behind `main` before PR creation.
- Scope review confirms no SQLDelight, ledger, transaction, balance, loan calculation, budget calculation, or shared reminder-planner changes.
- Local Git state on `2026-08-12` confirms the branch is restored from detached `HEAD` to `feat/android-reminder-settings-ui-v1` with a clean worktree.
- Local exact-head validation passed on `2026-08-12` after hardening [gradle/wrapper/gradle-wrapper.properties](/G:/projects/Tio-Ledger/gradle/wrapper/gradle-wrapper.properties) network tolerance and rerunning the repository checklist commands.
- Executed checks: shared metadata compilation, critical shared tests, SQLDelight migration verification, `ktlintCheck`, `detekt`, and `git diff --check`.
- Additional accessibility-hardening validation on `2026-08-12`: `:apps:android:test`, `ktlintCheck`, `detekt`, and `git diff --check`.
- Remote branch alignment cleanup on `2026-08-12` pruned every GitHub-merged remote branch after confirming squash-merge history through PR state; `origin` now retains only `main` and the active `feat/android-reminder-settings-ui-v1` branch.
- Preview-review support validation on `2026-08-12`: `:apps:android:compileDebugKotlin` passed after adding `compose.components.uiToolingPreview` and Android-owned reminder Settings previews for light, dark, and large-text states.
- Follow-up bug tracking on `2026-08-12`: GitHub issue `#57` created for Android Settings system-back behavior and duplicate top-title removal.
- Follow-up implementation validation on `2026-08-12`: `:apps:android:assembleDebug` passed after moving route ownership into the Android activity boundary for system-back handling and applying an Android `NoActionBar` activity theme.
- Device verification on `2026-08-12` confirmed `Settings -> system back -> Accounts` works and the duplicate top `Tio Ledger` title is gone.
- Follow-up validation on `2026-08-12`: `:apps:android:test` passed after the latest Android route/theme changes, so the earlier branch-local unit-test drift is no longer reproducing.
- Non-blocking environment notes remain: Android metrics initialization could not write `C:\Users\SANTOSH\.android\analytics.settings`, and Gradle reported the deprecated property `kotlin.mpp.androidGradlePluginCompatibility.nowarn`.
- Non-blocking local build noise also showed Kotlin daemon access failures under `C:\Users\SANTOSH\AppData\Local\kotlin\daemon\`, but Gradle completed using fallback compilation and the successful task results above are the authoritative outcome.
- Device/visual accessibility review completed on `2026-08-12`; `Settings -> system back -> Accounts`, duplicate-title removal, phone-width light/dark, large-text, and TalkBack expectations were verified.
- PR `#55` merged to `main` on `2026-08-12` as squash commit `d988a7a2223c6129f8970be929ff21822a3e9d56`.

## Changed Areas

- `apps/android`: Android-owned Settings UI, permission launcher/settings Intent bridge, localized copy, focused tests, direct Compose dependencies.
- `apps/android/src/main/AndroidManifest.xml`: apply an Android theme that removes the duplicate platform action bar title.
- `apps/android/src/main/res/values/themes.xml`: define the Android `NoActionBar` activity theme.
- `apps/android/build.gradle.kts`: Android preview dependency for reminder Settings review support.
- `apps/android/src/main/res/values/strings.xml`: explicit switch-state accessibility copy.
- `shared/ui`: minimal Settings destination-content injection, existing Accounts non-primary Settings entry, canonical bottom-navigation regression coverage, and optional external route ownership for Android system-back support.
- `docs/module-design.md`: clarifies existing Android ownership for platform Settings/permission UI.
- `.ai`: archives merged #51 task and points continuity at #54.

## Next Action

Track the remaining umbrella acceptance work under issue `#56` / parent `#43`; this implementation slice itself is complete and merged.
