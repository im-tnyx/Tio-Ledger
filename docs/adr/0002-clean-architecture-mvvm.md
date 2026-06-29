# ADR-0002: Clean Architecture With MVVM

## Status

Accepted

## Context

The product will contain durable finance logic, multiple clients, and persistence that may evolve from local-only to sync-capable. UI code must not own business rules.

## Decision

Use Clean Architecture with MVVM for presentation.

- UI observes immutable state from ViewModels.
- ViewModels call use cases.
- Use cases coordinate repositories and engines.
- Repositories abstract data sources.
- Domain models and engine APIs remain platform-independent.

## Consequences

Positive:

- Business logic is testable and shared.
- Platform apps stay thin.
- Persistence can evolve without rewriting UI.
- Complex finance behavior has clear ownership.

Negative:

- More modules and interfaces than a small single-platform app.
- Requires discipline to avoid anemic pass-through layers.
