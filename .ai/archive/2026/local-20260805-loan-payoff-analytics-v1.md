# Loan Payoff Analytics v1

Status: Merged
Objective: Extend Loan Details with deterministic, read-only payoff progress derived from persisted loan terms and installment rows.
Branch: `feat/loan-payoff-analytics-v1`
Scope: `shared/analytics`, `shared/application`, `shared/bootstrap`, `shared/ui`, focused tests, and Loan reference/milestone documentation
Created: `2026-08-05`
Completed: `2026-08-05`
Issue: `https://github.com/im-tnyx/Tio-Ledger/issues/29`
Pull Request: `https://github.com/im-tnyx/Tio-Ledger/pull/30`
Merge Commit: `8f57b64f06b56c572ea32e5c9b2250a559bf5c3e`

## Constraints Preserved

- Reused `LoanRepository.findDetails()` and the persisted schedule.
- Kept calculations in `shared:analytics`; UI and ViewModel consume immutable Application results.
- Treated only `PAID` installments as completed.
- Kept all other installment statuses outstanding in v1.
- Used integer minor units and existing `Money` arithmetic only.
- Added no prepayment/recast/refinance simulation, payment posting, schedule mutation, loan closure action, schema change, new route, or chart dependency.
- Preserved current Loan Overview, creation, navigation, loading/error, and amortization schedule behavior.

## Decisions

- Extended existing Loan Details instead of adding a second analytics destination.
- Calculated principal-based progress in basis points clamped to `0..10_000`.
- Derived paid and remaining principal, interest, payment, installment counts, next due date, and projected payoff date from persisted rows.
- Kept the presentation text-first and accessible; no graphical chart in v1.
- Empty schedules remain valid and produce zero paid metrics with full principal remaining.
- Kept the Application DTO owned by `shared:application`; analytics-domain models do not leak into UI contracts.
- Kept bottom-navigation defects out of this feature scope and tracked them separately in issue #34.

## Completed Work

- [x] Implemented payoff analytics calculator and focused tests.
- [x] Extended Application mapping and tests.
- [x] Registered calculator/use-case graph in Bootstrap and validated DI.
- [x] Extended Loan Details UI state, mapping, screen, previews, and tests.
- [x] Updated Loan reference and milestone documentation.
- [x] Updated CI to run direct Analytics and Bootstrap tests.
- [x] Resolved the formatting-only CI blocker.
- [x] Completed final diff, branch, review-thread, and safety audit.
- [x] Merged PR #30 and closed issue #29.

## Validation

Final GitHub Actions CI run #340 on head `11a7846fc6dfa378cfaf1edea1fc1a15d1c78169` passed:

- Shared metadata compilation.
- Focused Analytics, Application, Bootstrap, and UI critical tests.
- `ktlintCheck`.
- `detekt`.
- SQLDelight migration verification.

The branch was 0 commits behind `main`, mergeable, and had no unresolved review threads before merge.

## Outcome

Loan Payoff Analytics v1 is merged into `main` through PR #30. Issue #29 is closed as completed. Bottom-navigation wiring remains a separate UI defect tracked by issue #34.
