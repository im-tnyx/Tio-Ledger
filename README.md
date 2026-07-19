# Tio Ledger

Tio Ledger is a Kotlin Multiplatform personal finance application planned for Android, iOS, and Wear OS. It is inspired by Money Manager, but designed with a modern offline-first architecture, ledger-first accounting model, and a powerful shared Loan & EMI engine.

Documentation v1.0 is the current baseline for all future implementation work.

## Project Vision

Build a fast, familiar, accounting-first personal finance app that preserves the workflows users already understand while improving architecture, maintainability, financial accuracy, accessibility, and cross-platform consistency.

## Why Tio Ledger

- Ledger-first accounting instead of mutable balance tracking.
- Deterministic financial calculations.
- Shared business logic across Android, iOS, and Wear OS.
- Offline-first local source of truth.
- Reference-driven UI development based on approved Money Manager-style workflows.
- Human-confirmed automation for SMS-assisted transaction capture and future smart suggestions.

## Feature Highlights

Planned capabilities include:

- Accounts, transactions, categories, and budgets.
- Loan and EMI schedules.
- Principal versus interest tracking.
- Prepayment simulation and interest savings.
- Amortization schedules.
- SMS-assisted transaction capture with explicit user confirmation.
- Analytics, notifications, and Wear OS quick workflows.

## Platform Support

Planned targets:

- Android
- iOS
- Wear OS

Business logic belongs in shared Kotlin Multiplatform modules wherever possible.

## Screenshots

Screenshots will be added after approved UI references and production UI implementation exist.

Production UI must follow the reference-source policy documented in [docs/references/README.md](docs/references/README.md).

## Architecture Overview

Tio Ledger uses:

- Kotlin Multiplatform.
- Compose Multiplatform.
- Clean Architecture.
- MVVM.
- Repository pattern.
- SQLDelight as primary persistence.
- Koin for dependency injection.
- kotlinx.serialization for stable contracts.
- Feature flags for experimental capabilities.

See [docs/architecture.md](docs/architecture.md) for the canonical architecture.

## Documentation Index

- [Documentation Home](docs/README.md)
- [Product Requirements](docs/product-requirements.md)
- [Architecture Overview](docs/architecture.md)
- [Architecture Changelog](docs/architecture-changelog.md)
- [Module Design](docs/module-design.md)
- [Offline-First Data Strategy](docs/offline-first-data-strategy.md)
- [Loan Engine Design](docs/loan-engine-design.md)
- [SMS Assisted Transaction Capture](docs/sms-assisted-transaction-capture.md)
- [Engineering Guidelines](docs/engineering-guidelines.md)
- [Definition Of Done](docs/definition-of-done.md)
- [Implementation Roadmap](docs/implementation-roadmap.md)
- [Testing Strategy](docs/testing-strategy.md)
- [UI Reference Sources](docs/references/README.md)
- [Architecture Decision Records](docs/adr/README.md)

## Development Status

Current status: UI Foundation v1, Application Bootstrap v1, frozen Database Schema, frozen Ledger Engine v1, Application/Data layers, Dashboard Screen v1, Accounts Screen v1, Transaction Entry Integration v1, and Transactions List / History Screen v1 are implemented and locally validated.

The next planned product milestone is Categories Screen v1. Production UI work must follow the reference-source policy documented in `docs/references/README.md`, including the JADX decompiled-reference restrictions.

## Roadmap

The implementation roadmap is maintained in [docs/implementation-roadmap.md](docs/implementation-roadmap.md).

Current engineering sequence:

1. Preserve frozen financial, persistence, and architecture invariants.
2. Complete core finance workflows in Phase 6.
3. Implement Categories Screen v1 from approved references.
4. Continue with budgets, loans, and SMS-assisted transaction review.
5. Add analytics, notifications, and release hardening after core workflows are complete.

## Contributing

See [.github/CONTRIBUTING.md](.github/CONTRIBUTING.md).

All production changes must follow:

- [Engineering Guidelines](docs/engineering-guidelines.md)
- [Definition Of Done](docs/definition-of-done.md)
- [Architecture Decision Records](docs/adr/README.md)

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE).
