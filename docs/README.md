# Tio Ledger Documentation

Tio Ledger is a Kotlin Multiplatform personal finance product designed for Android, iOS, and Wear OS. The application is offline-first, modular, and built around shared business logic so that financial calculations, persistence rules, and product behavior remain consistent across every client.

## Product Vision

Build a modern personal finance application inspired by Money Manager, with stronger foundations for long-term maintainability and a more capable Loan & EMI engine. The product should help users understand daily cash flow, budgets, debt obligations, prepayment opportunities, and financial trends without requiring continuous network access.

The product should remain functionally familiar to Money Manager. The goal is to modernize the experience while preserving the speed, simplicity, accounting-first information architecture, and recognizable workflows existing users already understand.

## Documentation Map

- [Product Requirements](product-requirements.md)
- [Architecture Overview](architecture.md)
- [Architecture Changelog](architecture-changelog.md)
- [Module Design](module-design.md)
- [Offline-First Data Strategy](offline-first-data-strategy.md)
- [Loan Engine Design](loan-engine-design.md)
- [SMS Assisted Transaction Capture](sms-assisted-transaction-capture.md)
- [Engineering Guidelines](engineering-guidelines.md)
- [Definition Of Done](definition-of-done.md)
- [Implementation Roadmap](implementation-roadmap.md)
- [Testing Strategy](testing-strategy.md)
- [Reference Screenshots](references/README.md)
- [ADRs](adr/README.md)

## Status Reports

- [Financial Correctness Validation Report](status/FINANCIAL_CORRECTNESS_VALIDATION_REPORT.md)
- [Architecture Validation Report](status/ARCHITECTURE_VALIDATION_REPORT.md)
- [Build Verification Report](status/BUILD_VERIFICATION_REPORT.md)
- [Application Layer Architecture Validation Report](status/APPLICATION_LAYER_ARCHITECTURE_VALIDATION_REPORT.md)
- [Application Layer Build Verification Report](status/APPLICATION_LAYER_BUILD_VERIFICATION_REPORT.md)
- [Application Bootstrap Validation Report](status/APPLICATION_BOOTSTRAP_VALIDATION_REPORT.md)
- [DI Validation Report](status/DI_VALIDATION_REPORT.md)
- [Navigation Validation Report](status/NAVIGATION_VALIDATION_REPORT.md)
- [UI Foundation Validation Report](status/UI_FOUNDATION_VALIDATION_REPORT.md)
- [Component Catalog Report](status/COMPONENT_CATALOG_REPORT.md)

## Target Repository Shape

```text
apps/
  android/
  wear/
  ios/

shared/
  core/
  domain/
  application/
  bootstrap/
  data/
  database/
  finance-engine/
  loan-engine/
  budget-engine/
  analytics/
  notifications/
  ui/

docs/
```

## Architecture Principles

1. Shared business logic first: finance rules, loan calculations, validation, and use cases belong in shared Kotlin modules.
2. Offline-first by default: local persistence is the source of truth; sync can be added without changing core domain models.
3. Low coupling, high cohesion: modules should expose narrow APIs and avoid leaking storage, UI, or platform details.
4. Clean Architecture with MVVM: UI observes state from ViewModels, ViewModels execute use cases, use cases depend on repositories, repositories coordinate local data sources and engines.
5. Deterministic financial math: money, interest, and schedule calculations must avoid floating-point drift.
6. Enterprise-grade maintainability: clear module ownership, ADRs for durable decisions, testable engines, and documented extension points.
7. Familiar UX modernization: preserve Money Manager-style workflows unless an approved reference explicitly changes them.
8. Human-confirmed automation: assisted capture can pre-fill transactions, but users must explicitly review and save.
9. Ledger-first accounting: every financial operation creates ledger entries, and balances are derived from the ledger.
10. Feature-flagged evolution: experimental capabilities must not destabilize core accounting workflows.
11. Reviewable engineering: production code should be readable, focused, tested, and aligned with approved ADRs.
12. Done means verified: features are complete only when the Definition of Done is satisfied.

## Immediate Priorities

1. Keep frozen layers stable: Database Schema, Ledger Engine, Application Layer, and Data Layer.
2. Use Application Bootstrap v1 as the app startup, DI, navigation, and design-system foundation.
3. Use UI Foundation v1 for reusable design tokens, stateless components, templates, and navigation placeholders.
4. Prepare Accounts Screen (Reference Implementation) from approved references before adding production account workflows.
