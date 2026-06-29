# ADR-0004: Koin For Dependency Injection

## Status

Accepted

## Context

The app needs dependency injection across shared Kotlin modules and platform applications. The solution should work in Kotlin Multiplatform and avoid heavy code generation constraints.

## Decision

Use Koin for dependency injection.

Shared modules will expose Koin modules for core services, repositories, use cases, and engines. Platform apps will provide platform-specific bindings such as database drivers and notification schedulers.

## Consequences

Positive:

- Kotlin Multiplatform support.
- Simple module declarations.
- Easy platform-specific bindings.
- Suitable for incremental adoption.

Negative:

- Runtime DI errors are possible if modules are misconfigured.
- Requires tests or startup checks to catch missing bindings.
