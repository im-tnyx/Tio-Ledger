# ADR-0008: kotlinx.serialization For Stable Contracts

## Status

Accepted

## Context

The app needs stable models for import/export, backups, future sync payloads, engine requests, and engine results. These contracts should work consistently across Kotlin Multiplatform targets.

## Decision

Use kotlinx.serialization for shared serialization contracts.

Serialization models should be explicit and version-aware. Domain models may be serializable where appropriate, but persistence rows and API/export DTOs should not be conflated automatically with domain entities.

## Consequences

Positive:

- Kotlin Multiplatform support.
- Compile-time serialization support.
- Useful for JSON backup/export and future sync.
- Clear path for versioned data contracts.

Negative:

- Requires care when evolving serialized models.
- Domain models and DTOs may require mapping to preserve clean boundaries.
