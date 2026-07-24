# Loan Creation And Details v1 Reference Note

## Screen

Loan Creation and Loan Details v1

## Reference Readiness

- Checked-in Loan screenshot reference: unavailable at milestone start.
- Approved fallback product source: issue #12 scope and acceptance criteria.
- Approved fallback visual source: the existing production Accounts, Budgets, Categories, app-bar, card, dialog, empty/error/loading, and typed-navigation patterns already checked into `shared:ui`.
- Financial source of truth: persisted Application read models produced from the deterministic loan engine and SQLDelight schedule rows.
- Technical reference boundary: no proprietary Java/Kotlin source, XML, resources, assets, strings, colors, dimensions, animations, or implementation details are copied or adapted.

The fallback package is sufficient because it fixes the workflow, information hierarchy, component vocabulary, navigation contract, error behavior, and financial boundaries without inventing a proprietary visual layout.

## Workflow Summary

1. Open the typed `MainRoute.Loans` destination.
2. Review loans in deterministic name and ID order.
3. Each loan card shows name, status, principal, scheduled EMI, outstanding principal, remaining installments, and next due date.
4. Use the add action to open a creation dialog.
5. Enter name, principal, annual interest rate, tenure, and start date.
6. Select an active `LOAN_LINKED` account.
7. Select a different active non-loan asset account with the same currency for disbursement.
8. Submit the form through `CreateLoanUseCase`; validation and persistence failures remain visible in the dialog.
9. Successful creation closes the dialog, refreshes the list, and exposes the persisted loan.
10. Select a loan card to navigate to typed Loan Details.
11. Loan Details shows contractual terms, EMI/outstanding totals, linked account labels, and the persisted amortization schedule.
12. Navigate back to Loans without mutating the persisted schedule.

## Information Hierarchy

### Loan list

1. App bar title and add action.
2. Success, loading, empty, and repository-error feedback.
3. Deterministically ordered loan cards.
4. Loan name and active status.
5. Outstanding principal as the primary value.
6. Principal and scheduled EMI.
7. Remaining installments and next due date.
8. Select-card affordance for details.

### Loan creation

1. Dialog title and explanatory fixed monthly reducing-balance scope.
2. Loan name.
3. Principal amount.
4. Annual interest rate as a percentage converted to basis points outside Compose.
5. Tenure in whole months.
6. Start date using an ISO `YYYY-MM-DD` text field for multiplatform determinism in v1.
7. Linked loan-account selector.
8. Disbursed asset-account selector.
9. Validation/persistence message.
10. Cancel and create actions.

### Loan details

1. Back navigation and loan name.
2. Outstanding principal, EMI, total interest, and total payable summary.
3. Principal, annual rate, tenure, start date, status, linked loan account, and disbursed account.
4. Amortization schedule ordered by installment number.
5. Each installment shows due date, payment, principal, interest, opening balance, closing balance, and status.

## Tio UI Specification

- Reuse `TioAppBar`, `TioBottomNavigation`, `TioFloatingActionButton`, `TioCard`, `TioLoadingState`, `TioEmptyState`, `TioErrorState`, typography, shapes, and spacing tokens.
- Use a dialog for loan creation so the v1 workflow remains compact on Android and iOS.
- Use separate account-selection dialogs so account lists do not overlap the creation dialog.
- Use a dedicated details destination rather than expanding a card inline.
- Use text labels with every status; color must never be the only status signal.
- Use compact stacked schedule cards at phone widths instead of a horizontally scrolling financial table.
- Format money, rates, and dates in the ViewModel/presentation mapping layer, not in Composables.
- Parse editable principal and percentage text in the ViewModel without floating-point money arithmetic.
- Convert percentage input to integer basis points with at most two decimal places.
- Do not calculate EMI, amortization, totals, or outstanding principal inside UI or ViewModels; consume Application read models only.
- Do not expose payment, prepayment, refinance, close, or payoff actions in v1.

## Navigation Definition

- Keep `MainRoute.Loans` at path `loans` with `TioIconToken.Loan`.
- Add typed `MainRoute.LoanDetails(loanId)` with path `loans/{loanId}` semantics and the Loan icon token.
- Register both cases in `RootNavigationHost`.
- Selecting a loan card emits the typed details destination.
- Details back navigation returns to `MainRoute.Loans`.
- Loans remains outside the five-item primary bottom navigation; bottom navigation still renders for the list and can exit to primary destinations.
- The details screen omits bottom navigation to preserve a focused nested-destination hierarchy.

## Intentional Deviations

- No checked-in Loan screenshot exists, so the implementation follows established Tio Ledger production components rather than reconstructing an unknown external layout.
- Creation uses an ISO date text field rather than a platform date picker because the shared UI has no approved multiplatform picker abstraction yet.
- Interest entry accepts a percentage with up to two decimals, while persistence uses integer basis points.
- The schedule uses stacked cards instead of a dense table for phone readability and accessibility.
- Loans is not added to the primary bottom navigation because the current five destinations are already allocated; the typed route remains available for direct/overflow entry.
- Payment, prepayment, payoff, contractual editing, and closure controls are absent because the posting and reconciliation policies are explicitly out of scope.

## Accessibility Considerations

- Loan cards expose a combined semantic description covering name, status, outstanding principal, EMI, remaining installments, and next due date.
- Add, back, account-selection, retry, cancel, and create controls have readable labels or content descriptions.
- All loading, empty, validation, persistence, and repository failures are conveyed as text.
- Schedule cards expose installment number, due date, payment, principal, interest, balances, and status in reading order.
- Long account/loan names wrap or truncate without obscuring financial values.
- Touch targets use Material components and existing Tio spacing.
- Light/dark previews must preserve contrast and hierarchy.

## Functional Acceptance Checklist

- [ ] Loading state is visible.
- [ ] Empty state offers an add-loan action.
- [ ] Repository failure offers retry.
- [ ] Populated list shows persisted loan summaries in deterministic order.
- [ ] Add dialog validates name, principal, percentage rate, tenure, date, and account selections.
- [ ] Account selectors expose only eligible active accounts for each role.
- [ ] Currency mismatch and same-account selections remain visibly rejected.
- [ ] Valid creation persists the loan and complete schedule atomically.
- [ ] Successful creation refreshes the list and shows feedback.
- [ ] Selecting a loan opens typed details navigation.
- [ ] Details show contractual terms, summaries, account labels, and persisted schedule rows.
- [ ] Details loading/repository failure and retry are visible.
- [ ] UI/ViewModels do not access repositories, SQLDelight, or the loan engine.
- [ ] No UI-side EMI, amortization, interest, or outstanding calculation exists.
- [ ] Navigation, ViewModel, and presentation behavior are test-covered.
- [ ] Previews cover list, empty, creation, and details states.
- [ ] Pixel review confirms phone-width readability in light and dark themes.

## Pixel Review Plan

- Verify compact list cards at common phone widths.
- Verify long loan/account names do not collide with monetary values.
- Verify creation dialog remains usable with validation messages and smaller heights.
- Verify the schedule remains readable for 1-, 12-, and long-tenure loans.
- Verify zero-interest and non-zero-interest values use the same hierarchy.
- Verify paid/pending labels remain distinguishable without relying on color.

## Approval Record

Issue #12 milestone execution and the user's `go` approval authorize this checked-in fallback package for Loan Creation and Loan Details v1. A future approved screenshot may refine visual styling, but it must not change the validated financial, persistence, Application, or navigation contracts without an explicit follow-up decision.
