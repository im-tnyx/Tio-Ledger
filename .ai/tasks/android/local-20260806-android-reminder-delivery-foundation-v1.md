# Android Reminder Delivery Foundation V1

Status: Ready for Review
Objective: Implement the non-UI Android reminder delivery foundation from issue #50 by consuming the validated shared/Application reminder plans without duplicating business or financial rules.
Branch: `feat/android-reminder-delivery-foundation-v1`
Platform Scope: `android`
Created: `2026-08-06`
Last Updated: `2026-08-06`
Issue: `https://github.com/im-tnyx/Tio-Ledger/issues/50`
Parent Issue: `https://github.com/im-tnyx/Tio-Ledger/issues/43`
UI Reference Issue: `https://github.com/im-tnyx/Tio-Ledger/issues/51`
Specification: `docs/emi-budget-reminders-v1.md`
Pull Request: `https://github.com/im-tnyx/Tio-Ledger/pull/52`

## Required Context

- `.ai/core/architecture.md`
- `.ai/core/workflow-rules.md`
- `AGENTS.md`
- `README.md`
- `docs/README.md`
- `docs/engineering-guidelines.md`
- `docs/product-requirements.md`
- `docs/architecture.md`
- `docs/adr/README.md`
- `docs/adr/0014-smart-automation-philosophy.md`
- `docs/definition-of-done.md`
- `docs/module-design.md`
- `docs/implementation-roadmap.md`
- `docs/emi-budget-reminders-v1.md`
- `.github/PUSH_TEMPLATE.md`
- `.github/PULL_REQUEST_TEMPLATE.md`
- `apps/android/build.gradle.kts`
- `apps/android/src/main/AndroidManifest.xml`
- `apps/android/src/main/kotlin/com/tioledger/apps/android/MainActivity.kt`
- `apps/android/src/main/kotlin/com/tioledger/apps/android/TioAndroidApplication.kt`
- `shared/application/src/commonMain/kotlin/com/tioledger/application/usecase/notification/PlanRemindersUseCase.kt`
- `shared/notifications/src/commonMain/kotlin/com/tioledger/notifications/ReminderModels.kt`
- `shared/ui/src/commonMain/kotlin/com/tioledger/ui/navigation/Routes.kt`

## Constraints

- Consume `PlanRemindersUseCase` output as the only desired-plan source.
- Do not reproduce EMI eligibility, due-date, budget threshold, spend, balance, interest, or money calculations in Android code.
- Keep all scheduling, preferences, receipts, permission status, channels, intents, and lifecycle hooks inside `apps/android`.
- Do not create a production Settings/permission Compose surface; issue #51 must approve UI artifacts first.
- Runtime notification permission requests may only be triggered by an explicit future user action.
- Use best-effort WorkManager delivery; do not request exact-alarm permission.
- Preferences and receipts are bounded non-financial platform-local metadata.
- No SQLDelight schema/migration, financial repository, ledger, transaction, loan payment, budget, or balance writes.
- No independent Wear or iOS scheduling.

## Decisions

- Use `androidx.work:work-runtime` 2.11.2 for persistent best-effort unique work scheduling.
- Stable platform work names derive from shared `identityKey`; reminder-type tags enable targeted cancellation.
- Reconciliation compares desired Application plans with a platform-local scheduled snapshot and emits deterministic schedule/replace/cancel/no-op operations.
- Budget delivery receipts use stable identity keys and bounded deterministic cleanup; receipts are never interpreted as financial state.
- Notification content is produced from Android string resources using semantic Application DTO data.
- Typed destination extras bridge to Loan Details or Budgets without exposing financial mutation actions.
- Effective notification availability includes both Android 13 runtime permission and the app-level notification-enabled state.

## Progress

- [x] Audit updated `main`, canonical reminder specification, Android app shell, dependencies, and UI reference readiness.
- [x] Split umbrella issue #43 into foundation issue #50 and reference-first UI issue #51.
- [x] Record branch-cleanup maintenance separately in issue #49.
- [x] Inspect existing navigation, Application DTOs, Gradle conventions, and test patterns.
- [x] Add Android scheduling dependencies and platform-local contracts/stores.
- [x] Implement deterministic reconciliation and WorkManager adapter.
- [x] Implement notification delivery, channels, destinations, and lifecycle hooks.
- [x] Add focused Android tests and CI coverage.
- [x] Update applicable architecture/module documentation.
- [x] Review production diff for platform, architecture, financial, and automation safety.
- [x] Open draft PR #52 and validate the implementation head.

## Validation

Implementation head `d363ee4f8bdd2d0c833f7034caaee1c37eda3772`:

- CI run #389 targeted validation passed shared metadata compilation, Android application compilation, shared and Android unit tests, `ktlint`, and `detekt`.
- CI run #386 passed the same targeted gates plus SQLDelight migration verification after the effective-notification-state fix.
- Run #389's queued migration job was cancelled by GitHub after newer branch activity; it was not a migration failure.
- The final review-state head must pass both CI jobs before PR #52 is marked Ready for Review.
- Local `git diff --check` was not run through the connector and is not claimed.
- Manual device delivery, reboot, timezone, permission-dialog, localization, and accessibility sign-off are not claimed by this non-UI foundation; they remain under parent issue #43 and UI issue #51.

## Changed Areas

- `apps/android` reminder platform adapter, lifecycle wiring, resources, and tests.
- Android Gradle dependency and version catalog.
- CI Android compile/test coverage and failure-only lint diagnostics.
- Module design, architecture changelog, and AI continuity.

## Next Action

Review PR #52 and merge only after explicit approval. After main synchronization, archive this task and continue the reference-backed reminder controls and permission experience in issue #51.
