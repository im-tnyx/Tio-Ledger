# ADR-0005: Shared Pure Financial Engines

## Status

Accepted

## Context

Loan, budget, and finance calculations must behave consistently across Android, iOS, and Wear OS. These calculations are high-trust product areas and should be easy to test.

## Decision

Implement financial engines as shared pure Kotlin modules.

Engines must not depend on UI, SQLDelight, platform APIs, or repositories. They accept explicit input models and return explicit output models.

## Consequences

Positive:

- Consistent behavior across platforms.
- Fast unit tests.
- Easy regression testing with golden schedules.
- Clear ownership for financial logic.

Negative:

- Repositories and use cases must perform mapping between persisted records and engine input models.
- Engine APIs need careful versioning as capabilities grow.
