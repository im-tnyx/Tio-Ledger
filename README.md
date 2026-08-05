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

Current status on `main`: UI Foundation v1, Application Bootstrap v1, frozen Database Schema, frozen Ledger Engine v1, Application/Data layers, Dashboard Screen v1, Accounts Screen v1, Transaction Entry Integration v1, Transactions List / History Screen v1, Categories Screen v1, Budgets Screen v1, Loan Creation and Loan Details v1, SMS-Assisted Transaction Review Flow v1, Spending Analytics / Reports Screen v1, Cash-flow Analytics v1, and Loan Payoff Analytics v1 are implemented and CI-validated. Phase 6 Finance Features is complete, and Phase 7 is in progress.

Loan Payoff Analytics v1 is merged through issue #29 and PR #30. Primary bottom-navigation wiring for Dashboard, Accounts, Transactions, Categories, and Budgets is completed through issue #34 and PR #36; this records callback consistency only and does not imply a Dashboard content redesign. The current planned Phase 7 product milestone is EMI and budget reminders. The Reports reference note retains separate visual and accessibility review follow-up for the Reports and Cash-flow presentation; no completion of those review items is implied here. Production UI work must follow the reference-source policy documented in `docs/references/README.md`, including the JADX decompiled-reference restrictions.

## Roadmap

The implementation roadmap is maintained in [docs/implementation-roadmap.md](docs/implementation-roadmap.md).

Current engineering sequence:

1. Preserve frozen financial, persistence, and architecture invariants.
2. Keep completed Phase 6 finance workflows stable unless a verified defect is discovered.
3. Preserve the Spending Analytics / Reports, Cash-flow Analytics, and Loan Payoff Analytics boundaries, deterministic calculations, multi-currency separation, and immutable read contracts.
4. Preserve canonical primary bottom-navigation routing without inventing Dashboard content that lacks an approved specification.
5. Define and implement EMI and budget reminders from documented requirements, then continue to release hardening.

## Contributing

See [.github/CONTRIBUTING.md](.github/CONTRIBUTING.md).

All production changes must follow:

- [Engineering Guidelines](docs/engineering-guidelines.md)
- [Definition Of Done](docs/definition-of-done.md)
- [Architecture Decision Records](docs/adr/README.md)

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE).
