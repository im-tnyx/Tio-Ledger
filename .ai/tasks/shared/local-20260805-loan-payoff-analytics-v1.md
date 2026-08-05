# Loan Payoff Analytics v1

Status: In Progress
Objective: Extend Loan Details with deterministic, read-only payoff progress derived from persisted loan terms and installment rows.
Branch: `feat/loan-payoff-analytics-v1`
Scope: `shared/analytics`, `shared/application`, `shared/bootstrap`, `shared/ui`, focused tests, and Loan reference/milestone documentation
Created: `2026-08-05`
Last Updated: `2026-08-05`
Issue: `https://github.com/im-tnyx/Tio-Ledger/issues/29`

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

## Progress

- [x] Merge Cash-flow Analytics v1 and complete post-merge task archival.
- [x] Verify `main` idle state.
- [x] Inspect canonical product, architecture, Loan Engine, Definition of Done, roadmap, and Loan reference documents.
- [x] Inspect existing Loan Engine, Domain, Application, Bootstrap, and Loan Details UI contracts.
- [x] Create issue #29 with approved scope and financial semantics.
- [x] Create dedicated feature branch from updated `main`.
- [ ] Implement payoff analytics calculator and focused tests.
- [ ] Extend Application mapping and tests.
- [ ] Register calculator/use-case graph in Bootstrap and validate DI.
- [ ] Extend Loan Details UI state, mapping, screen, previews, and tests.
- [ ] Update Loan reference and milestone documentation.
- [ ] Run exact-head CI-equivalent validation and review final diff.
- [ ] Open PR and advance only after checks pass.

## Validation

- Not run yet.

## Changed Files

- `.ai/tasks/shared/local-20260805-loan-payoff-analytics-v1.md`

## Next Action

Implement the pure payoff analytics calculator and focused regression tests before changing Application or UI contracts.
