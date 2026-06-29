# ADR-0013: Ledger First Architecture

## Status

Accepted

## Context

The application should be a ledger, not just an expense tracker. Expense tracker architectures often store mutable balances or isolated transaction records without enough auditability for loans, transfers, refunds, and interest postings.

## Decision

Every financial operation must create one or more ledger entries.

Examples:

- Expense.
- Income.
- Transfer.
- Loan disbursement.
- EMI payment.
- Interest posting.
- Investment.
- Refund.

Balances are computed from ledger entries rather than stored independently.

Historical ledger entries are immutable. Corrections must be represented through reversal, adjustment, or replacement workflows that preserve the audit trail.

## Consequences

Positive:

- Account balances are explainable.
- Loans, transfers, refunds, and interest postings share a common accounting foundation.
- Data integrity improves.
- Future analytics and reconciliation become more reliable.

Negative:

- Write flows need careful multi-entry transaction handling.
- UI must hide accounting complexity without weakening the model.
- Balance projection queries and indexes must be designed for performance.
