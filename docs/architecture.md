# Architecture Overview

## Architectural Style

Tio Ledger uses a native Gradle Kotlin Multiplatform monorepo with Clean Architecture, MVVM, repository pattern, SQLDelight persistence, kotlinx.serialization, and Koin dependency injection.

The architecture must support Money Manager-style workflows without cloning implementation details blindly. Product behavior should remain familiar; modernization belongs in presentation quality, accessibility, responsiveness, and architecture.

Tio Ledger is ledger-first. Balances are derived from immutable ledger entries rather than maintained as independent mutable state.

```text
Compose UI / Platform UI
        |
ViewModel state and intents
        |
Use cases
        |
Domain models and engine APIs
        |
Repositories
        |
Local data sources / SQLDelight / platform services
```

## Layers

### Presentation

Owned by `apps/*` and `shared/ui`.

- Compose Multiplatform screens and reusable UI components.
- MVVM state holders.
- Platform-specific navigation, lifecycle, permissions, and notification hooks.
- No direct SQLDelight access.
- No financial formulas embedded in UI.
- Major screens must be built from approved screenshots in `docs/references/`.
- UI changes that alter familiar Money Manager-style workflows require an approved reference update.

### Domain

Owned by `shared/domain`.

- Core entities and value objects.
- Repository interfaces.
- Use case contracts.
- Domain validation.
- Ledger operation models and invariants.
- No dependency on SQLDelight, Compose, Android, iOS, or Wear APIs.

### Engines

Owned by `shared/finance-engine`, `shared/loan-engine`, and `shared/budget-engine`.

- Deterministic calculations.
- Pure Kotlin where possible.
- No database dependency.
- Inputs and outputs are explicit serializable models.
- Heavy unit and property-style test coverage.

### Data

Owned by `shared/data` and `shared/database`.

- Repository implementations.
- SQLDelight queries and migrations.
- DTO/entity mapping.
- Local-first synchronization abstraction for future cloud sync.
- Transaction boundaries and data integrity rules.
- Append-only ledger writes and balance projections.

### Platform Services

Owned by `apps/*` and selected expect/actual implementations in shared modules.

- Notification scheduling.
- File import/export.
- Secure storage where required.
- Platform clocks if needed.
- App lifecycle integration.
- SMS access and message ingestion where a platform permits it.

## Dependency Direction

Dependencies flow inward:

```text
apps -> shared/ui -> shared/domain
apps -> shared/data -> shared/domain
shared/data -> shared/database
shared/data -> engines
engines -> shared/core
shared/domain -> shared/core
```

Domain must not depend on data, database, or UI modules. Engines must not depend on repositories. UI must call use cases rather than persistence directly.

Production code must also follow the [Engineering Guidelines](engineering-guidelines.md). Those guidelines are binding for module boundaries, financial logic, UI implementation, testing, and AI-assisted development.

## Ledger-First Accounting

Every financial operation creates one or more ledger entries:

- Expense.
- Income.
- Transfer.
- Loan disbursement.
- EMI payment.
- Interest posting.
- Investment.
- Refund.

Balances are projections over ledger entries. Stored balance snapshots may be used later as performance optimizations, but they must be reproducible and reconcilable from the ledger.

Historical transactions are immutable. Corrections should be represented by reversal, adjustment, or replacement workflows that preserve auditability.

## State Management

The app uses MVVM with unidirectional state updates:

1. UI sends user intents.
2. ViewModel validates UI-level input and calls use cases.
3. Use cases coordinate repositories and engines.
4. Repositories read/write local storage.
5. ViewModel exposes immutable screen state.

SMS-assisted transaction capture follows the same flow:

1. Platform layer receives or imports candidate message text.
2. Shared parser produces deterministic candidates and confidence signals.
3. ViewModel exposes an editable review state.
4. User confirms or rejects the suggestion.
5. A normal transaction use case saves only after explicit confirmation.

For Kotlin Multiplatform compatibility, shared ViewModels should avoid Android-only APIs. Lifecycle integration can be adapted per platform.

## Error Handling

- Use typed domain errors for expected business failures.
- Use exceptions only for unexpected infrastructure failures.
- Keep user-facing messages in presentation resources.
- Include calculation warnings where financial assumptions matter, such as approximate monthly interest conversion.

## Money And Precision

Financial calculations must not use `Double` for persisted money. Use integer minor units for stored money and fixed-scale decimal/rational helpers for interest calculations. Engine outputs should make rounding rules explicit.

`Float` and `Double` are prohibited for money. Interest calculations must be deterministic and reproducible across supported platforms.

## Sync Readiness

The initial product is offline-first and local-only. Data models should still be sync-ready:

- Stable UUID identifiers.
- Created and updated timestamps.
- Soft-delete tombstone support where needed.
- Versioned schema migrations.
- Repository APIs that do not assume local-only forever.

## UX Governance

The product uses reference-driven UI implementation:

- `docs/references/` stores approved screenshots, fallback references, approval notes, and deviation logs for major screens.
- Every major screen should have an approved reference source before production UI implementation.
- Reference source priority is local approved screenshots, official Money Manager website, official Play Store listing and screenshots, then approved Tio Ledger mockups.
- Every production UI screen requires a reference source, functional specification, navigation definition, and acceptance checklist.
- The first implementation target is functional familiarity, then modern polish.
- Changes to information architecture must be treated as product decisions, not incidental UI refactors.
- AI should never invent major workflows or screen structures when an official reference source is available.

## Feature Flags

Experimental capabilities must be isolated behind feature flags so they cannot destabilize core accounting behavior.

Initial feature-flag candidates:

- AI Insights.
- OCR Receipt Scanner.
- SMS Parser.
- Cloud Sync.
- Family Accounts.
- Investment Tracking.
- Wear Tiles.
- Predictive Budgeting.

Feature flags should be evaluated in shared code when they affect shared business behavior, with platform-specific exposure controlled by the app shell.
