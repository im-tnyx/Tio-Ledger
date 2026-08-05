# Cash-flow Analytics v1

Status: Ready for Review
Objective: Extend Reports v1 with deterministic, per-currency cash-flow buckets derived from immutable transaction history.
Branch: `feat/cash-flow-analytics-v1`
Scope: `shared/analytics`, `shared/application`, `shared/ui`, focused CI/tests, and Reports milestone documentation
Created: `2026-08-04`
Last Updated: `2026-08-04`
Issue: `https://github.com/im-tnyx/Tio-Ledger/issues/26`
Pull Request: `https://github.com/im-tnyx/Tio-Ledger/pull/27`

## Required Context

- `.ai/core/architecture.md`
- `.ai/core/coding-rules.md`
- `.ai/core/financial-rules.md`
- `.ai/core/ui-rules.md`
- `.ai/core/workflow-rules.md`
- `AGENTS.md`
- `docs/product-requirements.md`
- `docs/implementation-roadmap.md`
- `docs/references/notes/reports.md`
- `docs/engineering-guidelines.md`
- `docs/definition-of-done.md`
- `.github/PUSH_TEMPLATE.md`

## Constraints

- Reuse immutable `TransactionHistoryRepository` reads and existing Reports period windows.
- Include only `INCOME` and `EXPENSE`; exclude transfers, loan operations, repayments, and adjustments.
- Keep currencies separate and never invent FX conversion.
- Use integer minor units and existing `Money` arithmetic only.
- Keep bucketing and aggregation outside UI and ViewModel.
- No SQLDelight schema, migration, ledger posting, balance mutation, new route, or chart dependency.
- Preserve existing spending totals and category/account breakdown contracts.

## Decisions

- Extend existing Reports analytics instead of introducing a second repository read path.
- Use daily buckets for weekly/monthly periods and monthly buckets for yearly periods.
- Include zero-value buckets in populated currency sections for continuous time-series presentation.
- Render accessible text rows before considering graphical charts in a later approved slice.
- Add direct `shared:analytics` compile/test coverage to CI and the push checklist because calculator tests were not previously part of the required job command.

## Progress

- [x] Inspect merged repository state and confirm idle task pointer.
- [x] Review canonical roadmap, product requirements, Reports reference note, and current analytics/application/UI contracts.
- [x] Create issue #26 with approved v1 boundaries and acceptance criteria.
- [x] Create dedicated feature branch from updated `main`.
- [x] Extend analytics models/calculation and add focused tests.
- [x] Extend Application mapping/use-case tests.
- [x] Extend Reports UI state, ViewModel mapping, screen, previews, and tests.
- [x] Update Reports reference note and milestone documentation.
- [x] Add direct analytics validation to CI and `.github/PUSH_TEMPLATE.md`.
- [x] Open draft PR #27.
- [x] Pass final status-inclusive head checks and complete final diff review.
- [x] Confirm PR #27 has no unresolved review threads or comments.
- [ ] Merge only after explicit user authorization.

## Validation

GitHub Actions run `30914890489` on status-inclusive head `5e68c3c2653d39905dc2ee97268e2c6186eb953a`:

- `Targeted KMP validation`: PASS.
  - Shared metadata compilation including `shared:analytics`: PASS.
  - Critical tests including `shared:analytics:test`: PASS.
  - `ktlintCheck`: PASS.
  - `detekt`: PASS.
- `SQLDelight migration verification`: PASS.

Earlier validation:

- Run `30914349499` on implementation/tooling head `9905a2d8fdbef97beaf685af8c475a02d2a93c41`: PASS for both required jobs with direct analytics coverage.
- Run `30914096107` on the earlier full implementation head: PASS before direct analytics CI coverage was added.
- Local `git diff --check`: not available because the connected execution environment has no local checkout.

## Changed Files

- `.ai/current.md`
- `.ai/tasks/shared/local-20260804-cash-flow-analytics-v1.md`
- `.github/PUSH_TEMPLATE.md`
- `.github/workflows/ci.yml`
- `README.md`
- `docs/README.md`
- `docs/implementation-roadmap.md`
- `docs/references/notes/reports.md`
- `shared/analytics/src/commonMain/kotlin/com/tioledger/analytics/SpendingAnalyticsCalculator.kt`
- `shared/analytics/src/commonTest/kotlin/com/tioledger/analytics/SpendingAnalyticsCalculatorTest.kt`
- `shared/application/src/commonMain/kotlin/com/tioledger/application/usecase/analytics/SpendingAnalyticsUseCases.kt`
- `shared/application/src/commonTest/kotlin/com/tioledger/application/usecase/analytics/SpendingAnalyticsUseCaseTest.kt`
- `shared/ui/src/commonMain/kotlin/com/tioledger/ui/reports/ReportsPreviews.kt`
- `shared/ui/src/commonMain/kotlin/com/tioledger/ui/reports/ReportsScreen.kt`
- `shared/ui/src/commonMain/kotlin/com/tioledger/ui/reports/ReportsUiState.kt`
- `shared/ui/src/commonMain/kotlin/com/tioledger/ui/reports/ReportsViewModel.kt`
- `shared/ui/src/commonTest/kotlin/com/tioledger/ui/reports/ReportsViewModelTest.kt`

## Next Action

Review PR #27 and merge only when explicitly requested. After merge, perform the documented post-merge synchronization and archive this task before starting Loan payoff analytics.
