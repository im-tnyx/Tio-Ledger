# Budgets Screen Reference Note

## Screen

Budgets Screen v1

## Reference Readiness

- Checked-in screenshot reference: unavailable at milestone start.
- Approved fallback source: official Google Play listing for **Money Manager Expense & Budget** by Realbyte Inc.
- Supporting product source: issue #10 acceptance criteria and Tio Ledger's existing design system/navigation patterns.
- Technical reference: no proprietary code, XML, resources, assets, colors, dimensions, or implementation details were used.

The official listing describes budget management as a quick comparison of budget and expense through a graph. Tio Ledger preserves that information hierarchy with deterministic target, spent, remaining, and progress presentation while using original Compose Multiplatform implementation and existing design tokens.

## Workflow Summary

1. Open Budgets from the primary bottom navigation.
2. Review recurring budgets in deterministic name order.
3. Each budget shows category scope, period, target, spent, remaining, date range, progress, and status.
4. Use the add action to create a weekly, monthly, or yearly budget.
5. Select an expense category or an all-expenses scope.
6. Select an existing budget to edit its name, amount, currency, category scope, or period.
7. Validation and persistence failures remain inside the editor without corrupting saved data.
8. Successful create/update closes the editor and refreshes current-period summaries.

## Information Hierarchy

1. App bar title and add action.
2. Success or error feedback.
3. Budget cards ordered deterministically.
4. Budget name and category scope.
5. Current period label and date range.
6. Target, spent, and remaining values.
7. Progress indicator and status label.
8. Create/edit dialog with name, amount, ISO currency code, recurring period, and expense-category scope.

## Tio UI Specification

- Use `TioAppBar`, `TioBottomNavigation`, `TioFloatingActionButton`, `TioCard`, loading/empty/error components, typography, shapes, and spacing tokens already present in `shared:ui`.
- Use a Material 3 linear progress indicator fed by integer utilization permille calculated outside Compose.
- Clamp only the visual progress fraction to the indicator range; preserve the full utilization/status text for exceeded budgets.
- Use an editor dialog for both create and edit so the workflow remains compact and deterministic across Android and iOS hosts.
- Use a separate category-selection dialog to keep the editor usable when category count grows.
- Do not perform period, spend, remaining, or status calculations in UI.
- Do not use `Float` or `Double` for money values.

## Navigation Definition

- Add `MainRoute.Budgets` with path `budgets` and `TioIconToken.Budget`.
- Register Budgets in the main graph.
- Budgets temporarily occupies the fifth primary bottom-navigation slot previously used by the placeholder Reports destination.
- Reports remains registered in the main graph for future direct/overflow navigation.
- Selecting another bottom-navigation destination exits Budgets through the existing typed `RootRoute.Main` callback.
- No unsaved editor data is persisted when the editor is dismissed.

## Intentional Deviations

- The fallback source describes a graph but does not provide an approved checked-in Budgets screenshot. V1 uses accessible progress cards rather than reproducing an unknown proprietary chart layout.
- Reports is temporarily removed from primary bottom navigation because Material navigation guidance and the current component are designed for a compact primary destination set; Budgets is the implemented finance workflow while Reports remains a placeholder.
- Custom budget periods are not exposed in v1 because the approved workflow does not yet define explicit start/end editing, even though the frozen schema permits custom periods.
- Budget-period rows are not persisted in v1; current recurring windows are derived deterministically by the pure budget engine.

## Accessibility Considerations

- Every budget card exposes a combined semantic description containing name, category, period, spent, target, remaining, and status.
- Add and edit actions use explicit content descriptions.
- Progress is never communicated by color alone; a textual status and numeric amount comparison are always shown.
- Dialog controls remain keyboard/switch-access reachable and retain readable labels.
- Loading, empty, validation, persistence, and repository-failure states include text.

## Functional Acceptance Checklist

- [ ] Loading state is visible.
- [ ] Empty state offers add-budget action.
- [ ] Repository/engine failure state offers retry.
- [ ] Populated budgets show period, target, spent, remaining, progress, and status.
- [ ] Add dialog supports weekly, monthly, and yearly budgets.
- [ ] Add dialog supports all-expenses or an active expense-category scope.
- [ ] Valid create persists and refreshes the list.
- [ ] Existing budget opens in edit mode.
- [ ] Valid update persists and refreshes the list.
- [ ] Invalid/duplicate input remains visible without changing persisted data.
- [ ] Navigation route and primary entry are test-covered.
- [ ] UI/ViewModel do not access repositories, SQLDelight, or budget engine directly.
- [ ] Previews cover populated and editor states.
- [ ] Pixel review confirms readable compact cards at phone width.
- [ ] Accessibility review confirms labels, status text, and touch targets.

## Pixel Review Plan

- Validate phone-width light and dark previews.
- Verify long budget/category names truncate or wrap without overlapping amounts.
- Verify 0%, warning, reached, and exceeded progress states remain readable.
- Verify editor controls fit without clipping at common mobile widths.

## Approval Record

Issue #10 milestone execution and the user's `go` approval authorize the official Google Play listing as the fallback reference source for Budgets Screen v1. A future checked-in screenshot may refine visuals but must not change the validated architecture or financial behavior without an explicit follow-up decision.
