# Dashboard Screen Reference Note

## Screen Name

Dashboard

## Primary Reference

- TBD before Dashboard implementation: approved screenshot in `docs/references/`.

## Supporting References

- User-approved clean-header decision recorded in issue #31 on 2026-08-05.
- Approved Tio Ledger mockups, if available.
- `docs/references/realbyteapps/` only if workflow, navigation, hierarchy, terminology, layout grouping, dialog flow, screen relationship, feature discovery, or interaction-pattern analysis is required.
- Official Money Manager website only if higher-priority references are insufficient.
- Official Play Store screenshots only if higher-priority references are insufficient.

## Workflow Summary

TBD before implementation. Capture the dashboard's role, entry points, summary cards, trends, and navigation relationships in original Tio Ledger language.

## Information Hierarchy

TBD before implementation. Document top-level summary order, sections, actions, and relationships to Accounts, Transactions, Categories, Budgets, Loans, and Reports.

## Intentional Deviations

- The Home/Dashboard template omits a branded app-name top bar. Dashboard content begins directly on the screen surface while bottom navigation and the floating action button remain unchanged.

## Reason For Deviations

- The user explicitly approved a cleaner Home screen on 2026-08-05. Repeating the product name at the top adds visual weight and consumes vertical space without improving navigation or task clarity.

## Accessibility Considerations

- Removing the decorative branded top bar must not remove any navigation action or status information.
- The first visible content section remains `Overview`, and existing bottom-navigation semantics remain unchanged.
- Complete screen reader labels, touch targets, dynamic type, light/dark theme behavior, and responsive-layout review before Dashboard is promoted from template to production screen.

## JADX Boundary

No copied Java/Kotlin source, XML layouts, resources, drawables, icons, strings, colors, dimensions, animations, assets, or proprietary implementation details are allowed in this note or implementation.
