# Account Screen Reference Note

## Screen Name

Accounts

## Primary Reference

- `docs/references/Screenshot_20260630_164106_Money Manager.jpg`

## Supporting References

- `docs/status/ACCOUNTS_SCREEN_VALIDATION_REPORT.md`
- `docs/status/UI_REFERENCE_COMPLIANCE_REPORT.md`
- `docs/status/ACCESSIBILITY_REPORT.md`
- `docs/references/realbyteapps/` only if additional workflow, navigation, hierarchy, terminology, or interaction-pattern analysis is required.

## Workflow Summary

Accounts Screen v1 presents the user's accounts with a total balance summary, search, account-type grouping, account rows, current balance, and currency context. The screen is read/display focused for v1 and does not add account creation, editing, transaction, loan, budget, report, SMS, or settings workflows.

## Information Hierarchy

1. Top app bar with screen title and actions.
2. Total balance summary.
3. Search.
4. Account-type group headings.
5. Account rows with icon, account name, type/currency context, and current balance.
6. Empty, loading, and error states when applicable.

## Intentional Deviations

- Icons use token-based placeholders from the existing Compose design system until production icon assets are approved.
- Amounts use deterministic currency-code formatting until a shared currency formatter is approved.
- Credit-card payable/outstanding specialization remains outside Accounts Screen v1 scope.

## Reason For Deviations

- The UI Foundation v1 design system is the approved source for current icons, tokens, typography, and theme behavior.
- Financial display behavior must remain deterministic until currency formatting is formalized.
- Additional credit-card workflows require their own approved reference and application-layer behavior.

## Accessibility Considerations

- Summary row should expose a combined semantic label for assets, liabilities, and total.
- Account rows should expose account name, type label, and displayed balance.
- Top app bar actions require content descriptions.
- Loading, empty, and error states require readable labels.
- Text must support dynamic type and avoid clipped balances.

## JADX Boundary

No copied Java/Kotlin source, XML layouts, resources, drawables, icons, strings, colors, dimensions, animations, assets, or proprietary implementation details are allowed in this note or implementation.
