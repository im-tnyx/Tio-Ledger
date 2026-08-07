# Settings Reminder And Permission UI v1

Status: Ready for Review
Objective: Define and approve the reference-backed Android Settings and notification-permission UX contract required by issue #51 before production Compose implementation.
Branch: `docs/settings-reminder-permission-ui-v1`
Scope: `docs/references`, Android reminder settings UX contract
Created: `2026-08-07`
Last Updated: `2026-08-07`

## Required Context

- `.ai/core/ui-rules.md`
- `.ai/core/workflow-rules.md`
- `docs/references/README.md`
- `docs/emi-budget-reminders-v1.md`
- `shared/ui/src/commonMain/kotlin/com/tioledger/ui/navigation/Routes.kt`
- `shared/ui/src/commonMain/kotlin/com/tioledger/ui/shell/RootNavigationHost.kt`
- `apps/android/src/main/kotlin/com/tioledger/apps/android/reminders/AndroidReminderSettingsService.kt`
- `apps/android/src/main/kotlin/com/tioledger/apps/android/reminders/NotificationPermissionController.kt`

## Constraints

- Issue #51 is documentation/specification only; no production Compose implementation belongs in this branch.
- Preserve the existing typed `MainRoute.Settings` destination and canonical five bottom-navigation destinations.
- Do not invent a sixth primary bottom-navigation item or a new Settings information architecture.
- The first Android 13+ runtime notification permission request must follow explicit user action.
- Permission denial or revocation must never block financial workflows or trigger repeated permission prompts.
- Only the two approved global controls are in scope: EMI reminders and budget reminders.
- Android platform code consumes shared reminder plans; this task does not change reminder eligibility, financial rules, ledger data, SQLDelight schema, or financial history.

## Decisions

- Use the official Realbyte Android Help Center article `How to set up a reminding alarm` as the approved fallback reference because no checked-in Settings screenshot or Settings reference folder exists.
- Use official Realbyte Settings/Configuration material only to confirm hierarchical Settings behavior, not to copy layouts, strings, assets, dimensions, or proprietary implementation details.
- Keep Settings outside primary bottom navigation. `MainRoute.Settings` remains the typed destination.
- Treat notification permission state and reminder preference state as non-financial platform metadata.
- Consume the merged Android foundation from PR #52; no UI-owned scheduling or reminder-rule duplication is allowed.

## Progress

- [x] Repository and canonical UI reference policy inspected.
- [x] Existing Settings route and placeholder behavior inspected.
- [x] PR #52 settings/permission service contract inspected and merged to `main`.
- [x] Approved external fallback reference identified.
- [x] Add the complete Settings/reminder reference note and UI contract.
- [x] Review issue #51 acceptance criteria against the finished document.
- [ ] Open a focused documentation PR for review.

## Validation

- Repository/runtime inspection completed through the GitHub connector.
- PR #52 exact-head CI run #390 passed before merge.
- Branch diff is documentation/AI continuity only; no production Kotlin, Gradle, SQLDelight, financial, or UI implementation file changes are included.
- Gradle validation has not been run locally through the connector and is not claimed.
- Local `git diff --check` is not available through the connector and is not claimed.

## Changed Files

- `.ai/archive/2026/android-reminder-delivery-foundation-v1.md`
- `.ai/tasks/android/local-20260806-android-reminder-delivery-foundation-v1.md` removed after merge synchronization.
- `.ai/tasks/docs/local-20260807-settings-reminder-permission-ui-v1.md`
- `.ai/current.md`
- `docs/references/notes/settings-reminders.md`

## Next Action

Open a focused documentation PR for issue #51 and use CI/review to validate the exact branch head before merge.
