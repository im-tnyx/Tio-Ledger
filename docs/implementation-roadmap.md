# Implementation Roadmap

## Current Milestone Status

- Ledger Engine: Frozen v1 as of 2026-06-30.
- Approved architectural blockers for commonMain compatibility, type-safe posting strategies, and strategy registry separation are resolved.
- Application Layer and Repository Contracts milestone is implemented as pure Kotlin Multiplatform shared code.
- Next milestone: SQLDelight Repository Implementations.

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
- `shared/data`
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

Objectives:

- Accounts.
- Transactions.
- Categories.
- Budgets.
- Loan creation and loan details.
- SMS-assisted transaction review flow.

Deliverables:

- Core personal finance workflows.
- Offline local persistence.
- Loan schedule screen.
- Budget tracking screen.
- Deterministic SMS parser MVP with explicit confirmation.
- Ledger-backed transaction, transfer, loan, and refund workflows.

## Phase 7: Analytics And Notifications

Objectives:

- Spending analytics.
- Cash-flow analytics.
- Loan payoff analytics.
- EMI and budget reminders.

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
