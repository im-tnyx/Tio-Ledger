# Loan Creation and Loan Details v1 Progress

Status: Engine and Domain foundation implemented — local validation pending
Issue: #12
Branch: `feat/loan-creation-details-v1`
PR: #13 (draft)

## Objective

Replace the Loans placeholder/navigation gap with production loan creation and loan details workflows backed by the Domain, Application, loan-engine, Data, SQLDelight, Bootstrap/Koin, and shared UI layers.

## Completed Audit

- [x] Reviewed existing loan-engine APIs, deterministic finance primitives, and tests.
- [x] Reviewed Domain loan models, events, repository contracts, and typed errors.
- [x] Reviewed frozen SQLDelight loan, installment, payment, prepayment, and ledger schema.
- [x] Reviewed Data repository adapters and transaction boundaries.
- [x] Reviewed Application loan orchestration and validation paths.
- [x] Reviewed Bootstrap/Koin registrations and diagnostics.
- [x] Reviewed `MainRoute.Loans`, root host behavior, and current UI placeholders.
- [x] Reviewed approved UI references and fallback-source requirements.
- [x] Defined the initial v1 create/details scope and explicit out-of-scope behavior.
- [x] Confirmed the verified architecture gaps before implementation.

## Audit Findings

### Existing foundations

- The frozen schema already defines `loans`, `emi_schedules`, `emi_payments`, and `loan_prepayments`.
- The schema supports a linked liability account, a disbursed asset account, integer principal/rate/fee values, monthly payment frequency, persisted schedule rows, payment status, and prepayment records.
- Domain accounting primitives already include `AccountType.LOAN_LINKED`, `TransactionType.LOAN_DISBURSEMENT`, `TransactionType.REPAYMENT`, `LedgerSourceType.EMI`, and `LedgerSourceType.PREPAYMENT`.
- `MainRoute.Loans` and the Loan icon token already exist and are registered in the main graph.
- The accepted loan-engine design and precision ADR require deterministic EMI/schedule calculations, integer minor-unit persistence, explicit rounding, and no floating-point money arithmetic.

### Missing executable paths found by audit

- `shared:loan-engine` was configured but contained no production calculator API or tests.
- No deterministic fixed-scale interest-rate helper existed in Core or Finance Engine.
- No Loan Domain entity, schedule model, repository contract, domain events, or typed loan errors existed.
- No SQLDelight loan query file, row mapper, Data repository, or atomic loan-plus-schedule persistence path exists.
- `shared:application` does not depend on `shared:loan-engine` and contains no loan use cases.
- Bootstrap/Koin does not depend on or register the loan engine, loan repository, or loan use cases.
- The posting engine supports income, expense, transfer, opening balance, and adjustment only; loan disbursement, EMI, and prepayment posting strategies are not implemented.
- `MainRoute.Loans` still resolves to the generic placeholder in `RootNavigationHost` and is not a primary bottom-navigation destination.
- `docs/references/notes/loan.md` exists only as a TBD placeholder; no approved checked-in Loan screenshot was found during the audit.

### Schema decision

No schema change is required for the baseline v1 workflow. Currency is derived from the linked loan account and must match the disbursed account. Existing schema fields are sufficient for a monthly reducing-balance loan and persisted amortization schedule.

## Completed Loan-Engine Foundation

- Added a narrow, stateless `LoanCalculator` API for monthly reducing-balance loans.
- Added typed loan terms, quote, installment, payment-frequency, result, and calculation-error models.
- Implemented schedule-first EMI discovery using deterministic integer arithmetic rather than `Double` or platform decimal APIs.
- Implemented annual basis-point to monthly-interest conversion with explicit half-up minor-unit rounding.
- Added overflow-safe addition, subtraction, multiplication, and multiply/divide calculation boundaries.
- Added final-payment adjustment so the generated schedule closes the balance exactly.
- Added calendar-month due dates that preserve the contractual day where possible and clamp to month end when required.
- Added golden tests for a standard 8.75% five-year loan, zero interest, month-end dates, final-payment adjustment, half-up rounding, validation errors, overflow, and schedule invariants.

## Completed Domain Foundation

- Added strongly typed Loan, LoanDetails, and persisted LoanInstallment models.
- Added fixed/floating/reducing/flat interest types and schema-aligned compounding, payment, status, and installment status enums.
- Added a narrow `LoanRepository` contract for deterministic list, details retrieval, and atomic loan-plus-schedule creation.
- Added `DomainEvent.LoanCreated`.
- Added typed `LoanNotFound` and `DuplicateLoanId` repository errors.

## Approved Initial V1 Scope

### Loan creation

- Name.
- Positive principal in linked-account currency.
- Non-negative annual interest rate represented in basis points.
- Positive tenure in months.
- Start date.
- Existing active `LOAN_LINKED` liability account.
- Existing active non-loan disbursed account with matching currency.
- Monthly payment frequency.
- Fixed-rate reducing-balance calculation with monthly interest accrual.
- Explicit half-up rounding at minor-unit schedule boundaries.
- Atomic persistence of the loan and its baseline EMI schedule.

### Loan list and details

- Loading, empty, repository-error, and populated loan-list states.
- Select a loan to open details.
- Principal, annual rate, tenure, start date, EMI, total interest, total payable, and linked accounts.
- Persisted installment schedule with due date, opening balance, EMI, principal, interest, closing balance, and status.
- No financial calculation inside UI or ViewModels.

### Explicitly out of scope for this milestone

- Recording disbursement ledger entries automatically.
- EMI payment posting and reconciliation.
- Prepayment entry/simulation and revised schedules.
- Floating-rate, flat-interest, quarterly, annual, moratorium, fee-financing, and custom due-day behavior.
- Editing contractual loan terms after creation.
- Loan closure and payoff actions.

These follow-up workflows require dedicated posting strategies and transactional policies so immutable ledger history remains authoritative.

## Implementation Sequence

1. [x] Add deterministic monthly reducing-balance loan-engine API, schedule-first EMI calculation, date rules, typed errors, and golden tests.
2. [x] Add Loan Domain models, repository contract, events, and typed repository errors.
3. [ ] Add SQLDelight CRUD/schedule queries, mappers, and atomic Data repository integration tests.
4. [ ] Add Application create/list/details use cases with account/currency/status validation.
5. [ ] Register engine/repository/use cases in Bootstrap/Koin diagnostics.
6. [ ] Resolve the approved Loan UI reference note before production Compose work.
7. [ ] Add loan list/create/details UI, typed navigation, previews, and tests.
8. [ ] Run final metadata, tests, static analysis, migration, and repository-integrity gates.

## Architecture Constraints

- UI must use Application use cases only.
- Loan-engine calculations must not run inside Composables or ViewModels.
- Money and rate calculations must remain deterministic and avoid floating-point money arithmetic.
- Ledger history remains immutable and authoritative.
- Baseline loan creation must not silently invent disbursement/payment transactions.
- Schema changes require a verified blocker and explicit review.
- Kotlin Multiplatform `commonMain` compatibility must be preserved.

## Current Validation Gate

```text
./gradlew :shared:core:compileKotlinMetadata :shared:domain:compileKotlinMetadata :shared:loan-engine:compileKotlinMetadata --no-daemon --console=plain --stacktrace
./gradlew :shared:loan-engine:test --no-daemon --console=plain --stacktrace
./gradlew ktlintCheck detekt --no-daemon --console=plain --stacktrace
git diff --check
git status
```

## Remaining Validation Gates

```text
Focused application/data/UI tests
SQLDelight migration verification
Final metadata compilation
Final ktlintCheck and detekt
git diff --check
clean working tree
```
