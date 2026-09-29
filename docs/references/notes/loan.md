# Loan Creation, Details, And Payoff Analytics Reference Note

## Screen

Loan Creation and Loan Details v1 with Loan Payoff Analytics v1

## Reference Readiness

- Checked-in Loan screenshot reference: unavailable at milestone start.
- Approved fallback product sources: issue #12 for Loan Creation and Loan Details v1 and issue #29 for Loan Payoff Analytics v1.
- Approved fallback visual source: the existing production Accounts, Budgets, Categories, Reports, app-bar, card, dialog, empty/error/loading, and typed-navigation patterns already checked into `shared:ui`.
- Financial source of truth: persisted Application read models produced from deterministic loan terms and SQLDelight schedule rows.
- Technical reference boundary: no proprietary Java/Kotlin source, XML, resources, assets, strings, colors, dimensions, animations, or implementation details are copied or adapted.

The fallback package is sufficient because it fixes the workflow, information hierarchy, component vocabulary, navigation contract, error behavior, payoff metrics, and financial boundaries without inventing a proprietary visual layout.

### Loans Entry-Point References (#61)

- Approved checked-in screenshots, `docs/references/accounts/`:
  - The primary Accounts screenshot shows loans as a `Loan` account group inside the Accounts screen.
  - The Accounts top bar carries non-primary actions (statistics and an overflow action).
  - The four reference tabs (transactions, statistics, accounts, more) contain no separate Loans destination.
  - The supporting `More` settings and configuration screenshots contain no Loans entry either.
- Checked-in JADX technical reference (structure only, per `docs/references/README.md`): no dedicated loan screen or loan menu exists. Loan concepts appear only as an account-group concept and in help text. No code, layout, resource, or string was copied.
- Approved Tio precedent: `docs/references/notes/settings-reminders.md` defines Settings as a non-primary shell destination. Production exposes it as an Accounts app-bar action that routes to the existing typed `MainRoute.Settings`.
- Conclusion: the reference product keeps loans inside Accounts and never gives them a primary tab. Tio's Loans list is a product extension (`docs/product-requirements.md` → Loans) with no screenshot of its own. The closest reference-consistent entry is therefore a non-primary Accounts app-bar action, following the approved Settings precedent.

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
12. Review read-only payoff analytics derived from the same persisted schedule.
13. Navigate back to Loans without mutating loan terms, installment statuses, or financial history.

## Payoff Analytics Rules

- Only installments with status `PAID` contribute to paid principal, paid interest, paid amount, and completed-installment metrics.
- `PENDING`, `OVERDUE`, `WAIVED`, and `ADJUSTED` installments remain in outstanding metrics in v1.
- Principal progress is calculated in integer basis points from paid principal divided by original principal, rounded half-up and clamped to `0..10_000`.
- Principal remaining equals original principal minus paid principal.
- Interest remaining and scheduled amount remaining are derived from installments not marked `PAID`.
- Next due date is the earliest due date among installments not marked `PAID`.
- Projected payoff date is the latest due date among installments not marked `PAID`; when every installment is paid, it is the final persisted schedule date.
- Empty schedules are valid and show zero paid metrics, full principal remaining, and no projected date.
- All schedule money must use the loan principal currency.
- Analytics are derived only. They do not update installment status, balances, terms, schedules, or ledger entries.

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
3. Payoff progress derived from persisted installment statuses.
4. Principal paid and remaining.
5. Interest paid and remaining.
6. Completed installment count and projected payoff date.
7. Principal, annual rate, tenure, start date, status, linked loan account, and disbursed account.
8. Amortization schedule ordered by installment number.
9. Each installment shows due date, payment, principal, interest, opening balance, closing balance, and status.

## Tio UI Specification

- Reuse `TioAppBar`, `TioBottomNavigation`, `TioFloatingActionButton`, `TioCard`, `TioLoadingState`, `TioEmptyState`, `TioErrorState`, typography, shapes, and spacing tokens.
- Use a dialog for loan creation so the v1 workflow remains compact on Android and iOS.
- Use separate account-selection dialogs so account lists do not overlap the creation dialog.
- Use a dedicated details destination rather than expanding a card inline.
- Use text labels with every status and payoff metric; color must never be the only signal.
- Use compact stacked schedule cards at phone widths instead of a horizontally scrolling financial table.
- Place the `Payoff progress` card after the existing top summary and before `Loan terms`.
- Render payoff analytics as text-first rows; do not add a chart dependency or gesture model in v1.
- Format money, rates, percentages, and dates in the ViewModel/presentation mapping layer, not in Composables.
- Parse editable principal and percentage text in the ViewModel without floating-point money arithmetic.
- Convert percentage input to integer basis points with at most two decimal places.
- Do not calculate EMI, amortization, totals, outstanding principal, or payoff metrics inside UI or ViewModels; consume Application read models only.
- Do not expose payment, prepayment, refinance, recast, close, or payoff-posting actions in v1.

## Navigation Definition

- Keep `MainRoute.Loans` at path `loans` with `TioIconToken.Loan`.
- Keep typed `MainRoute.LoanDetails(loanId)` with path `loans/{loanId}` semantics and the Loan icon token.
- Keep both cases registered in `RootNavigationHost`.
- Selecting a loan card emits the typed details destination.
- Details back navigation returns to `MainRoute.Loans`.
- Loans remains outside the five-item primary bottom navigation; bottom navigation still renders for the list and can exit to primary destinations.
- The details screen omits bottom navigation to preserve a focused nested-destination hierarchy.
- Loan Payoff Analytics introduces no new destination.

### Loans Entry Point (#61)

- **Source surface:** the Accounts screen top app bar (`MainRoute.Accounts`, the main-graph start route).
- **Affordance:** one non-primary icon action using `TioIconToken.Loan`.
  - Place it before the existing Settings action.
  - Use the same minimum touch target and click pattern as the Settings action.
  - It is an action button, not a tab, bottom-navigation item, or FAB.
- **Destination:** exactly `RootRoute.Main(MainRoute.Loans)`. No new route, no loan ID, and no parameters.
- **Entry behavior:**
  - Always visible and enabled, including on a clean install with no accounts or loans.
  - The Loans list keeps its existing empty state and prerequisite guidance for a missing `LOAN_LINKED` account.
- **Back behavior:**
  - The Loans list app bar keeps no back arrow; it stays a top-level non-primary destination like Settings.
  - Android system back from Loans returns to `TioNavigationGraphs.root.mainEntry` (Accounts), matching Settings, instead of leaving the app.
  - System back from `MainRoute.LoanDetails` still returns to `MainRoute.Loans`.
- **Bottom navigation:**
  - The canonical five destinations (Dashboard, Accounts, Transactions, Categories, Budgets) are unchanged.
  - The Loans list keeps rendering them with no item selected.
  - Loan Details keeps omitting them.
- **Accessibility:**
  - The action exposes the content description `Loans` and a button role.
  - It is reachable in focus order after the Accounts title and before Settings.
  - It meets the minimum touch target and its meaning is not conveyed by the icon alone.
- **Relationship to Loan Details:** unchanged. Loan Details is reached only by selecting a loan card, or by an EMI reminder deep link with a known `loanId`.
- **No financial behavior change:** the entry only navigates. No loan, account, ledger, schedule, or reminder data is read or written by the affordance, and no engine, Application, repository, or SQLDelight change is involved.
- **Rejected for this slice:**
  - A Dashboard entry, because Dashboard is still a placeholder without an approved screenshot.
  - A sixth bottom-navigation item, which contradicts this note and the five-destination rule.
  - A new overflow/"more" menu system, because no approved Tio overflow component exists and one route does not justify one.
  - A tappable `Loan-linked` group row, because it is not reachable from a clean install and Accounts v1 groups are display-only.

## Intentional Deviations

- No checked-in Loan screenshot exists, so the implementation follows established Tio Ledger production components rather than reconstructing an unknown external layout.
- Creation uses an ISO date text field rather than a platform date picker because the shared UI has no approved multiplatform picker abstraction yet.
- Interest entry accepts a percentage with up to two decimals, while persistence uses integer basis points.
- The schedule uses stacked cards instead of a dense table for phone readability and accessibility.
- Loans is not added to the primary bottom navigation because the current five destinations are already allocated. The typed route is reached from a non-primary Accounts app-bar action (see Loans Entry Point). The reference product keeps loans inside Accounts, and this reuses the approved Settings entry pattern instead of inventing a new navigation system.
- Payoff progress uses accessible text rows instead of a chart because no approved chart reference or interaction model exists.
- Payment, prepayment, refinance, schedule recast, payoff posting, contractual editing, and closure controls remain absent because posting and reconciliation policies are not part of issue #29.
- V1 follows persisted installment statuses and does not infer payment completion from dates, balances, or external account activity.

## Accessibility Considerations

- The Accounts app-bar Loans entry exposes the content description `Loans`, a button role, and a minimum touch target. It is reachable by screen reader, keyboard, and switch access between the Accounts title and the Settings action.
- Loan cards expose a combined semantic description covering name, status, outstanding principal, EMI, remaining installments, and next due date.
- Add, back, account-selection, retry, cancel, and create controls have readable labels or content descriptions.
- All loading, empty, validation, persistence, repository, and calculation failures are conveyed as text.
- The payoff card exposes explicit progress, principal paid/remaining, interest paid/remaining, completed installments, and projected payoff labels in reading order.
- Progress meaning remains understandable without color or a visual chart.
- Schedule cards expose installment number, due date, payment, principal, interest, balances, and status in reading order.
- Long account/loan names wrap or truncate without obscuring financial values.
- Touch targets use Material components and existing Tio spacing.
- Light/dark previews preserve contrast and hierarchy.

## Functional Acceptance Checklist

- [ ] Loading state is visible.
- [ ] Empty state offers an add-loan action.
- [ ] Repository or payoff-calculation failure offers retry.
- [ ] Populated list shows persisted loan summaries in deterministic order.
- [ ] Add dialog validates name, principal, percentage rate, tenure, date, and account selections.
- [ ] Account selectors expose only eligible active accounts for each role.
- [ ] Currency mismatch and same-account selections remain visibly rejected.
- [ ] Valid creation persists the loan and complete schedule atomically.
- [ ] Successful creation refreshes the list and shows feedback.
- [ ] The Accounts app bar exposes a `Loans` action that opens `MainRoute.Loans` from a clean install, with no loan ID.
- [ ] The Loans entry does not change the five primary bottom-navigation destinations; Loans shows them with no item selected.
- [ ] Android system back from Loans returns to Accounts; system back from Loan Details returns to Loans.
- [ ] The Loans entry has an accessible name, a button role, and a minimum touch target, and it is reachable in focus order.
- [ ] Selecting a loan opens typed details navigation.
- [ ] Details show contractual terms, summaries, account labels, and persisted schedule rows.
- [ ] Payoff analytics use only persisted loan terms and installment rows.
- [ ] Only `PAID` installments contribute to paid metrics.
- [ ] Other installment statuses remain in outstanding metrics.
- [ ] Principal progress uses integer basis points and deterministic rounding.
- [ ] Principal paid/remaining and interest paid/remaining are visible.
- [ ] Completed installments and projected payoff date are visible.
- [ ] Empty schedules remain valid and readable.
- [ ] UI/ViewModels do not access repositories, SQLDelight, or analytics calculators.
- [ ] No UI-side EMI, amortization, interest, outstanding, or payoff calculation exists.
- [ ] No financial history or schedule mutation is available from the payoff card.
- [ ] Navigation, Application, calculator, ViewModel, and Bootstrap behavior are test-covered.
- [ ] Light and dark previews cover populated payoff state.
- [ ] Pixel review confirms phone-width readability in light and dark themes.

## Pixel Review Plan

- Verify compact list cards at common phone widths.
- Verify long loan/account names do not collide with monetary values.
- Verify creation dialog remains usable with validation messages and smaller heights.
- Verify the payoff card remains readable with large text and long currency values.
- Verify the schedule remains readable for 1-, 12-, and long-tenure loans.
- Verify zero-interest and non-zero-interest values use the same hierarchy.
- Verify paid/pending labels remain distinguishable without relying on color.
- Verify the projected payoff date and completed-installment row do not overlap at phone widths.

## Approval Record

The Loans Entry Point section (#61) is a proposed reference decision. It becomes approved when its documentation PR is merged by the repository owner; production navigation is not implemented before that.

Issue #12 milestone execution and the user's earlier `go` approval authorize the Loan Creation and Details fallback package. Issue #29 and the user's explicit `Next go` authorization approve the narrow read-only Loan Payoff Analytics extension documented here. A future approved screenshot may refine visual styling or introduce a separately approved chart, but it must not change the validated financial, persistence, Application, status-classification, or navigation contracts without an explicit follow-up decision.
