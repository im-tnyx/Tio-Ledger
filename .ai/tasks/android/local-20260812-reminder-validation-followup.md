# Android Reminder Validation Follow-up

Status: In Progress
Objective: Close the remaining Android reminder acceptance gaps recorded in issue #56 after PR #55 merged, without reopening merged Settings UI scope unless a concrete defect is found.
Branch: `main`
Scope: `apps/android` validation follow-up, issue hygiene, and only defect-driven Android reminder fixes
Created: `2026-08-12`
Last Updated: `2026-08-12`
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

## Remaining Gaps

- Representative pre-Android-13 reminder permission/status validation.
- Delivery, deep-link, restart, reboot, timezone, and upgrade lifecycle evidence where safe eligible data can be exercised.
- Formal TalkBack / screen-reader order and focus verification.
- Decision on whether issue `#56` should stay open for broader production acceptance evidence or split into narrower follow-up tasks.
- Manual audit of open issues `#54` and `#57`, which still appear open despite merged implementation.

## Next Action

Provision a representative pre-Android-13 phone emulator environment before any new branch work:

1. create a pre-Android-13 phone AVD from the newly installed Android 11 system image,
2. rerun the outstanding `#56` permission/status checks on that environment,
3. only then decide whether a defect-driven Android branch is actually needed.
