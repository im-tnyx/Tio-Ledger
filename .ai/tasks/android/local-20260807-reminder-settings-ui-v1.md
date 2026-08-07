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
- Use Activity Result permission APIs and Activity resume callbacks in `MainActivity` rather than leaking Android lifecycle APIs into shared UI.
- Reuse the existing Settings icon affordance on Accounts as the non-primary entry.

## Progress

- [x] Repository/docs/runtime audit completed on merged PR #53 main.
- [x] Issue #54 created from parent #43.
- [x] Focused branch created from updated `main`.
- [ ] Add minimal shared navigation destination injection and Settings entry wiring.
- [ ] Implement Android Settings/reminder Compose screen and localized copy.
- [ ] Implement explicit permission request/settings guidance/resume refresh bridge.
- [ ] Add focused tests.
- [ ] Review reference, accessibility, architecture, and financial safety.
- [ ] Run exact-head CI and prepare focused PR.

## Validation

Not run yet.

## Changed Files

- `.ai/archive/2026/settings-reminder-permission-ui-v1.md`
- `.ai/tasks/docs/local-20260807-settings-reminder-permission-ui-v1.md` removed
- `.ai/tasks/android/local-20260807-reminder-settings-ui-v1.md`

## Next Action

Implement the shared navigation slot and Android-owned Settings/reminder screen without changing reminder planning or financial behavior.
