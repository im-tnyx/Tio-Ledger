# Dashboard Screen Reference Note

## Screen Name

Dashboard

## Primary Reference And Runtime Status

None of the [24 checked-in root screenshots](../screenshot-inventory.md) shows a Dashboard/Home content view suitable as a direct reference. `MainRoute.Dashboard` is registered in navigation, but `RootNavigationHost` currently renders `MainPlaceholderDestination` for it; a template/preview is not production Dashboard content. A product-approved screenshot or fallback source and complete screen specification are still required before implementing that content.

## Supporting References

- User-approved clean-header decision recorded in issue #31 on 2026-08-05.
- Approved Tio Ledger mockups, if available.
- `docs/references/realbyteapps/` only if workflow, navigation, hierarchy, terminology, layout grouping, dialog flow, screen relationship, feature discovery, or interaction-pattern analysis is required.
- Official Money Manager website only if higher-priority references are insufficient.
- Official Play Store screenshots only if higher-priority references are insufficient.

## Workflow Summary

The current route can be selected from bottom navigation but leads to a placeholder. The intended summary workflow, actions, and data sources remain to be specified against an approved reference. The clean-header decision below applies to a future production Dashboard; it does not validate its content.

## Information Hierarchy

Only the navigation position and the approved absence of a branded top bar are established. Summary order, sections, actions, and relationships to Accounts, Transactions, Categories, Budgets, Loans, and Reports are not yet an approved production hierarchy.

## Intentional Deviations

- The Home/Dashboard template omits a branded app-name top bar. Dashboard content begins directly on the screen surface while bottom navigation and the floating action button remain unchanged.

## Reason For Deviations

- The user explicitly approved a cleaner Home screen on 2026-08-05. Repeating the product name at the top adds visual weight and consumes vertical space without improving navigation or task clarity.

## Accessibility Considerations

- Removing the decorative branded top bar must not remove any navigation action or status information.
- The template's first section was conceived as `Overview`; the current production route is a placeholder and does not display that section. Any future Overview must be specified and reviewed, while existing bottom-navigation semantics remain unchanged.
- Complete screen reader labels, touch targets, dynamic type, light/dark theme behavior, and responsive-layout review before Dashboard is promoted from template to production screen.

## JADX Boundary

No copied Java/Kotlin source, XML layouts, resources, drawables, icons, strings, colors, dimensions, animations, assets, or proprietary implementation details are allowed in this note or implementation.
