# Android Reminder Validation Follow-up

Status: In Progress
Objective: Close the remaining Android reminder acceptance gaps recorded in issue #56 after PR #55 merged, without reopening merged Settings UI scope unless a concrete defect is found.
Branch: `main`
Scope: `apps/android` validation follow-up, issue hygiene, and only defect-driven Android reminder fixes
Created: `2026-08-12`
Last Updated: `2026-09-28`
Issue: `#56`
Parent: `#43`

## Required Context

- `.ai/core/ui-rules.md`
- `.ai/core/workflow-rules.md`
- `docs/emi-budget-reminders-v1.md`
- `docs/implementation-roadmap.md`
- `docs/definition-of-done.md`
- `apps/android/src/main/kotlin/com/tioledger/apps/android/reminders/`
- `apps/android/src/main/kotlin/com/tioledger/apps/android/MainActivity.kt`

## Constraints

- No speculative Android reminder refactor.
- No GitHub issue edits, closes, or comments unless explicitly requested.
- No financial, ledger, SQLDelight, or shared reminder-planner rule changes.
- No new branch until a real defect or scoped implementation change is confirmed.
- Treat device validation as authoritative over stale local continuity notes.

## Current Evidence

- PR `#55` merged to `main` on `2026-08-12`.
- Merged implementation includes Settings UI, explicit permission UX, system-back fix, duplicate-title removal, preview coverage, and accessibility hardening.
- Local and CI validation already passed for shared metadata compile, critical tests, SQLDelight migration verification, `ktlintCheck`, `detekt`, `:apps:android:assembleDebug`, `:apps:android:test`, and `git diff --check`.
- Verified on device/emulator: `Settings -> system back -> Accounts`, duplicate title removed, phone-width light/dark review, large-text layout, and key notification-permission transitions.
- Local Android SDK audit on `2026-08-12` found `platforms;android-30` installed. A representative pre-Android-13 phone system image `system-images;android-30;google_apis;x86_64` is now installed locally, but the only existing phone AVD is still `Pixel_9`, so a dedicated Android 11 phone AVD remains pending.
- `2026-09-28`: local environment had drifted since `2026-08-12` (no `Pixel_9`/`TioLedger_Android11` AVD existed any more, only `Pixel_9_API35` and `Wear_OS_Large_Round`) — confirmed live per this task's own "treat device validation as authoritative" constraint rather than trusting the stale note above. Created `TioLedger_Android11` (`system-images;android-30;google_apis;x86_64`, `pixel_3a` device profile) and ran it.
- `2026-09-28` pre-Android-13 (API 30) validation on `TioLedger_Android11`, against `main`/`HEAD` (`b0e9448`, includes merged PR #59):
  - Opening Settings never triggered any OS permission prompt.
  - Notification delivery row correctly showed `NOT_REQUIRED` status: "Notifications available — This Android version does not require a runtime notification permission." (matches the settings-reminders.md permission matrix exactly.)
  - Toggled EMI reminders on; no permission dialog appeared (correct, API 30 predates POST_NOTIFICATIONS).
  - Force-stopped and relaunched the app: EMI-on/Budget-off preference state persisted correctly across restart.
  - `adb logcat` (app PID, filtered for exception/error/fatal, plus a global `AndroidRuntime:E *:F` pass) showed nothing — no crash, no exception, during any of the above.
  - `uiautomator dump` of the Settings screen confirmed accessible reading order and combined content-desc labels matching the approved spec exactly, e.g. `"EMI reminders. Receive upcoming and due loan installment reminders."` with `checkable=true`/`checked` reflecting the switch state, and the permission row's status conveyed as text (not color alone). This is the accessibility-tree-level evidence TalkBack announcements are driven from; literal TalkBack audio narration was not run.

## Remaining Gaps

- ~~Representative pre-Android-13 reminder permission/status validation.~~ Closed by the `2026-09-28` evidence above.
- Delivery, deep-link, restart, reboot, timezone, and upgrade lifecycle evidence where safe eligible data can be exercised — still open; needs a seeded loan/budget with a near-term due reminder to exercise for real, which is a larger, separate effort than this pass.
- Formal TalkBack / screen-reader order and focus verification — accessibility-tree evidence gathered `2026-09-28` (see above); literal TalkBack audio narration still not run.
- Decision on whether issue `#56` should stay open for broader production acceptance evidence or split into narrower follow-up tasks — still open, not decided.
- Manual audit of open issues `#54` and `#57`, which still appear open despite merged implementation — not done this pass.
- New, separately filed (not part of `#56`'s original scope, discovered during PR #59): issue #60 (`MainActivity` hardcodes `darkTheme = false`, app never follows system dark mode) and issue #61 (no navigation entry point reaches `MainRoute.Loans`).

## Next Action

Pre-Android-13 permission/status validation is done. The next slice is the delivery/lifecycle evidence gap:

1. Seed a loan or budget with a near-term due date on `TioLedger_Android11` (or a fresh instance of it) so a real EMI/budget reminder becomes eligible.
2. Exercise delivery, the notification deep link into `LoanDetails`/`Budgets`, app restart, device reboot (`adb reboot`), timezone change, and an app-upgrade path (install over an older APK) against that seeded data.
3. Record pass/fail evidence per gap above; only then decide whether `#56` should stay open, split, or close, and whether `#54`/`#57` need action — no GitHub issue edits without explicit request per this task's constraints.
