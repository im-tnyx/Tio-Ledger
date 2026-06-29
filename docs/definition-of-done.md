# Definition Of Done

A feature is considered complete only when all applicable checklist items are satisfied. No feature should be merged unless the applicable Definition of Done items are complete.

## Functional

- Requirements implemented.
- Business rules verified.
- Ledger integrity preserved.
- Financial calculations validated.

## Quality

- Unit tests pass.
- Integration tests pass, where applicable.
- No `TODO`, `FIXME`, or placeholder code.
- No compiler warnings introduced.

## UI

- Matches approved reference.
- Light and dark themes verified.
- Responsive layouts verified.
- Accessibility reviewed.

## Documentation

- Relevant documentation updated.
- ADR updated if architecture changed.
- Architecture changelog updated when applicable.

## Performance

- No measurable regression.
- Startup remains responsive.
- Common interactions remain responsive.

## Merge Rule

If an item is not applicable, the reason should be clear in the implementation notes, pull request description, or review summary. Financial correctness, ledger integrity, and user-confirmed automation rules are never optional for affected features.
