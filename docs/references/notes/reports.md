# Reports Screen Reference Note

## Screen

Reports Screen v1 with Cash-flow analytics v1

## Reference Readiness

- Checked-in screenshot reference: unavailable at milestone start.
- Approved fallback product sources: issue #19 for Spending Analytics / Reports Screen v1 and issue #26 for Cash-flow analytics v1.
- Approved fallback visual source: the existing production Accounts, Transactions, Budgets, Categories, Reports, and Tio design-system patterns already checked into `shared:ui`.
- Technical reference: no proprietary code, XML, resources, assets, colors, dimensions, or implementation details were used.

The fallback package is sufficient because it fixes the workflow, information hierarchy, period controls, state handling, deterministic bucket rules, and financial boundaries without inventing a proprietary layout. Reports stays familiar to personal-finance users by leading with period selection, top-line totals, cash flow over time, and deterministic category/account breakdowns.

## Workflow Summary

1. Open Reports from the registered typed `MainRoute.Reports` destination.
2. Review the current spending period using `Week`, `Month`, or `Year`.
3. See deterministic start and end dates for the selected period.
4. Review income, expense, and net totals grouped by currency.
5. Review income, expense, and net cash flow over time for the same currency and period.
6. Review expense totals by category for the same currency and period.
7. Review expense totals by spending account for the same currency and period.
8. Retry from the same screen if repository or calculation loading fails.
9. Exit to another primary destination through the existing typed bottom navigation.

## Information Hierarchy

1. App bar title.
2. Period filter chips.
3. Selected period date range.
4. Currency-scoped income, expense, and net summary cards.
5. Cash flow over time for each currency.
6. Expense breakdown by category.
7. Expense breakdown by account.
8. Empty, loading, and error states.

## Tio UI Specification

- Use `TioAppBar`, `TioBottomNavigation`, `TioCard`, `TioSummaryCard`, `TioFilterChip`, loading/empty/error components, typography, shapes, and spacing tokens already present in `shared:ui`.
- Keep period calculations, filtering, grouping, bucketing, and money aggregation outside Compose and outside the ViewModel.
- Use integer minor units only. Do not use `Float` or `Double` for money.
- Keep multi-currency data separated into distinct sections. Do not infer exchange rates or convert currencies in v1.
- Limit v1 spending and cash-flow math to `INCOME` and `EXPENSE` transaction types. Do not classify transfers, loan disbursements, repayments, or adjustments as spending or cash-flow totals.
- Use the existing `[startInclusive, endExclusive)` report-window semantics in the selected timezone.
- Use seven daily buckets for `Week`, calendar-day buckets for `Month`, and twelve calendar-month buckets for `Year`.
- Include zero-value buckets within a populated currency section so the time sequence remains continuous.
- Render cash flow as accessible text rows showing bucket label, income, expense, and net. Do not add a chart dependency in v1.
- Use original labels and summaries derived from Tio Ledger data. Do not reproduce proprietary screenshots, colors, assets, or copied layout structures.

## Navigation Definition

- Keep `MainRoute.Reports` with path `reports` and `TioIconToken.Analytics`.
- Keep Reports registered in the main graph and outside the five-item primary bottom navigation.
- Keep the production `ReportsRoute`; no additional nested cash-flow destination is introduced.
- The Reports screen renders the standard primary bottom navigation so users can exit to existing primary destinations through the typed `RootRoute.Main` callback.

## Intentional Deviations

- No checked-in screenshot exists for Reports or Cash-flow analytics at milestone start, so the screen uses the existing Tio card-and-summary vocabulary instead of guessing a proprietary chart layout.
- Reports remains outside the five-item primary bottom navigation because Budgets currently occupies the fifth slot and that navigation contract is already validated.
- V1 shows deterministic summary cards, text-first cash-flow rows, and breakdown lists rather than charts. This keeps the screen accessible and original while preserving the required analytics information hierarchy.
- V1 separates currencies instead of converting them because the product does not define an exchange-rate source or a base-currency policy.
- V1 excludes transfers, loan disbursements, repayments, and adjustments because the approved scope limits Reports analytics to ledger-backed income and expense history.
- V1 includes zero-value buckets only for currencies that have qualifying activity within the selected report window; it does not invent currencies from account metadata.

## Accessibility Considerations

- Period controls expose explicit text labels and selected state.
- Summary meaning is never communicated by color alone; each amount keeps a text label.
- Every cash-flow row exposes a bucket label plus explicit `Income`, `Expense`, and `Net` labels.
- Positive, negative, and zero net meaning remains understandable without relying on tone or color.
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
- [ ] Weekly cash flow renders seven local-calendar-day rows.
- [ ] Monthly cash flow renders one row for every local-calendar day in the selected month.
- [ ] Yearly cash flow renders twelve local-calendar-month rows.
- [ ] Zero-value rows remain visible inside a populated currency section.
- [ ] Every cash-flow row shows bucket label, income, expense, and net.
- [ ] Expense totals by category are visible for each populated currency section.
- [ ] Expense totals by account are visible for each populated currency section.
- [ ] Transfers, loan disbursements, repayments, and adjustments do not affect spending or cash-flow totals.
- [ ] Navigation route remains test-covered and no new nested route is introduced.
- [ ] Compose and ViewModel do not access repositories, SQLDelight, or analytics calculation logic directly.
- [ ] Light and dark previews cover a populated cash-flow state.
- [ ] Pixel review confirms readable summary cards, cash-flow rows, and breakdown rows at phone width.
- [ ] Accessibility review confirms labels, selected period state, and text-only comprehension.

## Pixel Review Plan

- Validate light and dark previews at common phone widths.
- Verify three summary cards remain readable without clipping.
- Verify cash-flow labels and income/expense/net rows remain readable without horizontal overlap.
- Verify long category or account names truncate cleanly without overlapping amounts.
- Verify multi-currency sections stay visually separated and scan quickly.
- Verify monthly lists remain scrollable and do not hide category/account sections.

## Approval Record

Issue #19 milestone execution and the user's earlier `go` approval authorize the Spending Analytics fallback package. Issue #26 and the user's explicit `Next go` authorization approve the narrow Cash-flow analytics extension documented here. A future approved screenshot may refine visual styling or introduce a separately approved chart, but it must not change the validated analytics boundaries, deterministic bucket semantics, multi-currency separation, typed navigation, or immutable-history contracts without an explicit follow-up decision.
