# Module Design

## apps/android

Android phone and tablet shell.

Responsibilities:

- Android application entry point.
- Compose navigation host.
- Android-specific permissions and notification registration.
- Android reminder scheduling, cancellation, delivery receipts, and boot/time-zone reconciliation.
- Android SMS permission and import surfaces where approved by product policy.
- Koin startup for Android.
- Platform-specific theming where needed.

Depends on:

- `shared:bootstrap`
- `shared:ui`
- `shared:data`
- `shared:database`
- `shared:notifications`

## apps/wear

Wear OS shell for glanceable and quick-entry workflows.

Responsibilities:

- Wear Compose entry point.
- Quick transaction capture.
- Balance and upcoming EMI tiles.
- Wear notification actions.

Depends on:

- `shared:bootstrap`
- `shared:ui` where reusable.
- `shared:domain`
- `shared:data`
- `shared:notifications`

## apps/ios

iOS application shell.

Responsibilities:

- iOS entry point.
- Compose Multiplatform or SwiftUI host integration.
- iOS notification registration.
- iOS-specific alternatives for SMS-assisted capture when direct SMS access is unavailable, such as manual paste or share/import flows.
- Koin startup for iOS.

Depends on:

- Shared KMP framework exported from selected shared modules.

## shared/core

Foundation primitives.

Responsibilities:

- Money value types.
- Date/time abstractions.
- IDs.
- Result and error primitives.
- Common validation helpers.
- Serialization helpers.

Should not depend on feature modules.

## shared/domain

Business language and contracts.

Responsibilities:

- Entities: Account, Transaction, Category, Budget, Loan.
- Ledger entities and operation models.
- Value objects: Money, InterestRate, Tenure, PaymentFrequency.
- Repository interfaces only; no database implementation.
- Lightweight domain events.
- Domain errors.
- Transaction suggestion and confirmation models.

Depends on:

- `shared:core`

## shared/application

Pure Kotlin application orchestration.

Responsibilities:

- Use cases for account, category, transaction, budget, loan, analytics, and reminder workflows.
- Input validation at application boundaries.
- Ledger Engine dispatch for posting financial transactions.
- Typed application results and errors.
- Domain event emission for successful business operations.
- Repository contract consumption without knowing persistence details.
- Read-only reminder candidate orchestration and mapping to Application-owned immutable DTOs.

Must not contain:

- SQL queries.
- SQLDelight implementations.
- UI, ViewModel, Android, iOS, or Wear OS dependencies.
- Platform notification scheduling, permission, or delivery code.

Depends on:

- `shared:core`
- `shared:domain`
- `shared:finance-engine`
- Feature engines and pure shared contracts required by individual use cases.

## shared/database

SQLDelight schema and database access.

Responsibilities:

- `.sq` schema files.
- SQLDelight generated queries.
- Migrations.
- Database driver factories through expect/actual.
- Low-level transaction helpers.

Depends on:

- `shared:core` only when needed for adapters.

## shared/bootstrap

Application startup and dependency injection composition.

Responsibilities:

- Koin module assembly for existing Core, Database, Data, Application, Finance Engine, analytics, and notification implementations.
- SQLDelight database initialization through platform driver factories.
- Startup diagnostics and bootstrap logging.
- Platform-neutral application startup wiring.

Must not contain:

- Business features.
- Financial calculations.
- Reminder rule evaluation.
- SQL schema definitions.
- Repository implementations.
- Production UI screens or navigation workflows.

Depends on:

- `shared:core`
- `shared:database`
- `shared:data`
- `shared:application`
- `shared:domain`
- `shared:finance-engine`
- Pure feature modules required for dependency composition.

## shared/data

Repository implementations.

Responsibilities:

- Implements domain repository interfaces.
- Maps database rows to domain models.
- Coordinates local writes and reads.
- Provides data source abstractions for future sync.
- Persists confirmed transactions only; SMS-derived suggestions are not transactions until confirmed.
- Writes financial operations as ledger entries.
- Computes balances from ledger entries or verified ledger projections.

Depends on:

- `shared:domain`
- `shared:database`
- Engine modules when repository operations require calculations.

## shared/finance-engine

General finance calculation primitives.

Responsibilities:

- Money arithmetic policies.
- Cash flow summaries.
- Balance projections.
- Rate conversion helpers.
- Date schedule utilities.
- Ledger balance projection helpers.

Depends on:

- `shared:core`

## shared/loan-engine

Loan and EMI calculation module.

Responsibilities:

- EMI calculation.
- Amortization schedule generation.
- Principal and interest split.
- Prepayment simulation.
- Interest savings calculation.
- Tenure reduction calculation.
- Revised repayment schedule generation.

Depends on:

- `shared:core`
- `shared:finance-engine`

## shared/budget-engine

Budget calculation module.

Responsibilities:

- Budget period calculations.
- Spend aggregation.
- Remaining budget projection.
- Budget warning thresholds.

Depends on:

- `shared:core`
- `shared:domain`

## shared/analytics

Derived insights.

Responsibilities:

- Trend aggregation.
- Category spend summaries.
- Cash-flow analytics.
- Debt progress analytics.

Depends on:

- `shared:domain`
- `shared:finance-engine`
- `shared:loan-engine`

## shared/notifications

Pure shared notification contracts and deterministic reminder planning.

Responsibilities:

- Immutable reminder identities, types, semantic destinations, and content contracts.
- Timezone-explicit EMI lead-day and due-day reminder planning from persisted schedule candidates.
- Budget warning/reached/exceeded transition planning and stable-identity suppression.
- Deterministic ordering and typed validation failures.
- Platform-neutral preference snapshots and planning context.

Must not contain:

- Android, iOS, or Wear OS APIs.
- Notification permission handling.
- Alarm, worker, or platform scheduler implementations.
- Delivery-receipt persistence.
- Repository access or financial writes.
- Duplicate loan, EMI, or budget calculations.

Depends on:

- `shared:core`
- `shared:domain`
- `shared:budget-engine`
- `shared:loan-engine`

## SMS-Assisted Capture Package

The initial project structure does not require a separate top-level SMS module. SMS-assisted capture should begin as cohesive packages inside existing modules:

- `shared/domain`: suggestion, confidence, and confirmation models.
- `shared/data`: repository coordination for confirmed saves.
- `shared/ui`: editable review screens.
- `apps/android`: SMS permission, receiver/import integration, and platform policy handling.
- `apps/ios`: permitted import alternatives.

If parser complexity grows, promote it later to a dedicated shared module through a new ADR.

## Feature Flags Package

Feature flags should begin in `shared/core` or `shared/domain` as simple typed capabilities:

- Stable production defaults.
- Explicit experimental flags.
- Platform override support at composition root boundaries.

If rollout logic becomes remote or complex, promote it to a dedicated module through a new ADR.

## shared/ui

Reusable presentation layer.

Responsibilities:

- Compose Multiplatform components.
- Shared screens where practical.
- ViewModels and UI state models.
- Design system tokens.
- Stateless reusable components and templates.
- Formatting utilities that are presentation-specific.
- Feature flag-aware presentation of experimental surfaces.

Depends on:

- Compose Multiplatform libraries.
- `shared:bootstrap` only for app shell diagnostics integration.

Must not contain:

- Repository implementations.
- SQL access.
- Ledger calculations.
- Production screen workflows without approved references.

## Coupling Rules

- Feature engines expose pure APIs and do not know about persistence.
- Database rows do not escape the data layer.
- UI state models do not become domain entities.
- Platform apps own platform lifecycle, permissions, scheduling, and notification delivery.
- Shared domain owns business language.
- Shared reminder plans remain semantic and platform-neutral; platform adapters consume them without re-evaluating business rules.
