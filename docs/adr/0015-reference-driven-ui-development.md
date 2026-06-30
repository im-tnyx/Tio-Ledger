# ADR-0015: Reference-Driven UI Development

## Status

Accepted

## Context

The product should remain familiar to Money Manager users. Production UI created directly from abstract prompts or AI imagination risks drifting away from recognizable workflows and information architecture.

## Decision

Every production UI screen must have:

- Approved screenshot reference.
- Approved fallback reference source if screenshots are unavailable.
- Functional specification.
- Navigation definition.
- Acceptance checklist.
- Deviation log when the implementation intentionally differs from the reference.

No production UI should be implemented directly from AI imagination.

Reference priority:

1. Approved screenshots in `docs/references/`.
2. Approved Tio Ledger mockups.
3. Decompiled Money Manager technical reference (JADX) in `docs/references/realbyteapps/`.
4. Official Money Manager website: `https://realbyteapps.com/`.
5. Official Play Store screenshots.

References and approval notes live under `docs/references/`. Decompiled technical analysis may be summarized in screen notes, but proprietary code, XML, resources, drawables, icons, strings, colors, dimensions, animations, assets, and implementation details must not be copied, translated, ported, or adapted into Tio Ledger.

Any intentional deviation from the reference must be documented with justification and approved before implementation.

## Consequences

Positive:

- Keeps UI implementation grounded.
- Makes product review concrete.
- Preserves familiar workflows while allowing modern polish.
- Improves QA by requiring acceptance checklists.
- Prevents production UI from being invented without reference-backed product intent.

Negative:

- UI work requires reference preparation.
- Reference maintenance becomes part of product governance.
- Experimental prototypes must be clearly separated from production UI.
- Official web and store references may need to be captured locally to avoid drift over time.
- Decompiled technical references require strict review discipline so no proprietary implementation enters the project.
