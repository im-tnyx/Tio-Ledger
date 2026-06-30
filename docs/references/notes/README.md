# Screen Reference Notes

This folder contains screen-level reference notes used before production UI implementation.

Screen notes convert approved screenshots, approved Tio Ledger mockups, and permitted technical analysis into original Tio Ledger implementation guidance. They must not contain copied code, copied XML, copied resources, copied strings, copied colors, copied dimensions, copied animations, copied assets, or proprietary implementation details.

## Required Fields

Each screen note must include:

- Screen name.
- Primary reference.
- Supporting references.
- Workflow summary.
- Information hierarchy.
- Intentional deviations.
- Reason for deviations.
- Accessibility considerations.

## Required Workflow

For every new production screen:

1. Approved Screenshot.
2. Reference Notes.
3. Technical Analysis (JADX if needed).
4. Tio UI Specification.
5. Compose Multiplatform Implementation.
6. Pixel Review.
7. Accessibility Review.
8. Approval.

## JADX Boundary

AI assistants and engineers may inspect `docs/references/realbyteapps/` only to understand workflows, navigation, hierarchy, terminology, and interaction patterns.

Do not reproduce or translate proprietary code, XML, resources, or implementation details into Tio Ledger. All implementation must be original and follow the project's architecture and design system.
