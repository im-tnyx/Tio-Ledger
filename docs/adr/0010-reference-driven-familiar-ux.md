# ADR-0010: Reference-Driven Familiar UX

## Status

Accepted

## Context

Tio Ledger is inspired by Money Manager. The product should modernize the experience without changing the core behavior users already understand. Major UI changes can accidentally alter workflow semantics if they are implemented from abstract descriptions alone.

## Decision

Every major UI screen must be implemented from approved reference sources.

Reference priority:

1. Approved screenshots stored in `docs/references/`.
2. Official Money Manager website: `https://realbyteapps.com/`.
3. Official Play Store listing and screenshots.
4. Approved design mockups created for Tio Ledger.

Screenshots and official sources are implementation references. The team may modernize spacing, typography, animation, accessibility, responsiveness, and visual polish, but the information architecture and interaction flow should remain recognizable unless an approved reference explicitly changes them.

If screenshots are unavailable, the official website may be used to understand layouts, workflows, terminology, and feature behavior. AI should never invent major workflows or screen structures when an official reference source is available.

## Consequences

Positive:

- Preserves familiar workflows.
- Reduces subjective redesign drift.
- Gives implementation teams concrete visual and behavioral targets.
- Supports incremental modernization without product confusion.
- Allows official public references to unblock implementation when local screenshots are not yet available.

Negative:

- UI implementation depends on collecting and maintaining references.
- Product changes require reference updates.
- Some platform adaptation will need careful documentation.
- Official references may change over time, so important assumptions should be captured locally before implementation.
