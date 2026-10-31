# Transactions Daily View v1 — Stage A

Status: Published - Review Pending
Objective: Implement and test the immutable Application-owned Daily Transactions snapshot for a selected local calendar month.
Implementation Owner: Codex
Branch: `tnyx/transactions-daily-stage-a`
Base SHA: `b55569a8d39db675655b82c3faf82fd7dddb8b58`
Published Head SHA: `668a0dc6c616168c4b5f3101de58857e1b421305`
Pull Request: `#92 <https://github.com/im-tnyx/Tio-Ledger/pull/92>`
Scope: `shared/application` with necessary Application DI registration and regression tests.
Created: `2026-10-31`
Last Updated: `2026-11-01`

## Required Context

- `.ai/core/architecture.md`
- `.ai/core/coding-rules.md`
- `.ai/core/financial-rules.md`
- `.ai/core/workflow-rules.md`
- `docs/engineering-guidelines.md`
- `docs/definition-of-done.md`
- `docs/adr/0002-clean-architecture-mvvm.md`
- `docs/adr/0005-shared-pure-financial-engines.md`
- `docs/adr/0006-offline-first-local-source-of-truth.md`
- `docs/adr/0009-deterministic-money-interest-precision.md`
- `docs/adr/0012-financial-accuracy.md`
- `docs/adr/0013-ledger-first-architecture.md`

## Approved Scope And Constraints

- Scope: Application query, immutable read models, use case, DI registration, and focused regression tests.
- Non-goals: Compose/UI, navigation, domain semantics, persistence/schema, ledger writes, FX, and publication.
- Existing Reports split and cash-flow semantics must be reused or explicitly surfaced if incompatible; no alternate financial formula may be invented.

## Decisions

- Use the canonical first day of `LocalDate` as the selected month.
- Convert local month boundaries with the validated explicit IANA timezone.
- Preserve all transaction rows while counting only `INCOME` and `EXPENSE` in monthly cash-flow totals.

## Progress

- [x] Implement contract and use case.
- [x] Add regression tests and DI coverage.
- [x] Run focused and repository-required validation.
- [x] Commit and publish the approved Stage A scope for independent review.

## Validation Evidence

- Checks executed: `:shared:application:testDebugUnitTest --tests com.tioledger.application.usecase.transaction.DailyTransactionsUseCaseTest`; `:shared:application:test :shared:analytics:test`; `:shared:application:test :shared:bootstrap:testDebugUnitTest`; metadata compilation command from `.github/PUSH_TEMPLATE.md`; `:shared:database:verifyCommonMainTioLedgerDatabaseMigration`; `ktlintCheck`; `:shared:application:ktlintCheck :shared:bootstrap:ktlintCheck`; `detekt`; `git diff --check`.
- Checks skipped / unavailable: metadata compile tasks and SQLDelight migration verification reported `SKIPPED`; detekt reported shared modules `NO-SOURCE`; no emulator/device UI acceptance in Stage A.
- Independent-review correction evidence: the new pre-epoch regression first failed with the existing `SpendingAnalyticsCalculator` `IllegalArgumentException`, then passed after an explicit typed `month` validation in the Application use case. The deterministic `Asia/Kolkata` boundary/grouping regression passes alongside the existing UTC and `America/New_York` cases.
- Publication evidence: PR `#92` is Draft/Open at published head `668a0dc6c616168c4b5f3101de58857e1b421305`, based on `b55569a8d39db675655b82c3faf82fd7dddb8b58`; CI completed green for the published head.

## Changed Files

- `.ai/current.md`
- `.ai/tasks/shared/transactions-daily-stage-a.md`
- `shared/application/src/commonMain/kotlin/com/tioledger/application/usecase/transaction/TransactionHistoryUseCases.kt`
- `shared/application/src/commonMain/kotlin/com/tioledger/application/usecase/transaction/DailyTransactionsUseCases.kt`
- `shared/application/src/commonTest/kotlin/com/tioledger/application/usecase/transaction/DailyTransactionsUseCaseTest.kt`
- `shared/bootstrap/src/commonMain/kotlin/com/tioledger/bootstrap/di/TioModules.kt`
- `shared/bootstrap/src/androidUnitTest/kotlin/com/tioledger/bootstrap/TioApplicationBootstrapTest.kt`

## Review Evidence And Findings

- Review Status: `[x] REVIEW_PENDING  [ ] REVIEW_COMPLETE  [ ] OWNER_DEFERRED_REVIEW`
- Independent Reviewer: `im-tio`
- Review classification: `[x] Independent review  [ ] Self-review  [ ] Tool/bot assessment`
- Review decision: `[ ] Approved  [x] Changes requested  [ ] Comments / Non-binding`
- Owner Deferral Authorization: `None`
- Deferral Reason & Accepted Risks: `None`
- Follow-up Review Owner & Checkpoint: `im-tio re-review after the P2 handoff correction; owner merge gate remains pending.`
- Outstanding findings: `im-tio COMMENTED with P2: the task pointer and task evidence had stale publication state and used the base SHA as its inspected head. This docs-only correction records PR #92 and the published head; independent re-review is pending.`
- Remaining risks: `Existing first/source/primary split semantics remain intentionally unchanged; Stage B must preserve the Application contract and expose no financial arithmetic in UI. Stage A follows the repository's structural read-only List/data-class convention rather than providing deep immutable collections. Daily snapshot lists are newly constructed by filtering, mapping and grouping, so no mutable repository collection is aliased; this is an accepted limitation, not a deep immutability guarantee.`

## Next Action And Handoff

- Owner: `Codex`
- Next Action: `Resolve the P2 handoff finding on PR #92, request independent re-review from im-tio, confirm CI remains green, and wait for the owner merge gate. Do not merge or begin Stage B.`
