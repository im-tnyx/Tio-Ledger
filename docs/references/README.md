# UI Reference Sources

Production UI must be implemented using one or more approved reference sources. This file is the operational source of truth for UI reference priority, reference-note requirements, and decompiled-reference restrictions.

## Purpose

Tio Ledger should remain functionally familiar to Money Manager while modernizing visual quality, accessibility, responsiveness, and platform fit. Screenshots and reference notes in this directory are implementation references, not loose inspiration.

Reference material is used to understand product behavior and user expectations. It is never a license to copy proprietary implementation details.

## Source Priority

Use reference sources in this priority order:

1. Approved screenshots in `docs/references/`.
2. Approved Tio Ledger mockups.
3. Decompiled Money Manager technical reference (JADX) in `docs/references/realbyteapps/`.
4. Official Money Manager website: `https://realbyteapps.com/`.
5. Official Play Store screenshots.

AI should never invent major workflows or screen structures when an approved reference source is available. Lower-priority references may clarify behavior, but they must not override higher-priority approved screenshots, approved Tio mockups, project architecture, or the design system.

### Decompiled Technical Reference (JADX)

The `docs/references/realbyteapps/` folder contains a decompiled technical reference captured for analysis only. It exists only to understand:

- Screen hierarchy.
- Navigation flow.
- Information architecture.
- Naming conventions.
- Interaction patterns.
- Layout grouping.
- Dialog flow.
- Screen relationships.
- Feature discovery.

The JADX reference is a supporting analysis aid. It must be summarized as original reference notes before implementation begins. It must never be treated as source code, layout source, asset source, design-token source, or a direct implementation template.

### Explicit Restrictions

The decompiled reference must never be used to copy, translate, port, adapt, or mechanically reproduce:

- Java/Kotlin source code.
- XML layouts.
- Resources.
- Drawables.
- Icons.
- Strings.
- Colors.
- Dimensions.
- Animations.
- Assets.
- Any proprietary implementation.

All Tio Ledger implementation must be original, architecture-compliant, and based on the project's design system. If a behavior can only be understood by copying proprietary implementation details, do not use that detail.

## UI Development Rule

For every new production screen, the implementation workflow is:

1. Approved Screenshot.
2. Reference Notes.
3. Technical Analysis (JADX if needed).
4. Tio UI Specification.
5. Compose Multiplatform Implementation.
6. Pixel Review.
7. Accessibility Review.
8. Approval.

Steps 1 through 4 must be complete before production implementation begins. Technical analysis must be written in original language and must not include copied code, copied XML, copied resources, copied assets, or proprietary implementation details.

## Screen Notes

Screen notes live under `docs/references/notes/`.

Each screen note must include:

- Screen name.
- Primary reference.
- Supporting references.
- Workflow summary.
- Information hierarchy.
- Intentional deviations.
- Reason for deviations.
- Accessibility considerations.

Screen notes should capture decisions and observations in Tio Ledger's own words. Do not include copied code, copied layouts, copied resources, copied strings, copied colors, copied dimensions, or copied assets.

## AI Development Rule

AI assistants may inspect the JADX reference only to understand workflows, navigation, hierarchy, terminology, and interaction patterns.

AI assistants must never reproduce or translate proprietary code, XML, resources, or implementation details into the project.

All implementation must be original and follow the project's architecture and design system.

## Rules

- Add reference screenshots before implementing a major screen.
- Add or update the relevant screen note before implementing a production screen.
- Add a functional specification before implementing a production screen.
- Add a navigation definition before implementing a production screen.
- Add an acceptance checklist before implementing a production screen.
- Record which approved reference source was used.
- Keep filenames descriptive.
- Include short notes when a screenshot requires interpretation.
- Do not redesign a major workflow without updating or adding an approved reference.
- Preserve familiar information architecture unless a product decision says otherwise.
- Document any intentional deviation from the reference with justification and approval before implementation.
- Keep JADX analysis limited to workflow, navigation, hierarchy, terminology, and interaction-pattern understanding.
- Do not copy proprietary code, layouts, resources, or assets from any reference.

## Suggested Organization

```text
docs/references/
  notes/
  accounts/
  transactions/
  budgets/
  loans/
  analytics/
  settings/
  wear/
  realbyteapps/
```

Feature folders can contain screenshots and reference notes describing expected behavior, accepted modernization, and intentional differences. Shared screen-note templates live in `docs/references/notes/`.

## Screen Readiness Checklist

Before production UI implementation, each screen needs:

- Approved screenshot reference.
- Screen note under `docs/references/notes/`.
- Approved fallback reference source if screenshots are unavailable.
- Technical-analysis summary when JADX is used.
- Tio UI specification.
- Functional specification.
- Navigation entry and exit behavior.
- Acceptance checklist.
- Deviation log, if the implementation intentionally differs from the reference.
- Known platform differences, if any.
- Pixel review plan.
- Accessibility review plan.

## Implementation Guidance

When building a screen:

1. Start from the highest-priority approved reference source available.
2. Capture reference notes in original language before implementation.
3. Match workflow and information hierarchy first.
4. Modernize spacing, typography, colors, motion, accessibility, and responsive behavior through the Tio Ledger design system.
5. Document and approve intentional deviations before implementation.
6. Validate that the result still feels familiar to existing Money Manager users.
7. Confirm no copied code, XML, resources, or proprietary implementation details entered the project.
