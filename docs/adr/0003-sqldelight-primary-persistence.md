# ADR-0003: SQLDelight As Primary Persistence

## Status

Accepted

## Context

Tio Ledger is offline-first and cross-platform. The database layer must work on Android and iOS while keeping queries explicit and migrations manageable.

## Decision

Use SQLDelight as the primary persistence technology.

Room may be used only for isolated Android-specific needs that do not need shared persistence.

## Consequences

Positive:

- Kotlin Multiplatform-compatible persistence.
- Type-safe SQL.
- Explicit migrations.
- Strong fit for local-first financial records.

Negative:

- Requires writing SQL directly.
- Some Android developers may be more familiar with Room.
- Native driver setup must be handled carefully.
