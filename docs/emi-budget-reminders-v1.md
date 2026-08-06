# EMI And Budget Reminders V1

Status: Approved
Approved: 2026-08-06
Tracking Issue: [#41](https://github.com/im-tnyx/Tio-Ledger/issues/41)

## Purpose

Define the first production reminder contract for Tio Ledger before notification code is introduced.

The v1 objective is to provide useful, deterministic reminders without noisy follow-up, hidden financial mutation, platform leakage into shared business rules, or a change to the frozen financial database schema.

## Scope

V1 includes:

- Shared deterministic EMI reminder planning.
- Shared deterministic budget-state reminder planning.
- Application-layer orchestration over existing loan and budget read paths.
- Global EMI and budget reminder controls.
- Android-first permission, scheduling, cancellation, delivery-receipt, and navigation integration.
- Compatible platform adapter contracts for later iOS delivery.
- Phone-mirrored Wear notification behavior.

V1 does not include:

- Posting or marking an EMI paid from a notification.
- Creating, editing, or archiving budgets from a notification.
- Overdue EMI follow-up.
- Per-loan or per-budget reminder settings.
- Recurring-transaction prompts.
- Exact-alarm permission solely for precise delivery.
- Independent Wear scheduling.
- Production iOS notification delivery.
- Cloud synchronization of reminder preferences or receipts.
- SQLDelight schema or migration changes.

## Non-Negotiable Safety Rules

- Reminder planning and delivery are read-only with respect to financial records.
- No notification action may create, edit, post, reverse, adjust, or silently modify financial history.
- Loan and budget eligibility rules live outside Compose and platform UI.
- Existing financial engines remain authoritative. Notification code must not reproduce EMI, budget-threshold, spend, balance, or interest calculations.
- Money values use the existing precise `Money` model and integer minor units. `Float` and `Double` are prohibited for money.
- Permission denial must not block any core financial workflow.

## EMI Reminder Rules

### Eligibility

An EMI reminder candidate is eligible only when:

- The loan status is `ACTIVE`.
- The installment status is `PENDING`.
- The installment has a persisted due date.
- The global EMI reminders setting is enabled.

The following are ineligible:

- `PAID`, `WAIVED`, `ADJUSTED`, or `OVERDUE` installments.
- Loans that are `CLOSED` or `DRAFT`.
- Installments whose eligible delivery time is already in the past when planning runs.

### Schedule

For every eligible installment, plan at most two reminders:

1. Three local-calendar days before the due date.
2. On the due date.

The preferred local delivery time is 09:00 in the explicitly supplied timezone.

Calendar-day subtraction must use local date arithmetic. It must not subtract a fixed number of milliseconds or assume every day is exactly 24 hours.

Android delivery is best-effort within normal platform constraints. V1 must not request exact-alarm permission solely to guarantee 09:00 delivery.

### Stable Identity

The stable EMI reminder identity is:

```text
(loanId, installmentId, leadDays)
```

Where `leadDays` is `3` or `0`.

A due-date change keeps the same stable identity but updates the planned delivery instant. A status or loan-state change that makes the installment ineligible cancels the corresponding scheduled identities.

### Destination

Tapping an EMI reminder opens Loan Details for the relevant `loanId`.

V1 exposes no action button that posts a payment or changes the installment status.

## Budget Reminder Rules

### Authoritative State

Budget notification planning consumes the existing Application `BudgetSummary` state.

The notification layer does not calculate spend or utilization and does not redefine thresholds. The existing budget engine remains authoritative:

- `ON_TRACK`
- `WARNING`
- `REACHED`
- `EXCEEDED`

The existing `WARNING` threshold remains 80% utilization.

### Eligibility And Transition Behavior

An eligible budget reminder is emitted once when a budget first enters one of these states within its current budget period:

- `WARNING`
- `REACHED`
- `EXCEEDED`

No reminder is emitted for `ON_TRACK`.

No recurring reminder is emitted while the same state remains active. A later transition to a different eligible state may produce one new reminder.

### Stable Identity

The stable budget reminder identity is:

```text
(budgetId, periodStartInclusive, status)
```

A delivery receipt for this identity prevents duplicate delivery after process restart or idempotent replanning.

Old budget-period receipt identities may be pruned after they are no longer needed. Receipt retention must be bounded.

### Destination

Tapping a budget reminder opens Budgets.

V1 exposes no notification action that creates or edits a budget or alters transactions.

## User Controls

V1 provides two global controls:

- EMI reminders enabled/disabled.
- Budget reminders enabled/disabled.

Per-loan and per-budget controls are deferred.

Reminder preferences are non-financial platform-local state. They are not stored as ledger entries or balances and do not require a SQLDelight financial-schema migration in v1.

Disabling a reminder type cancels its scheduled platform jobs. Re-enabling triggers an idempotent replan.

## Shared Contract

`shared:notifications` owns pure, repository-free planning.

The shared contract must represent these concepts as immutable models:

- `ReminderIdentity`
- `ReminderPlan`
- `ReminderDestination`
- `EmiReminderCandidate`
- `BudgetReminderCandidate`
- `ReminderPreferencesSnapshot`
- `ReminderPlanningContext`

A reminder plan contains, at minimum:

- Stable identity.
- Reminder type.
- Planned delivery epoch milliseconds.
- Explicit timezone ID used for planning.
- Semantic destination and route payload.
- Semantic content data required for platform localization.

Shared plans must not contain:

- Android notification objects.
- iOS notification objects.
- Compose navigation controllers.
- Localized user-facing strings.
- Repository implementations.
- SQLDelight rows.

The planner accepts an injected current timestamp and explicit timezone. It must be deterministic for the same inputs.

## Application Orchestration

`shared:application` owns read-path orchestration and mapping to planner inputs.

The first implementation may use existing contracts:

- List loans.
- Read persisted details for active loans.
- List budget summaries for an explicit timestamp and timezone.

A new all-upcoming-installments repository contract is not required for v1 unless profiling demonstrates a real problem.

Application orchestration returns immutable reminder plans or typed failures. It does not schedule platform notifications directly.

This creates the dependency direction:

```text
apps/* -> shared:application -> shared:notifications
shared:notifications -> shared:domain
shared:notifications -> shared:loan-engine
shared:notifications -> shared:budget-engine
```

The planner must consume persisted schedule results and existing budget states rather than duplicating engine calculations.

## Platform Adapter Boundary

Platform apps/services own:

- Permission request and status APIs.
- Local scheduling and cancellation.
- Platform-local reminder settings.
- Platform-local delivery receipts.
- Notification channel/category configuration.
- Localized title and body generation.
- Deep-link or typed destination bridging.
- App startup, reboot, upgrade, timezone-change, and permission-change hooks.

The shared planner owns none of these platform APIs.

### Android V1

Android is the first production delivery adapter.

The Android implementation must:

- Request notification permission only from an explicit user action where required by the OS.
- Avoid repeated permission prompts after denial.
- Offer settings guidance when permission is denied or revoked.
- Use normal best-effort background scheduling without requiring exact-alarm permission solely for the preferred 09:00 delivery time.
- Use stable unique work/scheduling identities for idempotent replace and cancellation behavior.
- Restore/reconcile scheduling after app startup, reboot recovery, app upgrade, timezone change, relevant data change, reminder-setting change, or permission change where Android permits.

### iOS And Wear

V1 keeps adapter contracts compatible with a later iOS implementation but does not claim production iOS delivery.

Wear OS mirrors phone notifications in v1. Independent watch scheduling requires separate approval.

## Delivery Receipts And Deduplication

Delivery receipts are non-financial platform-local metadata.

Receipts must:

- Be keyed by the stable reminder identity.
- Prevent repeated budget transition delivery after process restart.
- Support bounded cleanup.
- Never be interpreted as payment, budget, ledger, or transaction state.

EMI scheduled work uses stable identity for replace/cancel semantics. A delivered EMI receipt may be recorded for diagnostics or duplicate prevention, but it must not alter installment status.

## Rescheduling And Cancellation

Replanning is idempotent.

Reconciliation compares desired shared plans with platform-scheduled identities:

- Missing desired identity: schedule it.
- Existing desired identity with changed delivery time: replace/reschedule it.
- Existing platform identity no longer desired: cancel it.
- Identical desired identity and time: no-op.

Replanning triggers include:

- App startup.
- Relevant loan or installment data changes.
- Relevant budget-summary changes.
- Global reminder-setting changes.
- Timezone changes.
- Permission changes.
- Reboot recovery where supported.
- App upgrade where supported.

## Error And Permission Behavior

- Planner validation failures are typed and testable.
- Repository/read failures return typed Application errors.
- Platform scheduling failures do not mutate financial data.
- Permission denial or scheduling failure leaves the app usable and can expose a non-blocking status or settings action.
- The app must not claim a reminder is scheduled when the platform adapter reports failure.

## Localization And Accessibility

Shared plans carry semantic data, not final strings.

Platform presentation produces localized titles and bodies and must include enough text to understand:

- Which loan or budget is involved.
- The due date or budget state.
- The relevant precise amount when included.
- The destination opened on tap.

Meaning must not depend only on color, icon, sound, or vibration.

## Testing Requirements

### Shared Planner

- EMI three-day and due-day boundaries.
- Multiple timezones.
- Month-end and year-end boundaries.
- Daylight-saving transitions.
- Past delivery times are omitted.
- Ineligible loan and installment statuses.
- Stable identity and due-date rescheduling.
- Budget transition identities and duplicate suppression.
- Budget period rollover.
- Deterministic repeated planning.
- Money values remain precise.

### Application

- Existing loan read-path orchestration.
- Existing budget summary read-path orchestration.
- Repository failure mapping.
- Invalid timestamp/timezone validation.
- Immutable plan output.
- No financial write use case invocation.

### Android Adapter

- Permission granted, denied, revoked, and re-enabled.
- Toggle enable/disable behavior.
- Idempotent scheduling.
- Replace and cancellation behavior.
- Delivery receipt persistence and bounded cleanup.
- Startup, reboot, upgrade, and timezone reconciliation hooks.
- Deep-link destination mapping.
- No exact-alarm permission requirement for v1.

## Implementation Sequence

1. Merge this canonical specification.
2. Implement the pure shared planner and Application orchestration in a focused issue/PR.
3. Implement Android preferences, permissions, scheduling, cancellation, delivery receipts, and destination bridging in a separate issue/PR.
4. Perform device-level permission, delivery, timezone, reboot, and accessibility verification.
5. Consider iOS production delivery and independent Wear scheduling only as separately approved milestones.

## Definition Of Done Boundary

The documentation milestone is complete when:

- This specification is merged and indexed.
- Issue #41 records approved decisions.
- Shared-planner and Android-adapter implementation issues exist.
- No production source, schema, or financial behavior changed.
- Exact-head repository CI passes.

The reminder feature itself is not complete until the later implementation PRs satisfy the full repository Definition of Done.
