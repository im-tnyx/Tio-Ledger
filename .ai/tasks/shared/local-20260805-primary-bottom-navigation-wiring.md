# Primary Bottom Navigation Wiring

Status: In Progress
Objective: Wire every visible production primary bottom-navigation item to its typed `MainRoute` while preserving canonical routes and selected-state behavior.
Branch: `fix/primary-bottom-navigation-wiring`
Scope: `shared/ui` navigation shell and affected screen route boundaries, focused navigation tests, and navigation validation documentation
Created: `2026-08-05`
Last Updated: `2026-08-05`
Issue: `https://github.com/im-tnyx/Tio-Ledger/issues/34`

## Required Context

- `.ai/core/architecture.md`
- `.ai/core/coding-rules.md`
- `.ai/core/ui-rules.md`
- `.ai/core/workflow-rules.md`
- `AGENTS.md`
- `README.md`
- `docs/README.md`
- `docs/engineering-guidelines.md`
- `docs/definition-of-done.md`
- `docs/status/NAVIGATION_VALIDATION_REPORT.md`
- `.github/PUSH_TEMPLATE.md`

## Constraints

- Keep `TioNavigationGraphs.main.bottomNavigationRoutes` as the canonical route source.
- Preserve the five approved primary destinations: Dashboard, Accounts, Transactions, Categories, and Budgets.
- Do not redesign Dashboard content or introduce a new screen hierarchy.
- Keep Reports and Loans navigable to primary tabs without falsely selecting a primary item.
- Preserve existing detail, add, SMS-review, and back-navigation flows.
- No financial, persistence, schema, analytics, loan, notification, or automation changes.
- Reuse existing `TioBottomNavigation` and typed `MainRoute` contracts.

## Implementation Plan

- [x] Verify issue #34 against current `main` runtime source.
- [x] Complete Loan Payoff Analytics post-merge closeout.
- [x] Create dedicated branch from updated `main`.
- [ ] Introduce a shared, testable bottom-navigation model derived from `MainGraph.bottomNavigationRoutes`.
- [ ] Wire `AccountsRoute` and `AccountsScreen` through typed navigation callbacks.
- [ ] Wire Dashboard/placeholder bottom-navigation taps through `RootNavigationHost`.
- [ ] Reuse the shared model across existing production screens to remove duplicate item-to-route mapping.
- [ ] Add focused route-order, selected-state, unknown-item, and callback-forwarding tests.
- [ ] Update navigation validation documentation.
- [ ] Run exact-head CI and review the final diff.
- [ ] Open PR and advance only after checks pass.

## Validation

- Not run yet.

## Next Action

Read the required AI context files, implement the shared bottom-navigation model, then wire Accounts and Dashboard before refactoring existing working screens.
