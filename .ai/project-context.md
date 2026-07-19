# Project Context

Tio Ledger is a Kotlin Multiplatform personal finance application for Android, iOS, and Wear OS.

The product is inspired by Money Manager and should remain functionally familiar while improving architecture, accessibility, responsiveness, and maintainability.

Current status:

- Documentation v1.0 baseline exists and remains the product/architecture reference.
- Gradle Kotlin Multiplatform repository foundation exists.
- Shared modules exist for core, domain, application, data, database, finance engine, loan engine, budget engine, analytics, notifications, and UI.
- App modules exist for Android, iOS, and Wear OS shells.
- Financial and ledger behavior must remain deterministic and test-backed whenever changed.
- Ledger Engine v1, database schema, Application/Data layers, and shared UI foundation are frozen or implemented.
- Dashboard Screen v1, Accounts Screen v1, Transaction Entry Integration v1, Transactions List / History Screen v1, Categories Screen v1, and Budgets Screen v1 are implemented and locally validated.
- The next planned milestone is Loan Creation and Loan Details v1.

Canonical references:

- [../README.md](../README.md)
- [../docs/README.md](../docs/README.md)
- [../docs/product-requirements.md](../docs/product-requirements.md)
- [../docs/implementation-roadmap.md](../docs/implementation-roadmap.md)
- [../docs/engineering-guidelines.md](../docs/engineering-guidelines.md)
- [../docs/definition-of-done.md](../docs/definition-of-done.md)
