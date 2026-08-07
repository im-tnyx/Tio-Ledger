# Settings And Reminder Permission Reference Note

## Screen

Android Settings / Reminders v1

## Issue Scope

- Parent: issue #43, Android EMI and budget reminder delivery.
- Specification task: issue #51.
- Android platform foundation: merged through PR #52 / issue #50.
- This document approves the Settings/reminder UX contract only. Production Compose implementation belongs in a later focused change.

## Reference Readiness

- Checked-in Settings screenshot: unavailable.
- Checked-in `docs/references/settings/` source: unavailable.
- Approved fallback source: official Realbyte Android Help Center article **How to set up a reminding alarm**: `https://help.realbyteapps.com/hc/en-us/articles/360060613673-How-to-set-up-a-reminding-alarm`.
- Supporting official source: Realbyte Help Center Settings/Configuration material, including **How to set style**: `https://help.realbyteapps.com/hc/en-us/articles/7318083094169-How-to-set-style`.
- Repository navigation source: existing `MainRoute.Settings` typed destination in `shared/ui/src/commonMain/kotlin/com/tioledger/ui/navigation/Routes.kt`.
- Android platform contract source: `AndroidReminderSettingsService` and `AndroidNotificationPermissionStatus` under `apps/android/src/main/kotlin/com/tioledger/apps/android/reminders/`.

The official Android reminder article confirms the familiar product hierarchy of entering reminder controls from the app's non-primary settings area and handling Android notification enablement separately from reminder preferences. Tio Ledger preserves that workflow relationship without copying Realbyte layouts, strings, assets, dimensions, colors, icons, or implementation details.

The user's `go next` instruction on 2026-08-07 authorizes this official Realbyte Help Center material as the fallback source for issue #51, consistent with the repository's reference fallback policy and prior screen-note precedent.

## Reference Boundary

Allowed findings from the fallback source:

- Reminder controls belong in a non-primary settings area rather than a financial workflow screen.
- App-level notification permission/enablement and in-app reminder preferences are separate concerns.
- Users may need guidance to Android notification settings when delivery is disabled.

Not imported from the fallback source:

- Proprietary visual layout.
- Exact copy or labels beyond generic platform terminology.
- Assets, icons, colors, dimensions, spacing, or animation.
- Realbyte-specific alarm business rules.
- Daily transaction reminder behavior.

## Workflow Summary

1. User opens the existing typed `MainRoute.Settings` destination from a non-primary Settings/overflow entry in the app shell.
2. Settings shows a Reminders section with exactly two global controls:
   - EMI reminders.
   - Budget reminders.
3. Each switch writes only the corresponding Android platform preference through `AndroidReminderSettingsService`.
4. Preference changes trigger the merged Android reconciliation path; UI does not calculate reminder eligibility, due dates, thresholds, schedules, or financial values.
5. Notification permission is presented as a separate delivery-status row.
6. On Android versions where runtime notification permission is not required, the row is informational and no permission action is shown.
7. On Android 13+ when permission has never been requested, the user may explicitly choose an `Allow notifications` action.
8. The OS runtime permission prompt is launched only from that explicit action. Merely opening Settings never prompts.
9. If permission is denied, revoked, or app notifications are disabled, financial workflows and reminder preferences remain usable. The screen shows non-blocking guidance to Android app notification settings instead of repeatedly prompting.
10. When permission state changes, Android reconciliation is requested through the platform service.
11. User leaves Settings through existing main navigation; the canonical five bottom-navigation destinations remain unchanged.

## Information Hierarchy

1. `Settings` app bar/title.
2. `Reminders` section heading with a short explanation that reminders are optional and do not change financial records.
3. EMI reminders row with switch.
4. Budget reminders row with switch.
5. Notification delivery section/row showing effective Android permission state.
6. Contextual action only when needed:
   - explicit runtime permission action for `NOT_REQUESTED` on Android 13+;
   - Android app-notification-settings guidance for `DENIED` or `REVOKED`.
7. Inline non-blocking error text if a preference update cannot be stored.

## Functional Specification

### Reminder Preferences

- The screen exposes only `EMI reminders` and `Budget reminders` global switches.
- Initial switch values come from `AndroidReminderSettingsService.snapshot().preferences`.
- Changing the EMI switch calls `setEmiRemindersEnabled(enabled)`.
- Changing the budget switch calls `setBudgetRemindersEnabled(enabled)`.
- A successful write relies on the platform service to enqueue `PREFERENCES_CHANGED` reconciliation.
- A failed write must not present a false persisted state. Reload the latest snapshot and show inline non-blocking feedback.
- Enabling a reminder preference does not itself create, edit, post, reverse, adjust, or mutate any financial record.
- Disabling a preference must result in platform reconciliation/cancellation through the existing Android foundation rather than UI-owned scheduling logic.

### Permission Request

- Opening the screen never launches an Android permission prompt.
- On `NOT_REQUESTED`, show an explicit `Allow notifications` action.
- When the user selects that action:
  1. Record the attempt through `markPermissionRequestAttempted()` before invoking the Android runtime request.
  2. Request only the runtime permission returned by the platform service/controller when non-null.
  3. After the platform result, call `onPermissionStateChanged()` and refresh the settings snapshot.
- The UI must not request notification permission again automatically after a denial.
- `DENIED` and `REVOKED` use settings guidance rather than repeated OS prompts.
- Permission denial never disables transaction entry, ledger access, budgets, loans, analytics, or any other financial workflow.

### Permission And Delivery-State Matrix

| Platform status | User-facing meaning | Primary UI behavior | Allowed action |
| --- | --- | --- | --- |
| `NOT_REQUIRED` | Runtime notification permission is not required on this Android version and notifications are effectively available | Informational delivery status | None |
| `NOT_REQUESTED` | Android 13+ runtime permission has not been requested yet | Explain that reminders need notification permission | Explicit `Allow notifications` action |
| `GRANTED` | Notifications are currently available to Tio Ledger | Show enabled/available status | None |
| `DENIED` | User denied the first runtime request and Tio Ledger must not repeatedly prompt | Show non-blocking denied state | Open Android app notification/settings guidance |
| `REVOKED` | Previously available notification delivery is now disabled or revoked | Show non-blocking disabled state | Open Android app notification/settings guidance |

`Settings guidance` is a UI presentation derived from `DENIED` or `REVOKED`; it is not a new platform enum or shared business state.

### Preference And Permission Independence

- Reminder switches represent user preference, not effective Android delivery capability.
- A preference may remain enabled while permission is denied/revoked. This preserves user intent so delivery can resume after permission is restored and reconciliation runs.
- UI must clearly distinguish `reminder enabled` from `notification delivery unavailable`.
- The screen must not silently switch preferences off solely because permission is unavailable.

## Tio UI Specification

- Use the existing Tio Ledger design system and reusable settings/list primitives available at implementation time.
- Use a simple sectioned list rather than cards that imply financial values.
- Each reminder row contains a text label, optional short supporting text, and a platform-appropriate switch.
- The entire row should remain readable with large text; the switch must have an explicit semantic label/state.
- Permission state uses text plus an optional status icon. Color is never the only signal.
- `Allow notifications` and `Open notification settings` are explicit user actions with clear accessible labels.
- Permission guidance must remain non-modal and non-blocking except for the Android-owned runtime permission dialog launched after the explicit action.
- Do not show money amounts, EMI calculations, budget spend, thresholds, or due-date calculations on this Settings screen.
- Do not introduce `Float` or `Double` money calculations or any financial computation in presentation code.
- Light and dark themes use existing Tio tokens rather than reference-source colors.

## Navigation Definition

- Reuse existing `MainRoute.Settings` with path `settings` and `TioIconToken.Settings`.
- Do not create a second Settings route.
- Do not add Settings as a sixth primary bottom-navigation destination. The canonical primary destinations remain Dashboard, Accounts, Transactions, Categories, and Budgets.
- The Settings entry is a non-primary app-shell action equivalent to the reference product's `More`/settings hierarchy. A later implementation PR may expose it through an app-bar overflow or other already-approved non-primary shell affordance, but it must route to the existing `MainRoute.Settings` destination.
- The Settings screen remains inside the main graph and preserves the existing primary bottom navigation unless a separate approved navigation decision changes that shell behavior.
- Selecting any primary bottom-navigation item exits Settings through the existing typed main-route callbacks.
- Notification deep links continue to target their functional destinations (Loan Details or Budgets), not Settings.

## Error And Edge States

- Preference write failure: retain/reload persisted state and show inline text; do not pretend the switch changed successfully.
- Runtime request unavailable/null: do not invent another permission API; refresh state and show platform guidance when appropriate.
- App notifications disabled after a previous grant: present as `REVOKED`/settings guidance.
- Permission changes outside the app: refresh snapshot on screen resume and enqueue permission-state reconciliation through the Android platform service.
- Both reminder preferences disabled: show both switches off; permission row may remain visible but should not pressure the user to enable notifications.
- No planned reminders: Settings still reflects preferences and permission status; it does not infer or display financial eligibility.

## Accessibility Requirements

- Switches expose accessible names and checked states independently of surrounding text.
- Status text explicitly states whether notification delivery is available, not requested, denied, or disabled.
- Permission status is never communicated only by icon or color.
- All interactive rows/actions meet the project touch-target standard and remain keyboard/switch-access reachable.
- Dynamic text must wrap without overlapping switches or action controls.
- Screen reader order follows visual hierarchy: title, reminder controls, delivery status, contextual action/error.
- Android runtime permission is requested only after an accessible, explicit user action.
- Denied/revoked states include clear next-step guidance without repeated prompts or blocking dialogs.
- Light/dark theme contrast and large-font behavior require review before production approval.

## Localized Copy Requirements

- Production strings live in Android/shared resource infrastructure, not hard-coded in Compose.
- Keep labels short and literal: Settings, Reminders, EMI reminders, Budget reminders, notification delivery status, permission action, settings guidance, and errors.
- Avoid implying that permission changes financial data.
- Avoid alarm/exact-time promises because v1 delivery is WorkManager best-effort and does not require exact-alarm permission.
- Translation review must preserve the difference between reminder preference and notification permission.

## Intentional Deviations

1. **Two reminder types only.** The reference product's generic daily alarm workflow is not reproduced. Tio Ledger v1 exposes only the approved global EMI and budget reminder preferences from issue #43.
2. **No sixth bottom-navigation destination.** The reference product uses a `More` hierarchy. Tio Ledger preserves its already-approved five primary destinations and exposes Settings as a non-primary route.
3. **Dedicated explicit permission action.** Opening Settings or changing a preference does not automatically launch the Android runtime prompt. The user chooses a dedicated action first.
4. **Preference survives permission denial.** Reminder preference and Android delivery capability are shown independently so user intent is preserved.
5. **No exact-alarm UX.** Tio Ledger does not ask for exact-alarm permission or promise delivery exactly at 09:00.

## Reasons For Deviations

- Repository navigation and issue #43 explicitly require preservation of the canonical five primary destinations and non-blocking permission behavior.
- The Android foundation keeps preferences and permission history as platform-local non-financial metadata.
- Shared reminder planning is already authoritative; adding Realbyte daily-alarm semantics would duplicate or change approved business rules.
- A dedicated explicit permission action provides a clearer consent boundary and guarantees that the first runtime prompt is user initiated.

## Pixel Review Plan

- Verify phone-width light and dark rendering.
- Verify long localized labels and supporting text wrap without clipping or switch overlap.
- Verify all five permission states produce a stable layout without large jumps.
- Verify inline write errors do not obscure controls.
- Verify large font scaling keeps status and actions readable.
- Verify canonical bottom navigation remains unchanged while Settings is active.

## Accessibility Review Checklist

- [ ] TalkBack announces both reminder switches with correct checked state.
- [ ] Permission status has an explicit textual description.
- [ ] `Allow notifications` is focusable and clearly described before launching the OS dialog.
- [ ] Denied/revoked guidance is reachable without repeated runtime permission prompts.
- [ ] Touch targets and keyboard/switch access meet project standards.
- [ ] Large text does not clip labels, actions, or switch controls.
- [ ] Light/dark contrast is reviewed.
- [ ] Status is not communicated by color alone.

## Functional Acceptance Checklist

- [ ] Existing `MainRoute.Settings` is reused.
- [ ] Canonical five bottom-navigation destinations remain unchanged.
- [ ] Only global EMI and budget reminder controls are exposed.
- [ ] Initial values come from the Android platform settings snapshot.
- [ ] Preference writes use the Android reminder settings service and trigger reconciliation outside UI.
- [ ] UI contains no reminder eligibility, schedule, budget-threshold, loan, or money calculations.
- [ ] Opening Settings never requests notification permission.
- [ ] First Android 13+ runtime request occurs only after explicit `Allow notifications` action.
- [ ] Denial is not repeatedly prompted.
- [ ] Revoked/disabled notifications lead to settings guidance.
- [ ] Permission denial does not block any financial workflow.
- [ ] Preference state is distinguishable from effective delivery state.
- [ ] Preference write failures do not show false persisted state.
- [ ] Production strings are localized resources.
- [ ] Pixel review is completed.
- [ ] Accessibility review is completed.
- [ ] No financial schema, ledger, history, or shared reminder rule changes are introduced.

## Deviation Log

| Area | Reference behavior | Tio Ledger v1 decision | Approval basis |
| --- | --- | --- | --- |
| Settings entry | Reference uses a `More`/settings hierarchy | Reuse typed `MainRoute.Settings` through a non-primary shell action | Existing route + canonical five-destination rule |
| Reminder scope | Generic reminding alarm | EMI + budget reminder preferences only | Issue #43 scope |
| Permission request | Reference directs user to enable app notifications | Dedicated explicit Android runtime-permission action, then settings guidance after denial/revocation | Issue #43 permission contract |
| Scheduling promise | Generic alarm reminder | Best-effort WorkManager delivery; no exact-alarm permission | Merged PR #52 architecture and issue #43 |
| Financial behavior | Not applicable | No financial write or calculation from Settings/notification actions | Tio Ledger automation and ledger rules |

## Approval Record

Issue #51 reference readiness is approved for implementation planning when this document is merged. The user's `go next` instruction authorizes the official Realbyte Help Center fallback source. Production Compose code remains out of scope for this documentation branch and must consume the merged Android foundation APIs without duplicating reminder rules.
