# ADR-0009: Deterministic Money And Interest Precision

## Status

Accepted

## Context

Personal finance and loan calculations require deterministic results. Floating-point drift would undermine user trust, especially for EMI schedules, principal versus interest tracking, prepayments, and interest savings.

## Decision

Store money as integer minor units with an explicit currency code. Do not persist monetary values as floating-point numbers.

Interest calculations must use a deterministic decimal strategy isolated inside shared finance primitives. Rounding policy must be explicit at calculation boundaries and tested with golden cases.

## Consequences

Positive:

- Predictable persisted values.
- Reproducible schedules across platforms.
- Easier auditability and regression testing.
- Safer loan and budget calculations.

Negative:

- More careful implementation than using `Double`.
- Currency-specific minor unit handling must be modeled explicitly.
- Interest rate calculations need dedicated helpers.
