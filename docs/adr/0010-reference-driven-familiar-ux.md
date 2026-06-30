# ADR-0010: Reference-Driven Familiar UX

## Status

Accepted

## Context

Tio Ledger is inspired by Money Manager. The product should modernize the experience without changing the core behavior users already understand. Major UI changes can accidentally alter workflow semantics if they are implemented from abstract descriptions alone.

## Decision

Every major UI screen must be implemented from approved reference sources.

Reference priority:

1. Approved screenshots in `docs/references/`.
2. Approved Tio Ledger mockups.
3. Decompiled Money Manager technical reference (JADX) in `docs/references/realbyteapps/`.
4. Official Money Manager website: `https://realbyteapps.com/`.
5. Official Play Store screenshots.

Screenshots and official sources are implementation references. The team may modernize spacing, typography, animation, accessibility, responsiveness, and visual polish, but the information architecture and interaction flow should remain recognizable unless an approved reference explicitly changes them.

If screenshots are unavailable, approved Tio Ledger mockups should be used before technical analysis. The decompiled Money Manager technical reference may be inspected only to understand screen hierarchy, navigation flow, information architecture, naming conventions, interaction patterns, layout grouping, dialog flow, screen relationships, and feature discovery. Java/Kotlin source, XML layouts, resources, drawables, icons, strings, colors, dimensions, animations, assets, and proprietary implementation details must never be copied, translated, ported, or adapted into Tio Ledger. AI should never invent major workflows or screen structures when an approved reference source is available.

## Consequences

Positive:

- Preserves familiar workflows.
- Reduces subjective redesign drift.
- Gives implementation teams concrete visual and behavioral targets.
- Supports incremental modernization without product confusion.
- Allows approved mockups, controlled technical analysis, official public references, and local screenshots to unblock implementation when primary screenshots are not yet sufficient.

Negative:

- UI implementation depends on collecting and maintaining references.
- Product changes require reference updates.
- Some platform adaptation will need careful documentation.
- Official references may change over time, so important assumptions should be captured locally before implementation.
- Decompiled technical references require strict review discipline so no proprietary implementation enters the project.
