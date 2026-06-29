# ADR-0012: Financial Accuracy

## Status

Accepted

## Context

Tio Ledger manages personal financial records and loan calculations. Users must be able to trust balances, interest calculations, amortization schedules, and historical records.

## Decision

All monetary calculations must be deterministic.

Requirements:

- Never use `Float` or `Double` for money.
- Use decimal, BigDecimal-style, or fixed-precision money types.
- Store money as minor units where practical.
- Make rounding policy explicit.
- Make all interest calculations reproducible across supported platforms.
- Keep historical transactions immutable.
- Ensure every balance is derivable from the transaction ledger.

## Consequences

Positive:

- Reproducible calculations across Android, iOS, and Wear OS.
- Stronger user trust.
- Easier audit and reconciliation.
- Safer loan and EMI behavior.

Negative:

- Requires deliberate money and interest primitives.
- Some calculations are more verbose than floating-point arithmetic.
- Migration and import flows must normalize precision carefully.
