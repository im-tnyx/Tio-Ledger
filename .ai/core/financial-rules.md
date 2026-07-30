# Financial Rules

- Store money as integer minor units; never use `Float` or `Double` for money.
- Use explicit deterministic rounding for interest and ratio calculations.
- Keep historical ledger entries immutable.
- Derive balances from ledger entries.
- Keep financial calculations outside UI and ViewModels.
- Require focused regression tests whenever financial behavior changes.
- Require explicit user confirmation before automation creates or changes
  financial records.

Canonical references:

- [Architecture](../../docs/architecture.md)
- [Loan Engine Design](../../docs/loan-engine-design.md)
- [Testing Strategy](../../docs/testing-strategy.md)
- [ADR-0009: Money And Interest Precision](../../docs/adr/0009-deterministic-money-interest-precision.md)
- [ADR-0012: Financial Accuracy](../../docs/adr/0012-financial-accuracy.md)
- [ADR-0013: Ledger First Architecture](../../docs/adr/0013-ledger-first-architecture.md)
