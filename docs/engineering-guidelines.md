# Engineering Guidelines

These rules apply to all production code in Tio Ledger.

All production changes must satisfy the [Definition Of Done](definition-of-done.md) before merge.

## General

- Prefer readability over cleverness.
- Follow Kotlin coding conventions.
- Keep files focused and cohesive.
- Avoid unnecessary abstractions.
- Keep changes small, intentional, and reviewable.

## Architecture

- Respect module boundaries.
- Domain layer must not depend on UI.
- Shared business logic belongs in Kotlin Multiplatform shared modules.
- Do not duplicate financial logic across apps or modules.
- Do not replace approved architecture with personal preferences.

## Financial Rules

- Never use `Float` or `Double` for money.
- All calculations must be deterministic.
- Historical ledger entries are immutable.
- Every balance must be derivable from ledger entries.
- Loan calculations must match deterministic reference results.

## UI

- Implement UI from approved references only.
- Do not redesign workflows without approval.
- Maintain familiar Money Manager behavior unless an intentional improvement is documented and approved.
- Do not invent major workflows or screen structures when an official reference source is available.

## Testing

- Every financial engine requires unit tests.
- Ledger invariants must always pass.
- Loan calculations must match deterministic reference results.
- Changes to financial behavior require focused regression tests.
- Unit tests must pass before merge.
- Integration tests must pass where applicable.
- Do not leave `TODO`, `FIXME`, or placeholder code in production paths.

## Documentation And Review

- Update relevant documentation with the code change.
- Update ADRs when architecture changes.
- Update the changelog when applicable.
- Do not merge a feature until applicable Definition of Done items are complete.

## AI Development Rules

- Never invent APIs or business rules.
- If requirements are unclear, stop and request clarification.
- Do not replace approved architecture with personal preferences.
- Keep commits small and reviewable.
- Treat existing documentation and ADRs as binding unless the user approves a change.
