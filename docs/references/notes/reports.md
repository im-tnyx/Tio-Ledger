# Reports Screen Reference Note

## Screen

Reports Screen v1

## Reference Readiness

- Checked-in screenshot reference: unavailable at milestone start.
- Approved fallback product source: issue #19 scope and acceptance criteria for Spending Analytics / Reports Screen v1.
- Approved fallback visual source: the existing production Accounts, Transactions, Budgets, Categories, and Tio design-system patterns already checked into `shared:ui`.
- Technical reference: no proprietary code, XML, resources, assets, colors, dimensions, or implementation details were used.

The fallback package is sufficient because it fixes the workflow, information hierarchy, period controls, state handling, and financial boundaries without inventing a proprietary layout. Reports v1 stays familiar to personal-finance users by leading with period selection, top-line totals, and deterministic category/account breakdowns.

## Workflow Summary

1. Open Reports from the registered typed `MainRoute.Reports` destination.
2. Review the current spending period using `Week`, `Month`, or `Year`.
3. See deterministic start and end dates for the selected period.
4. Review income, expense, and net totals grouped by currency.
5. Review expense totals by category for the same currency and period.
6. Review expense totals by spending account for the same currency and period.
7. Retry from the same screen if repository or calculation loading fails.
8. Exit to another primary destination through the existing typed bottom navigation.

## Information Hierarchy

1. App bar title.
2. Period filter chips.
3. Selected period date range.
4. Currency-scoped income, expense, and net summary cards.
5. Expense breakdown by category.
6. Expense breakdown by account.
7. Empty, loading, and error states.

## Tio UI Specification

- Use `TioAppBar`, `TioBottomNavigation`, `TioCard`, `TioSummaryCard`, `TioFilterChip`, loading/empty/error components, typography, shapes, and spacing tokens already present in `shared:ui`.
- Keep period calculations, filtering, grouping, and money aggregation outside Compose and outside the ViewModel.
- Use integer minor units only. Do not use `Float` or `Double` for money.
- Keep multi-currency data separated into distinct sections. Do not infer exchange rates or convert currencies in v1.
- Limit v1 cash-flow math to `INCOME` and `EXPENSE` transaction types. Do not classify transfers, loan disbursements, repayments, or adjustments as spending totals.
- Use original labels and summaries derived from Tio Ledger data. Do not reproduce proprietary screenshots, colors, assets, or copied layout structures.

## Navigation Definition

- Keep `MainRoute.Reports` with path `reports` and `TioIconToken.Analytics`.
- Keep Reports registered in the main graph and outside the five-item primary bottom navigation.
- Replace the placeholder host branch with a production `ReportsRoute`.
- The Reports screen renders the standard primary bottom navigation so users can exit to existing primary destinations through the typed `RootRoute.Main` callback.
- No new nested destination is introduced in v1.

## Intentional Deviations

- No checked-in screenshot exists for Reports at milestone start, so v1 uses the existing Tio card-and-summary vocabulary instead of guessing a proprietary chart layout.
- Reports remains outside the five-item primary bottom navigation because Budgets currently occupies the fifth slot and that navigation contract is already validated.
- V1 shows deterministic summary cards and breakdown lists rather than charts. This keeps the screen accessible and original while preserving the required spending-analysis information hierarchy.
- V1 separates currencies instead of converting them because the product does not define an exchange-rate source or a base-currency policy.
- V1 excludes transfers, loan disbursements, repayments, and adjustments from spending totals because issue #19 scopes Reports to ledger-backed income and expense analytics.

## Accessibility Considerations

- Period controls expose explicit text labels and selected state.
- Summary meaning is never communicated by color alone; each amount keeps a text label.
- Currency sections remain readable in light and dark themes with large text scaling.
- Empty, loading, and error states include text instructions.
- Category and account breakdown rows expose both label and amount.

## Functional Acceptance Checklist

- [ ] Loading state is visible.
- [ ] Empty state explains that no spending data exists for the selected period.
- [ ] Repository or calculation failure state offers retry.
- [ ] `Week`, `Month`, and `Year` period filters are available.
- [ ] Period change refreshes the date range and report content deterministically.
- [ ] Income, expense, and net totals are derived from immutable transaction history.
- [ ] Multi-currency data renders as separate sections with no currency conversion.
- [ ] Expense totals by category are visible for each populated currency section.
- [ ] Expense totals by account are visible for each populated currency section.
- [ ] Transfers, loan disbursements, repayments, and adjustments do not affect spending totals.
- [ ] Navigation route is test-covered and no longer renders the generic placeholder.
- [ ] Compose and ViewModel do not access repositories, SQLDelight, or analytics calculation logic directly.
- [ ] Light and dark previews cover a populated state.
- [ ] Pixel review confirms readable summary cards and breakdown rows at phone width.
- [ ] Accessibility review confirms labels, selected period state, and text-only comprehension.

## Pixel Review Plan

- Validate light and dark previews at common phone widths.
- Verify three summary cards remain readable without clipping.
- Verify long category or account names truncate cleanly without overlapping amounts.
- Verify multi-currency sections stay visually separated and scan quickly.

## Approval Record

Issue #19 milestone execution and the user's `go` approval authorize this fallback reference package for Reports Screen v1. A future approved screenshot may refine visual styling, but it must not change the validated analytics boundaries, multi-currency separation, typed navigation, or immutable-history contracts without an explicit follow-up decision.
