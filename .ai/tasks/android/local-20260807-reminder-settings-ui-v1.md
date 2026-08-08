# Android Reminder Settings UI v1

Status: In Progress
Objective: Implement issue #54 production Android reminder Settings and explicit notification-permission UX from the approved reference contract.
Branch: `feat/android-reminder-settings-ui-v1`
Scope: `apps/android`, minimal `shared/ui` navigation extension
Created: `2026-08-07`
Last Updated: `2026-08-07`
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
- [x] Review architecture and financial safety; no shared reminder or financial behavior changed.
- [ ] Exact-head Android/shared CI validation.
- [ ] Phone-width light/dark, large-text, and TalkBack/accessibility review.
- [ ] Final PR review and merge readiness.

## Validation

- Branch comparison confirms 0 commits behind `main` before PR creation.
- Scope review confirms no SQLDelight, ledger, transaction, balance, loan calculation, budget calculation, or shared reminder-planner changes.
- Local Gradle and `git diff --check` are not available through the GitHub connector and are not claimed.
- Exact-head CI and device/visual accessibility review are pending.

## Changed Areas

- `apps/android`: Android-owned Settings UI, permission launcher/settings Intent bridge, localized copy, focused tests, direct Compose dependencies.
- `shared/ui`: minimal Settings destination-content injection, existing Accounts non-primary Settings entry, canonical bottom-navigation regression coverage.
- `docs/module-design.md`: clarifies existing Android ownership for platform Settings/permission UI.
- `.ai`: archives merged #51 task and points continuity at #54.

## Next Action

Open a draft PR for issue #54, use exact-head CI for compiler/test/lint validation, and keep the PR draft until required device visual/accessibility checks are complete.
