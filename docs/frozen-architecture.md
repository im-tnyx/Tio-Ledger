# Frozen Architecture Registry

This document records the architectural layers that have been declared **Frozen v1** and defines the rules governing modification of frozen scopes to prevent scope creep.

## Frozen Layers Status

| Layer | Package / Module | Version | Status | Frozen Since |
| :--- | :--- | :--- | :--- | :--- |
| **Database Schema** | `shared:database` | v1.0.0 | **Frozen** | 2026-06-29 |
| **Ledger Engine** | `shared:finance-engine` | v1.0.0 | **Frozen** | 2026-06-29 |
| **Application Layer** | `shared:application` | v1.0.0 | **Frozen** | 2026-06-30 |
| **Data Layer** | `shared:data` | v1.0.0 | **Frozen** | 2026-06-30 |

## Scope Control & Freeze Rules

1. **Bug Fixes Only**: Modifying code in a frozen layer is strictly restricted to resolving logical correctness bugs or regressions discovered during testing.
2. **Feature Isolation**: No new business rules, domain strategies, queries, or repository boundaries may be introduced directly to a frozen layer. New features must be extended in higher-level layers or distinct modules.
3. **Migration Policy**: Database schema changes are prohibited unless a versioned SQLite migration script is defined, accompanied by architectural review approval.
4. **Clean Decoupling**: Ensure database concepts (e.g. SQLDelight classes, exceptions) never bypass the repository boundary into domain modules.
