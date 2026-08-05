# Loan Payoff Analytics v1

Status: Ready for Review
Objective: Extend Loan Details with deterministic, read-only payoff progress derived from persisted loan terms and installment rows.
Branch: `feat/loan-payoff-analytics-v1`
Scope: `shared/analytics`, `shared/application`, `shared/bootstrap`, `shared/ui`, focused tests, and Loan reference/milestone documentation
Created: `2026-08-05`
Last Updated: `2026-08-05`
Issue: `https://github.com/im-tnyx/Tio-Ledger/issues/29`
Pull Request: `https://github.com/im-tnyx/Tio-Ledger/pull/30`

## Required Context

- `.ai/core/architecture.md`
- `.ai/core/coding-rules.md`
- `.ai/core/financial-rules.md`
- `.ai/core/ui-rules.md`
- `.ai/core/workflow-rules.md`
- `AGENTS.md`
- `README.md`
- `docs/README.md`
- `docs/product-requirements.md`
- `docs/architecture.md`
- `docs/loan-engine-design.md`
- `docs/engineering-guidelines.md`
- `docs/definition-of-done.md`
- `docs/implementation-roadmap.md`
- `docs/references/notes/loan.md`
- `.github/PUSH_TEMPLATE.md`

## Constraints

- Reuse `LoanRepository.findDetails()` and the persisted schedule.
- Keep calculations in `shared:analytics`; UI and ViewModel consume immutable Application results.
- Treat only `PAID` installments as completed.
- Keep all other installment statuses outstanding in v1.
- Use integer minor units and existing `Money` arithmetic only.
- No prepayment/recast/refinance simulation, payment posting, schedule mutation, loan closure action, schema change, new route, or chart dependency.
- Preserve current Loan Overview, creation, navigation, loading/error, and amortization schedule behavior.

## Decisions

- Extend existing Loan Details instead of adding a second analytics destination.
- Calculate principal-based progress in basis points clamped to `0..10_000`.
- Derive paid and remaining principal, interest, payment, installment counts, next due date, and projected payoff date from persisted rows.
- Keep the presentation text-first and accessible; no graphical chart in v1.
- Empty schedules remain valid and produce zero paid metrics with full principal remaining.
- Keep the Application DTO owned by `shared:application`; analytics-domain models do not leak into UI contracts.
- Keep bottom-navigation defects out of this feature scope and track them separately in issue #34.

## Progress

- [x] Merge Cash-flow Analytics v1 and complete post-merge task archival.
- [x] Verify `main` idle state.
- [x] Inspect canonical product, architecture, Loan Engine, Definition of Done, roadmap, and Loan reference documents.
- [x] Inspect existing Loan Engine, Domain, Application, Bootstrap, and Loan Details UI contracts.
- [x] Create issue #29 with approved scope and financial semantics.
- [x] Create dedicated feature branch from updated `main`.
- [x] Implement payoff analytics calculator and focused tests.
- [x] Extend Application mapping and tests.
- [x] Register calculator/use-case graph in Bootstrap and validate DI.
- [x] Extend Loan Details UI state, mapping, screen, previews, and tests.
- [x] Update Loan reference and milestone documentation.
- [x] Run exact-head CI-equivalent validation and review final implementation diff.
- [x] Open PR #30 and resolve the formatting-only CI blocker.

## Validation

GitHub Actions CI run #339 on implementation head `62b21b31f0cf19cfba43898251b9f8051023d6c4` passed:

- Shared metadata compilation: success.
- Focused Analytics, Application, Bootstrap, and UI critical tests: success.
- `ktlintCheck`: success.
- `detekt`: success.
- SQLDelight migration verification: success.

The final documentation-only head must retain the same green gates before PR #30 is marked Ready for Review.

## Changed Files

- `.ai/current.md`
- `.ai/tasks/shared/local-20260805-loan-payoff-analytics-v1.md`
- `.github/PUSH_TEMPLATE.md`
- `.github/workflows/ci.yml`
- `README.md`
- `docs/README.md`
- `docs/implementation-roadmap.md`
- `docs/references/notes/loan.md`
- `shared/analytics/src/commonMain/kotlin/com/tioledger/analytics/LoanPayoffAnalyticsCalculator.kt`
- `shared/analytics/src/commonTest/kotlin/com/tioledger/analytics/LoanPayoffAnalyticsCalculatorTest.kt`
- `shared/application/src/commonMain/kotlin/com/tioledger/application/usecase/loan/LoanPayoffAnalyticsUseCase.kt`
- `shared/application/src/commonTest/kotlin/com/tioledger/application/usecase/loan/LoanPayoffAnalyticsUseCaseTest.kt`
- `shared/bootstrap/src/androidUnitTest/kotlin/com/tioledger/bootstrap/TioApplicationBootstrapTest.kt`
- `shared/bootstrap/src/commonMain/kotlin/com/tioledger/bootstrap/di/TioModules.kt`
- `shared/ui/src/commonMain/kotlin/com/tioledger/ui/loans/LoansPreviews.kt`
- `shared/ui/src/commonMain/kotlin/com/tioledger/ui/loans/LoansScreen.kt`
- `shared/ui/src/commonMain/kotlin/com/tioledger/ui/loans/LoansViewModels.kt`
- `shared/ui/src/commonTest/kotlin/com/tioledger/ui/loans/LoansViewModelsTest.kt`

## Next Action

Confirm the final documentation head remains green, complete the PR #30 diff/review-state audit, and merge only after explicit approval. After merge, synchronize `main`, archive this task, reset `.ai/current.md`, and then begin the separately tracked bottom-navigation issue #34.
