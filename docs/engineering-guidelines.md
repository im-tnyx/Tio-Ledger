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
- Follow the reference priority and screen-note workflow in `docs/references/README.md`.
- For every production screen, complete Approved Screenshot, Reference Notes, Technical Analysis when needed, Tio UI Specification, Compose Multiplatform Implementation, Pixel Review, Accessibility Review, and Approval in that order.
- Use `docs/references/realbyteapps/` only as a decompiled technical reference for workflow, navigation, hierarchy, terminology, layout grouping, dialog flow, screen relationships, feature discovery, and interaction patterns.
- Do not copy Java/Kotlin source, XML layouts, resources, drawables, icons, strings, colors, dimensions, animations, assets, or proprietary implementation details from any reference.
- Do not redesign workflows without approval.
- Maintain familiar Money Manager behavior unless an intentional improvement is documented and approved.
- Do not invent major workflows or screen structures when an approved reference source is available.

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
- AI assistants may inspect the JADX reference only to understand workflows, navigation, hierarchy, terminology, and interaction patterns.
- AI assistants must never reproduce or translate proprietary code, XML, resources, or implementation details into the project.
- All implementation must be original and follow the project architecture and design system.
- Keep commits small and reviewable.
- Treat existing documentation and ADRs as binding unless the user approves a change.
