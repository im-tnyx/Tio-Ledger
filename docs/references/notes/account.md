# Account Screen Reference Note

## Screen Name

Accounts

## Primary Reference

- `docs/references/accounts/Screenshot_20260630_164106_Money Manager.jpg`

## Supporting References

- `docs/references/accounts/Screenshot_20260630_164124_Money Manager.jpg`
- `docs/references/accounts/Screenshot_20260630_164134_Money Manager.jpg`
- `docs/references/accounts/Screenshot_20260630_164139_Money Manager.jpg`
- `docs/references/accounts/README.md`
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

## Credit Card Specialization Reference Decision (#74)

### What the approved screenshot establishes

The primary approved Accounts screenshot `docs/references/accounts/Screenshot_20260630_164106_Money Manager.jpg` provides higher-priority evidence than lower-priority fallback sources for the Accounts list hierarchy.

It establishes the following presentation facts:

- Credit cards remain part of the Accounts screen rather than becoming a primary navigation destination.
- `Credit Card` is presented as its own account-type group.
- The group uses two distinct financial labels: `Balance Payable` and `Outst. Balance`.
- Individual credit-card rows are presented in the same two-value context instead of the single generic balance treatment used by current Accounts v1.
- The reference Accounts hierarchy also includes a `Loan` group. Tio Ledger's separately approved Loans list and Loan Details workflow remain an intentional product extension and are not removed or redesigned by this Accounts reference finding.

The screenshot does **not** establish a dedicated Credit Card Details screen, account-row tap behavior, statement-cycle workflow, due-date workflow, payment flow, or any formula for either displayed value. Those behaviors must not be invented from this screenshot.

### Current Tio capability gap

Current Accounts v1 groups `AccountType.CREDIT_CARD` correctly, but its Application/UI read path exposes only one generic ledger-derived account balance:

- `ListAccountSummariesUseCase` returns `AccountBalanceSummary(account, balance)`.
- `AccountsViewModel` maps that single balance into both group aggregation and each generic account row.
- `AccountsScreen` has no credit-card-specific two-value row/header contract.
- No typed `AccountDetails` or `CreditCardDetails` route exists.

Therefore the screenshot's two-value credit-card presentation cannot be implemented correctly by presentation-only formatting. A ledger-first Application contract must define the financial meaning first.

### Financial semantics that remain unresolved

Until explicitly approved, Tio Ledger does not define:

- the exact meaning of `Balance Payable`;
- the exact meaning of `Outstanding Balance`;
- which immutable ledger entries, transaction states, or periods contribute to either value;
- sign conventions for positive, zero, or negative credit-card values;
- group aggregation semantics when multiple credit cards exist;
- whether statement-cycle, statement balance, billing date, due date, minimum payment, or payment-allocation concepts are required;
- whether the frozen v1 persistence model already contains enough information to derive both values deterministically.

Do not map the current generic liability balance to either label merely because it is available. Do not persist a mutable payable/outstanding balance solely to reproduce the screenshot.

### Future implementation gate

Before production code for this specialization:

1. Approve the ledger-first definitions of `Balance Payable` and `Outstanding Balance`.
2. Audit Domain/Application/Data/SQLDelight support for those definitions.
3. Add an Application-owned read model that exposes only deterministic derived values.
4. Update this reference note with the approved financial contract and exact row/group behavior.
5. Decide separately whether any Account Details or Credit Card Details destination is needed; the current screenshot does not authorize one.
6. Add focused financial regression tests before Compose consumes the new values.
7. Perform the normal Pixel, theme, responsive, and accessibility review required by the Definition of Done during the later production UI slice.

This #74 reference/specification slice is GitHub-only and makes no production, schema, ledger, or financial-behavior change.

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
