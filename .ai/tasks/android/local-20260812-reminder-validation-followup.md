# Android Reminder Validation Follow-up

Status: Blocked
Objective: Close the remaining Android reminder acceptance gaps recorded in issue #56 after PR #55 merged, without reopening merged Settings UI scope unless a concrete defect is found.
Branch: `docs/emi-validation-readiness` (continuity only); validation runs against `main`
Scope: `apps/android` validation follow-up, issue hygiene, and only defect-driven Android reminder fixes
Created: `2026-08-12`
Last Updated: `2026-09-29`
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
- No financial test data (accounts, loans) may be created without explicit owner approval of the exact fixture; never inject DB rows.
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
  - `uiautomator dump` of Settings showed switch nodes exposing `checkable=true`/`checked` and combined labels (e.g. `"EMI reminders. Receive upcoming and due loan installment reminders."`), and delivery status as text. Correction: the dump is view-hierarchy order, not TalkBack traversal order, and it listed the `Settings` title after the content, so it is **not** screen-reader-order evidence. TalkBack spoken output was not run.
- `2026-09-28` delivery/lifecycle pass on `TioLedger_Android11` (API 30, IST). Scenario built only through production UI/use cases: Cash/INR account `Wallet` → EXPENSE category `Food` → Monthly budget `Groceries` INR 100.00 (Food) → expenses INR 85.00 / 15.00 / 5.00. No DB injection; DB inspected read-only (`sqlite3 -readonly` via `run-as`).
  - Defect 1 (found on `main@8a9c029`): `RELEVANT_DATA_CHANGED` was declared but never enqueued. After the 85.00 expense (budget `Near limit`), no reconciliation/notification for 3+ min while the same process stayed alive; force-stop + relaunch then delivered `Budget warning` immediately. Fixed by PR #62, merged to `main` as `b37376b` on 2026-09-29 (exact-head CI run #418 green; app/shared code identical to validated `ea5c80f`): read-only SQLDelight change listener on `loans`, `emi_schedules`, `budgets`, `transactions`, `transaction_splits`, `ledger_entries` enqueues reconciliation.
  - Revalidated on `ea5c80f`: upgrade-install over `8a9c029` data → process started for `ReminderReconciliationReceiver` broadcast, reconcile SUCCESS, no duplicate WARNING (PASS); cold boot → process started for the same receiver, reconcile SUCCESS, no duplicate (PASS); 15.00 expense in same process → `Budget limit reached` ~2s later, no restart (PASS, fix verified); notification tap → Budgets screen, auto-cancel, DB unchanged (PASS); force-stop + relaunch → no duplicate REACHED, receipts keyed `budget|<id>|<periodStart>|<status>` (PASS); Budget reminders disabled → EXCEEDED transition not delivered (PASS); re-enabled → EXCEEDED delivered once (PASS).
  - Defect 2 (open, tracked by #64, needs spec decision): time-zone change `Asia/Kolkata → America/New_York` reconciled (hook PASS) but re-delivered `Budget limit exceeded` for the same budget/month/state, because `periodStartInclusive` is a time-zone-dependent instant (`1788201000000` IST vs `1788235200000` EDT) inside the stable budget identity. Violates "no recurring reminder while the same state remains active". Fix would change the shared reminder identity rule defined in `docs/emi-budget-reminders-v1.md` → outside this task's constraints; not changed.
  - Final DB: 3 transactions / 6 ledger entries, DEBIT 10500 = CREDIT 10500, all from the three user-entered expenses; no notification, settings, upgrade, reboot, or time-zone action mutated financial rows.
  - Not testable: any EMI delivery/deep link/cancellation (creating an ACTIVE loan needs `LoansScreen`, which no production UI reaches — issue #61); cancellation of *pending* budget work (budget plans deliver immediately, nothing stays pending).
  - Observation (out of #56 scope): the Accounts/Transactions FAB content description is not exposed in the uiautomator tree (only the placeholder glyph text).

## EMI Validation Readiness (2026-09-29, read-only)

- #61 closed (#69 `ba5fac5`, #70 `3a986a0`): Accounts app bar → Loans is reachable from production UI; EMI checks are no longer blocked by navigation. No EMI validation performed yet.
- Eligibility (source): loan `ACTIVE` + installment `PENDING` (both set by `CreateLoanUseCase`, which posts no ledger entries); linked account active `LOAN_LINKED`; disbursement a different active non-loan ASSET account with the same currency; EMI preference enabled; delivery at local 09:00 on due-3 and due-day; past delivery instants skipped; first due = start date + 1 month (clamped).
- Device `TioLedger_Android11` (API 30, `Asia/Calcutta`), app code = `main@3a986a0`: `emi_enabled=false`, `budget_enabled=true`; only account Wallet (CASH, INR, active); 0 loans / 0 EMI schedules; no enqueued WorkManager work; 3 transactions / 3 splits / 6 ledger entries, DEBIT = CREDIT = 10500.
- Proposed fixture (awaiting owner approval; see #56): new `LOAN_LINKED` INR account "Device Validation Loan Account"; loan "Device Validation EMI Loan", INR 1,000.00, 0.00%, 1 month, start 2026-09-03, linked → new account, disbursed → Wallet; expected single installment due 2026-10-03, EMI INR 1,000.00; reminders 2026-09-30 09:00 IST (lead 3) and 2026-10-03 09:00 IST (due day); plus enabling EMI reminders in Settings.

## Remaining Gaps

- EMI delivery, deep link, disable → cancellation, restart/reboot restore, time-zone rescheduling: ready; awaiting explicit owner approval of the test fixture.
- TalkBack spoken-output, screen-reader order, keyboard/switch access: not run (not blocked).
- Remaining permission-matrix breadth items in `#56` (denial vs broader financial workflows, full five-state layout, preference-write-error visual state).
- Tracker hygiene noted, not acted on: #54 still open although PR #55 merged on 2026-08-12.
- Separately filed, out of scope: #60 (dark theme).

## Next Action

Wait for explicit owner approval of the exact EMI fixture; only then create it through production UI and run the planned EMI matrix.
