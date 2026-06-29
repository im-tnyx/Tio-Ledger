# ADR-0001: Native Gradle Kotlin Multiplatform Monorepo

## Status

Accepted

## Context

Tio Ledger targets Android, iOS, and Wear OS with shared business logic. The repository needs a build structure that fits Kotlin Multiplatform and Compose Multiplatform without introducing JavaScript-oriented monorepo tooling.

## Decision

Use a native Gradle Kotlin DSL monorepo. Do not use Turborepo.

The top-level structure is:

```text
apps/
shared/
docs/
```

Gradle settings will include application modules under `apps/*` and reusable Kotlin Multiplatform modules under `shared/*`.

## Consequences

Positive:

- Native fit for Kotlin Multiplatform.
- Direct access to Gradle metadata, targets, and source sets.
- Better alignment with Android and Compose tooling.
- Lower operational complexity than mixing unrelated build systems.

Negative:

- Gradle configuration must be kept disciplined as modules grow.
- Build performance requires convention plugins and dependency hygiene over time.
