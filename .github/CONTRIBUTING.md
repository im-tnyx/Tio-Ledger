# Contributing

Thanks for helping build Tio Ledger.

## Before You Start

Read the canonical documentation:

- [Documentation Home](../docs/README.md)
- [Engineering Guidelines](../docs/engineering-guidelines.md)
- [Definition Of Done](../docs/definition-of-done.md)
- [Architecture Decision Records](../docs/adr/README.md)

## Development Rules

- Keep changes small and reviewable.
- Respect module boundaries.
- Do not duplicate financial logic.
- Do not use `Float` or `Double` for money.
- Preserve ledger integrity.
- Implement UI only from approved references.
- Update ADRs when architecture changes.

## Pull Requests

Every pull request should explain:

- What changed.
- Why it changed.
- How it was verified.
- Whether ledger, financial, UI, or architecture behavior is affected.

No feature should be merged until applicable Definition of Done items are complete.
