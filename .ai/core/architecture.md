# Architecture Summary

Tio Ledger is a native Gradle Kotlin Multiplatform monorepo with shared
business logic.

Approved architecture:

- Kotlin Multiplatform and Compose Multiplatform.
- Clean Architecture with MVVM and repository boundaries.
- SQLDelight as primary offline-first persistence.
- Koin for dependency injection.
- kotlinx.serialization for stable contracts.
- Ledger-first accounting with immutable financial history.
- Shared deterministic engines for finance, loans, budgets, and analytics.
- `shared:bootstrap` owns startup assembly and diagnostics, not business logic.

Dependencies flow inward. UI calls Application use cases; use cases depend on
Domain contracts; Data coordinates persistence; engines remain pure and do not
depend on repositories or UI.

Canonical references:

- [Architecture](../../docs/architecture.md)
- [Module Design](../../docs/module-design.md)
- [Offline-First Data Strategy](../../docs/offline-first-data-strategy.md)
- [ADRs](../../docs/adr/README.md)
