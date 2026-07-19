# Loan Creation and Loan Details v1 Progress

Status: Engine and Domain validated; Data/SQLDelight foundation implemented — local validation pending
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

### Remaining executable gaps

- `shared:application` does not depend on `shared:loan-engine` and contains no loan use cases.
- Bootstrap/Koin does not depend on or register the loan engine, loan repository, or loan use cases.
- The posting engine supports income, expense, transfer, opening balance, and adjustment only; loan disbursement, EMI, and prepayment posting strategies are not implemented.
- `MainRoute.Loans` still resolves to the generic placeholder in `RootNavigationHost` and is not a primary bottom-navigation destination.
- `docs/references/notes/loan.md` exists only as a TBD placeholder; no approved checked-in Loan screenshot was found during the audit.

### Schema decision

No schema change is required for the baseline v1 workflow. Currency is derived from the linked loan account and must match the disbursed account. Existing schema fields are sufficient for a monthly reducing-balance loan and persisted amortization schedule.

## Implemented Loan-Engine Foundation

- Added a stateless `LoanCalculator` API with monthly fixed-rate reducing-balance terms, typed results, typed calculation errors, quotes, and installment rows.
- Added schedule-first EMI calculation using integer minor units only; no `Float`, `Double`, or platform-specific decimal API is used.
- Added annual basis-point to monthly-interest conversion with explicit half-up rounding at each minor-unit interest boundary.
- Added overflow-safe multiplication, addition, and subtraction paths with typed arithmetic failure.
- Added exact final-payment adjustment so the schedule closes at zero without negative balances.
- Added due-date generation by calendar month with month-end clamping and leap-year handling.
- Added golden tests for a representative 875-basis-point regression fixture, zero-interest loans, final-payment adjustment, month-end dates, half-up rounding, invalid inputs, overflow handling, and schedule invariants.
- The 875-basis-point value is only a regression fixture; production rates are supplied dynamically through `LoanTerms`.

## Implemented Domain Foundation

- Added schema-aligned Loan enums for interest type, EMI method, compounding frequency, payment frequency, loan status, and installment status.
- Added `Loan`, `LoanInstallment`, and `LoanDetails` Domain models.
- Added `LoanRepository` with deterministic list, details retrieval, and atomic loan-plus-schedule creation contract.
- Added `DomainEvent.LoanCreated`.
- Added typed `LoanNotFound` and `DuplicateLoanId` repository errors.

## Implemented Data And SQLDelight Foundation

- Added deterministic loan insert, schedule insert, loan-list, loan-details, and ordered-installment SQLDelight queries.
- Derived loan currency from the linked account rather than duplicating currency in the frozen loan schema.
- Added SQLDelight row mappers for Loan and LoanInstallment Domain models.
- Added `SQLDelightLoanRepository` with deterministic list/details reads and typed missing/duplicate results.
- Persisted the loan and complete baseline schedule in one database transaction.
- Added schedule-ownership validation so installments cannot be persisted against another loan ID.
- Added integration coverage for deterministic ordering, details reconstruction, typed duplicate/missing errors, and full rollback after an installment insert failure.
- No schema or migration change was introduced.

## Validated Engine And Domain Gate

```text
Metadata compilation
BUILD SUCCESSFUL in 1m 18s
13 actionable tasks: 4 executed, 9 up-to-date

Loan-engine tests
BUILD SUCCESSFUL in 55s
104 actionable tasks: 15 executed, 89 up-to-date

ktlintCheck detekt
BUILD SUCCESSFUL in 20s
74 actionable tasks: 4 executed, 70 up-to-date

git diff --check
(no output)

git status
clean and up to date
```

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

## Remaining Implementation Sequence

1. Validate the Data/SQLDelight foundation locally.
2. Add Application create/list/details use cases with account/currency/status validation.
3. Register engine/repository/use cases in Bootstrap/Koin diagnostics.
4. Resolve the approved Loan UI reference note before production Compose work.
5. Add loan list/create/details UI, typed navigation, previews, and tests.
6. Run final metadata, tests, static analysis, migration, and repository-integrity gates.

## Current Validation Gate

```text
./gradlew :shared:database:compileKotlinMetadata :shared:data:compileKotlinMetadata --no-daemon --console=plain --stacktrace
./gradlew :shared:data:test --no-daemon --console=plain --stacktrace
./gradlew ktlintCheck detekt --no-daemon --console=plain --stacktrace
./gradlew :shared:database:verifyCommonMainTioLedgerDatabaseMigration --no-daemon --no-parallel --max-workers=1 --console=plain --stacktrace
git diff --check
git status
```

## Architecture Constraints

- UI must use Application use cases only.
- Loan-engine calculations must not run inside Composables or ViewModels.
- Money and rate calculations must remain deterministic and avoid floating-point money arithmetic.
- Ledger history remains immutable and authoritative.
- Baseline loan creation must not silently invent disbursement/payment transactions.
- Schema changes require a verified blocker and explicit review.
- Kotlin Multiplatform `commonMain` compatibility must be preserved.
