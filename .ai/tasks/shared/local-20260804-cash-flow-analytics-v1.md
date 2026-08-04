# Cash-flow Analytics v1

Status: In Progress
Objective: Extend Reports v1 with deterministic, per-currency cash-flow buckets derived from immutable transaction history.
Branch: `feat/cash-flow-analytics-v1`
Scope: `shared/analytics`, `shared/application`, `shared/ui`, focused DI/tests, and Reports reference documentation
Created: `2026-08-04`
Last Updated: `2026-08-04`
Issue: `https://github.com/im-tnyx/Tio-Ledger/issues/26`

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

## Progress

- [x] Inspect merged repository state and confirm idle task pointer.
- [x] Review canonical roadmap, product requirements, Reports reference note, and current analytics/application/UI contracts.
- [x] Create issue #26 with approved v1 boundaries and acceptance criteria.
- [x] Create dedicated feature branch from updated `main`.
- [ ] Extend analytics models/calculation and add focused tests.
- [ ] Extend Application mapping/use-case tests.
- [ ] Extend Reports UI state, ViewModel mapping, screen, previews, and tests.
- [ ] Update Reports reference note and milestone documentation where required.
- [ ] Run CI-equivalent validation and review final diff.
- [ ] Open draft PR and advance only after final-head checks pass.

## Validation

- Not run yet.

## Changed Files

- `.ai/tasks/shared/local-20260804-cash-flow-analytics-v1.md`

## Next Action

Implement the deterministic analytics bucket model and focused calculator tests before changing Application or UI contracts.
