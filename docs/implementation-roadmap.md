# Implementation Roadmap

## Current Milestone Status

- Ledger Engine: Frozen v1 as of 2026-06-30.
- Approved architectural blockers for commonMain compatibility, type-safe posting strategies, and strategy registry separation are resolved.
- Application Layer, repository contracts, SQLDelight repository implementations, application bootstrap, and shared UI foundation are implemented.
- Dashboard Screen v1, Accounts Screen v1, Transaction Entry Integration v1, Transactions List / History Screen v1, Categories Screen v1, Budgets Screen v1, Loan Creation and Loan Details v1, SMS-Assisted Transaction Review Flow v1, Spending Analytics / Reports Screen v1, Cash-flow Analytics v1, Loan Payoff Analytics v1, and Shared Reminder Planner v1 are implemented and CI-validated on `main`.
- Loan Payoff Analytics v1 is merged through issue #29 and PR #30.
- Primary bottom-navigation callback wiring is completed through issue #34 and PR #36 without changing the canonical five destinations or redesigning Dashboard content.
- Phase 6 Finance Features is complete.
- Phase 7 Analytics And Notifications is in progress.
- EMI and budget reminders v1 product rules and platform boundaries are approved through issue #41, and the pure shared planner/Application slice is implemented through issue #42 and PR #47. Android delivery integration under issue #43 is the active next milestone.

## Phase 0: Documentation And Decisions

Objectives:

- Establish product requirements.
- Define architecture and module boundaries.
- Record major ADRs.
- Design Loan Engine behavior before production code.
- Define UX reference governance.
- Define SMS-assisted capture rules before implementation.
- Define ledger-first accounting invariants.
- Define feature flag policy.
- Define engineering guidelines for production code and AI-assisted development.

Exit criteria:

- Documentation exists under `docs/`.
- ADRs are created for key technical choices.
- Initial module map is agreed.
- `docs/references/` exists for approved screen references, fallback source notes, and deviation logs.
- ADRs exist for financial accuracy, ledger-first architecture, automation philosophy, reference-driven UI, and feature flags.
- Engineering guidelines are documented and linked from the project documentation index.
- Definition of Done is documented and linked from the project documentation index.
- Architecture changelog is documented and linked from the project documentation index.

## Phase 1: Repository And Build Foundation

Objectives:

- Create Gradle Kotlin DSL monorepo.
- Configure Kotlin Multiplatform targets for Android, iOS, and Wear OS.
- Add Compose Multiplatform.
- Configure static analysis and formatting.
- Establish test tasks.

Deliverables:

- `settings.gradle.kts`
- Root `build.gradle.kts`
- Version catalog.
- Initial `apps/*` and `shared/*` modules.
- CI-ready Gradle tasks.

## Phase 2: Core Domain And Finance Primitives

Objectives:

- Implement shared value objects.
- Implement domain entities and repository contracts.
- Define result/error model.
- Implement money and rate primitives.
- Implement ledger operation models and invariants.
- Implement feature flag primitives.

Deliverables:

- `shared/core`
- `shared/domain`
- `shared/application`
- Unit tests for value objects, validation, and application use cases.
- Ledger invariant tests.

## Phase 3: Loan Engine MVP

Objectives:

- Implement EMI calculation.
- Implement amortization schedule generation.
- Implement tenure-reduction prepayment simulation.
- Implement interest savings summary.

Deliverables:

- `shared/loan-engine`
- Loan calculation API.
- Comprehensive unit test suite.
- Golden sample schedules for regression tests.

## Phase 4: Database And Repository Layer

Objectives:

- Add SQLDelight.
- Create schemas and migrations.
- Implement repository adapters.
- Add local transaction boundaries.
- Persist immutable ledger entries.
- Implement balance projections from ledger entries.

Deliverables:

- `shared/database`
- `shared:data`
- Repository tests.
- Migration tests.
- Ledger reconciliation tests.

## Phase 5: App Shells And Shared UI

Objectives:

- Build Android app shell.
- Build Wear OS app shell.
- Build iOS app shell.
- Create shared design system and navigation patterns.
- Implement major screens only after approved reference sources, functional specs, navigation definitions, and acceptance checklists are available.

Deliverables:

- Usable Android Compose app.
- Wear quick capture and glance UI.
- iOS Compose/host integration.
- Koin platform setup.

## Phase 6: Finance Features

Status: Complete.

Objectives:

- Accounts.
- Transactions.
- Categories.
- Budgets.
- Loan creation and loan details.
- SMS-assisted transaction review flow.

Completed milestones:

- Accounts Screen v1: complete.
- Transaction Entry Integration v1: complete.
- Transactions List / History Screen v1: complete and locally validated.
- Categories Screen v1: complete and locally validated.
- Budgets Screen v1: complete and locally validated.
- Loan Creation and Loan Details v1: complete and locally validated through PR #13.
- SMS-Assisted Transaction Review Flow v1: complete and locally validated through issue #15 and PR #16.

Deliverables:

- Core personal finance workflows.
- Offline local persistence.
- Loan schedule screen.
- Budget tracking screen.
- Deterministic SMS parser MVP with explicit confirmation.
- Ledger-backed transaction, transfer, loan, and refund workflows.

## Phase 7: Analytics And Notifications

Spending Analytics / Reports Screen v1 is implemented and locally validated
through PR #20. Cash-flow Analytics v1 is implemented and CI-validated through
issue #26 and PR #27. Loan Payoff Analytics v1 is implemented, merged, and
CI-validated through issue #29 and PR #30. The shared Reports reference note
retains separate visual and accessibility review follow-up for the Reports and
Cash-flow milestones.

EMI and budget reminders v1 rules are approved through issue #41. The canonical
specification is `docs/emi-budget-reminders-v1.md`. The shared planner and
Application read-orchestration slice is implemented through issue #42 and PR
#47. Android permission, scheduling, cancellation, delivery-receipt, lifecycle,
and destination integration remains isolated under issue #43.

Objectives:

- Spending analytics.
- Cash-flow analytics.
- Loan payoff analytics.
- EMI and budget reminders.

Current sequence:

1. Spending Analytics / Reports Screen v1 — implementation complete through
   PR #20; visual and accessibility review follow-up remains documented.
2. Cash-flow Analytics v1 — deterministic per-currency daily/monthly buckets,
   Application mapping, accessible Reports rows, direct analytics CI coverage,
   and focused tests complete through PR #27; visual and accessibility review
   follow-up remains documented.
3. Loan Payoff Analytics v1 — deterministic read-only payoff metrics, an
   Application-owned DTO boundary, Bootstrap/Koin registration, an accessible
   Loan Details progress card, and focused Analytics, Application, Bootstrap,
   and UI tests complete through PR #30.
4. EMI and budget reminders v1 specification — approved conservative behavior,
   stable identities, timezone rules, read-only safety, platform boundaries,
   permission behavior, lifecycle reconciliation, and testing matrix tracked by
   issue #41 and `docs/emi-budget-reminders-v1.md`.
5. Shared reminder planner and Application orchestration — deterministic EMI
   and budget planning, stable identities, Application-owned DTOs, read-only
   repository orchestration, Bootstrap/Koin registration, and focused CI
   coverage complete through issue #42 and PR #47.
6. Android permission, settings, scheduling, cancellation, delivery receipts,
   lifecycle reconciliation, and destination integration — next separate slice
   under issue #43; platform code must consume shared plans without duplicating
   reminder business rules.

Deliverables:

- `shared/analytics`
- `shared/notifications`
- Platform notification adapters.

## Phase 8: Hardening And Release Preparation

Objectives:

- Performance profiling.
- Accessibility pass.
- Backup/export.
- Migration verification.
- Release packaging.

Deliverables:

- Release candidate builds.
- QA checklist.
- Privacy documentation.
- Store-ready metadata.

## Ongoing Engineering Practices

- Keep ADRs updated when decisions change.
- Require tests for financial calculation changes.
- Keep engines pure and deterministic.
- Prefer small module APIs over cross-module leakage.
- Review migrations carefully.
- Preserve familiar workflows unless a reference-backed product decision changes them.
- Keep experimental capabilities behind feature flags.
- Do not persist mutable balances that cannot be reconciled from ledger entries.
- Keep commits small and reviewable.
- Stop for clarification rather than inventing APIs, workflows, or business rules.
- Do not merge features until applicable Definition of Done items are complete.
- Update the architecture changelog for changes that affect project structure, data flow, module boundaries, or engineering practices.
