# ADR-0006: Offline-First Local Source Of Truth

## Status

Accepted

## Context

Personal finance users need reliable access to records even without network connectivity. The app may support cloud sync later, but the initial experience should not require it.

## Decision

Use an offline-first architecture with the local database as the source of truth.

All critical user workflows must work offline. Future sync should be added behind repository and data source abstractions.

## Consequences

Positive:

- Fast app experience.
- Strong privacy posture.
- Reliable core workflows.
- Future sync can be introduced incrementally.

Negative:

- Conflict resolution must be designed when sync is added.
- Backup and restore become important product responsibilities.
