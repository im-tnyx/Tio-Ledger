# ADR-0011: Human-Confirmed SMS Assisted Capture

## Status

Accepted

## Context

SMS alerts can reduce transaction entry friction, especially for bank, credit card, UPI, and wallet transactions. However, automatic transaction creation can introduce incorrect records, privacy concerns, and user trust issues.

## Decision

Use SMS-assisted transaction capture only as a pre-fill and review workflow. Never automatically create transactions from SMS.

Parsing should be deterministic, privacy-friendly, and offline whenever possible. The user must explicitly confirm before any SMS-derived suggestion becomes a transaction.

## Consequences

Positive:

- Preserves user control.
- Reduces incorrect financial records.
- Keeps the workflow compatible with accounting-first expectations.
- Supports privacy-friendly offline parsing.

Negative:

- Requires one extra user action compared with full automation.
- Parser confidence and review UX need careful design.
- Platform SMS capabilities differ and must be handled per platform.
