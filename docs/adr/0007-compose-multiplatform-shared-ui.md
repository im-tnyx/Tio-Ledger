# ADR-0007: Compose Multiplatform Shared UI

## Status

Accepted

## Context

Tio Ledger targets Android, iOS, and Wear OS. The product should share presentation patterns where practical while still allowing platform shells to handle navigation, lifecycle, permissions, and native integration.

## Decision

Use Compose Multiplatform for shared UI components and screens where the experience benefits from reuse.

Platform apps remain responsible for app startup, platform lifecycle, notification permissions, and platform-specific navigation integration. Wear OS may use more specialized UI surfaces because watch interactions are meaningfully different.

## Consequences

Positive:

- Shared design system and reusable screens.
- Consistent product behavior across platforms.
- Lower duplication for finance workflows.
- Strong fit with Kotlin Multiplatform modules.

Negative:

- Some platform-specific UX still needs dedicated code.
- iOS integration must be validated carefully.
- Wear OS should not inherit phone-first layouts by default.
