# Module Design

## apps/android

Android phone and tablet shell.

Responsibilities:

- Android application entry point.
- Compose navigation host.
- Android-specific permissions and notification registration.
- Android SMS permission and import surfaces where approved by product policy.
- Koin startup for Android.
- Platform-specific theming where needed.

Depends on:

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

- Use cases for account, category, and transaction workflows.
- Input validation at application boundaries.
- Ledger Engine dispatch for posting financial transactions.
- Typed application results and errors.
- Domain event emission for successful business operations.
- Repository contract consumption without knowing persistence details.

Must not contain:

- SQL queries.
- SQLDelight implementations.
- UI, ViewModel, Android, iOS, or Wear OS dependencies.

Depends on:

- `shared:core`
- `shared:domain`
- `shared:finance-engine`

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

Shared notification rules and platform adapters.

Responsibilities:

- Notification intent models.
- Reminder rule evaluation.
- expect/actual or interface adapters for platform scheduling.
- EMI and budget notification planning.

Depends on:

- `shared:domain`
- `shared:loan-engine`
- `shared:budget-engine`

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
- Formatting utilities that are presentation-specific.
- Feature flag-aware presentation of experimental surfaces.

Depends on:

- `shared:domain`
- `shared:data` through injected use cases or repositories only at composition root boundaries.

## Coupling Rules

- Feature engines expose pure APIs and do not know about persistence.
- Database rows do not escape the data layer.
- UI state models do not become domain entities.
- Platform apps own platform lifecycle and permissions.
- Shared domain owns business language.
