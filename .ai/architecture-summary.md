# Architecture Summary

Tio Ledger uses a native Gradle Kotlin Multiplatform monorepo with shared business logic.

Approved architecture:

- Kotlin Multiplatform.
- Compose Multiplatform.
- Clean Architecture.
- MVVM.
- Repository pattern.
- SQLDelight as primary persistence.
- Koin for dependency injection.
- kotlinx.serialization for stable contracts.
- Offline-first local source of truth.
- Ledger-first accounting.
- Feature flags for experimental capabilities.

Do not introduce Turborepo.

Canonical references:

- [../docs/architecture.md](../docs/architecture.md)
- [../docs/module-design.md](../docs/module-design.md)
- [../docs/offline-first-data-strategy.md](../docs/offline-first-data-strategy.md)
- [../docs/adr/README.md](../docs/adr/README.md)
