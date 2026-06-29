# Financial Rules

Financial correctness is a core product requirement.

Rules:

- Never use `Float` or `Double` for money.
- All calculations must be deterministic.
- Historical ledger entries are immutable.
- Every balance must be derivable from ledger entries.
- Loan calculations must match deterministic reference results.
- Every financial engine requires unit tests.
- Ledger invariants must always pass.

Canonical references:

- [../docs/architecture.md](../docs/architecture.md)
- [../docs/loan-engine-design.md](../docs/loan-engine-design.md)
- [../docs/offline-first-data-strategy.md](../docs/offline-first-data-strategy.md)
- [../docs/testing-strategy.md](../docs/testing-strategy.md)
- [../docs/adr/0012-financial-accuracy.md](../docs/adr/0012-financial-accuracy.md)
- [../docs/adr/0013-ledger-first-architecture.md](../docs/adr/0013-ledger-first-architecture.md)
