# Testing Strategy

## Testing Priorities

The highest-risk areas are financial calculations, data integrity, migrations, and cross-platform consistency. Tests should be strongest around shared engines and repositories because those determine whether users can trust the app.

Ledger integrity is a release-blocking concern. Every balance must be derivable from ledger entries.

## Test Pyramid

### Unit Tests

Primary coverage target.

Use for:

- Money arithmetic.
- Domain validation.
- Ledger invariants.
- EMI formulas.
- Amortization schedules.
- Prepayment simulations.
- Budget calculations.
- Use case behavior.

### Repository Tests

Use SQLDelight test drivers where possible.

Use for:

- Query correctness.
- Mapping correctness.
- Transaction boundaries.
- Soft-delete behavior.
- Migration compatibility.
- Balance projection from immutable ledger entries.

### UI Tests

Use sparingly for critical workflows.

Use for:

- Transaction creation.
- SMS-assisted transaction review and confirmation.
- Loan creation.
- Loan schedule display.
- Budget status display.

### Snapshot Or Golden Tests

Use for loan schedules and financial summaries.

Golden tests should cover:

- Known loan terms.
- Known EMI.
- Known total interest.
- Known schedule rows.
- Known prepayment savings.

## Loan Engine Test Requirements

Every change to `shared/loan-engine` must include tests for:

- EMI amount.
- Schedule length.
- Total principal paid.
- Total interest paid.
- Closing balance equals zero.
- Final payment adjustment.
- Prepayment impact.
- Invalid input handling.

## Ledger Test Requirements

Ledger behavior must include tests for:

- Expense entries.
- Income entries.
- Transfer entries.
- Loan disbursement entries.
- EMI payment entries with principal and interest components.
- Interest posting entries.
- Investment entries.
- Refund entries.
- Balance derivation by account.
- Reversal and adjustment flows.
- Immutability of historical entries.
- Reconciliation between projected balances and ledger entries.

## SMS Parser Test Requirements

SMS-assisted capture must include tests for:

- Bank debit messages.
- Bank credit messages.
- Credit card spend messages.
- UPI payments and receipts.
- Wallet transactions.
- Failed transaction messages.
- OTP, promotional, and balance-only non-transaction messages.
- Low-confidence suggestions.
- User confirmation requirement.
- Raw SMS content not being persisted as a transaction by default.

## Feature Flag Test Requirements

Feature-flagged capabilities must include tests for:

- Disabled-by-default experimental behavior.
- No data mutation when a flagged feature is disabled.
- Stable behavior of core accounting workflows regardless of experimental flags.
- Platform app hiding disabled surfaces where practical.

## Cross-Platform Testing

Shared module tests should run on JVM first and later on native targets where practical. Financial calculations must not depend on JVM-only behavior.

## Test Data

Use readable fixtures:

- Small loans for manual verification.
- Realistic mortgage-style loans.
- High-interest consumer loans.
- Zero-interest loans.
- Edge-case rounding loans.

## Release Gates

Before release:

- All shared unit tests pass.
- Applicable integration tests pass.
- Database migrations pass from every supported previous schema.
- Loan Engine golden tests pass.
- Financial engines have unit tests for every production calculation path.
- Ledger invariants pass.
- Critical UI flows pass on Android.
- SMS parser fixtures pass.
- Ledger reconciliation tests pass.
- No `TODO`, `FIXME`, or placeholder code remains in production paths.
- No compiler warnings are introduced.
- iOS and Wear app shells build successfully.
