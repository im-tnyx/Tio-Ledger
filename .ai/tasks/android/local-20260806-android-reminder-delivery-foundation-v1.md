# Android Reminder Delivery Foundation V1

Status: In Progress
Objective: Implement the non-UI Android reminder delivery foundation from issue #50 by consuming the validated shared/Application reminder plans without duplicating business or financial rules.
Branch: `feat/android-reminder-delivery-foundation-v1`
Platform Scope: `android`
Created: `2026-08-06`
Last Updated: `2026-08-06`
Issue: `https://github.com/im-tnyx/Tio-Ledger/issues/50`
Parent Issue: `https://github.com/im-tnyx/Tio-Ledger/issues/43`
UI Reference Issue: `https://github.com/im-tnyx/Tio-Ledger/issues/51`
Specification: `docs/emi-budget-reminders-v1.md`

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
- Reconciliation will compare desired Application plans with a platform-local scheduled snapshot and emit deterministic schedule/replace/cancel/no-op operations.
- Budget delivery receipts will use stable identity keys and an injectable bounded-entry policy; no financial timestamp or state semantics will be inferred from receipts.
- Notification content is produced from Android string resources using semantic Application DTO data.
- Typed destination extras bridge to Loan Details or Budgets without exposing financial mutation actions.

## Progress

- [x] Audit updated `main`, canonical reminder specification, Android app shell, dependencies, and UI reference readiness.
- [x] Split umbrella issue #43 into foundation issue #50 and reference-first UI issue #51.
- [x] Record branch-cleanup maintenance separately in issue #49.
- [ ] Inspect existing navigation, Application DTOs, Gradle conventions, and test patterns.
- [ ] Add Android scheduling dependencies and platform-local contracts/stores.
- [ ] Implement deterministic reconciliation and WorkManager adapter.
- [ ] Implement notification delivery, channels, destinations, and lifecycle hooks.
- [ ] Add focused Android tests and CI coverage.
- [ ] Update applicable architecture/module documentation.
- [ ] Run exact-head CI, review diff, and open PR.

## Validation

Not run yet.

## Next Action

Inspect existing navigation, reminder DTOs, Android Gradle conventions, and test setup before implementation.
