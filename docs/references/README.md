# UI Reference Sources

Production UI must be implemented using one or more approved reference sources.

## Purpose

Tio Ledger should remain functionally familiar to Money Manager while modernizing visual quality, accessibility, responsiveness, and platform fit. Screenshots in this directory are implementation references, not loose inspiration.

## Source Priority

Use reference sources in this priority order:

1. Approved screenshots stored in `docs/references/`.
2. Official Money Manager website: `https://realbyteapps.com/`.
3. Official Money Manager app: `docs/references/realbyteapps/`.
4. Official Play Store listing and screenshots.
5. Approved design mockups created for Tio Ledger.

If local screenshots are unavailable, implementation may reference the official website to understand layouts, workflows, terminology, and feature behavior. AI should never invent major workflows or screen structures when an official reference source is available.

## Rules

- Add reference screenshots before implementing a major screen.
- Add a functional specification before implementing a production screen.
- Add a navigation definition before implementing a production screen.
- Add an acceptance checklist before implementing a production screen.
- Record which approved reference source was used.
- Keep filenames descriptive.
- Include short notes when a screenshot requires interpretation.
- Do not redesign a major workflow without updating or adding an approved reference.
- Preserve familiar information architecture unless a product decision says otherwise.
- Document any intentional deviation from the reference with justification and approval before implementation.

## Suggested Organization

```text
docs/references/
  accounts/
  transactions/
  budgets/
  loans/
  analytics/
  settings/
  wear/
```

Each folder can contain screenshots and a `notes.md` file describing expected behavior, accepted modernization, and intentional differences.

## Screen Readiness Checklist

Before production UI implementation, each screen needs:

- Approved screenshot reference.
- Approved fallback reference source if screenshots are unavailable.
- Functional specification.
- Navigation entry and exit behavior.
- Acceptance checklist.
- Deviation log, if the implementation intentionally differs from the reference.
- Known platform differences, if any.

## Implementation Guidance

When building a screen:

1. Start from the highest-priority approved reference source available.
2. Match workflow and information hierarchy first.
3. Modernize spacing, typography, colors, motion, accessibility, and responsive behavior.
4. Document and approve intentional deviations before implementation.
5. Validate that the result still feels familiar to existing Money Manager users.
