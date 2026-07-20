# Loan Creation and Loan Details v1 Progress

Status: Engine/Domain validated; Data/SQLDelight and Application foundations implemented — Application gate pending
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

## Architecture Findings

- The frozen schema already defines `loans`, `emi_schedules`, `emi_payments`, and `loan_prepayments`.
- Currency is derived from the linked loan account and must match the disbursed account.
- Existing accounting primitives include loan-linked accounts, loan disbursement/repayment transaction types, and EMI/prepayment ledger sources.
- `MainRoute.Loans` exists but still resolves to the generic placeholder.
- Loan posting, payment reconciliation, prepayment, and payoff policies remain intentionally out of scope.
- No schema change is required for baseline monthly fixed-rate reducing-balance creation and persisted schedules.

## Implemented Loan-Engine Foundation

- Added a stateless `LoanCalculator` API with typed terms, results, errors, quotes, and installment rows.
- Added deterministic schedule-first EMI calculation using integer minor units only.
- Added annual basis-point conversion and explicit half-up minor-unit interest rounding.
- Added overflow-safe arithmetic, exact final-payment adjustment, and clean zero-balance closure.
- Added calendar-month due dates with month-end clamping and leap-year handling.
- Added golden coverage for a representative 875-basis-point regression fixture, zero interest, final adjustment, date boundaries, rounding, invalid terms, overflow, and schedule invariants.
- The 875-basis-point value is test data only; production rates are supplied dynamically through `LoanTerms`.

## Implemented Domain Foundation

- Added schema-aligned loan interest, EMI method, compounding, payment, loan-status, and installment-status enums.
- Added `Loan`, `LoanInstallment`, and `LoanDetails` models.
- Added `LoanRepository` for deterministic list/details reads and atomic loan-plus-schedule creation.
- Added `DomainEvent.LoanCreated`.
- Added typed `LoanNotFound` and `DuplicateLoanId` errors.

## Implemented Data And SQLDelight Foundation

- Added deterministic loan insert, schedule insert, loan-list, loan-details, and ordered-installment queries.
- Derived currency from the linked account without duplicating the frozen schema.
- Added SQLDelight-to-Domain Loan and LoanInstallment mappers.
- Added `SQLDelightLoanRepository` with typed missing and duplicate results.
- Persisted each loan and its complete baseline schedule in one database transaction.
- Added schedule ownership validation.
- Added integration coverage for ordering, details reconstruction, duplicate/missing errors, and rollback after an installment insert failure.
- No schema or migration change was introduced.

## Implemented Application Foundation

- Connected `shared:application` to `shared:loan-engine`.
- Added `CreateLoanUseCase`, `ListLoansUseCase`, and `GetLoanDetailsUseCase`.
- Added immutable `LoanOverview` and `LoanDetailsView` read models derived from persisted schedule rows.
- Added normalized ID/name validation, positive principal, non-negative basis-point rate, positive tenure, and timestamp validation.
- Required an active `LOAN_LINKED` account and a different active non-loan asset disbursed account.
- Required matching account currencies and derived principal currency from the linked loan account.
- Converted date-only loan and installment dates to deterministic UTC start-of-day persistence values.
- Generated installment IDs through the shared `IdGenerator`.
- Mapped engine calculation failures to typed Application validation or ledger failures.
- Emitted `DomainEvent.LoanCreated` only after successful atomic persistence.
- Added focused tests for successful creation, generated schedules, account eligibility, currency mismatch, calculator failure, persisted summaries, deterministic ordering, and repository failures.

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

## Latest Data Formatting Regression Gate

```text
ktlintCheck detekt
BUILD SUCCESSFUL in 47s
74 actionable tasks: 3 executed, 71 up-to-date

git diff --check
(no output)

git status
clean and up to date
```

## Approved Initial V1 Scope

### Loan creation

- Name, positive principal, non-negative annual rate in basis points, tenure in months, and start date.
- Existing active `LOAN_LINKED` liability account.
- Existing active non-loan asset disbursed account with matching currency.
- Monthly fixed-rate reducing-balance calculation and half-up minor-unit rounding.
- Atomic persistence of the loan and baseline amortization schedule.

### Loan list and details

- Loading, empty, repository-error, and populated states.
- EMI, totals, outstanding principal, remaining installments, next due date, linked accounts, and contractual terms.
- Persisted installment schedule with opening balance, payment, principal, interest, closing balance, and status.
- No financial calculation inside UI or ViewModels.

### Explicitly out of scope

- Automatic disbursement ledger posting.
- EMI payment posting and reconciliation.
- Prepayment simulation and revised schedules.
- Floating/flat interest, non-monthly frequencies, moratorium, financed fees, and custom due-day behavior.
- Editing contractual terms, loan closure, and payoff actions.

## Remaining Implementation Sequence

1. Validate the Application loan slice locally.
2. Register the engine, repository, and use cases in Bootstrap/Koin diagnostics.
3. Resolve the approved Loan UI reference/specification.
4. Add production loan list/create/details UI, typed navigation, previews, and tests.
5. Run final metadata, tests, static analysis, migration, and repository-integrity gates.

## Current Validation Gate

```text
./gradlew :shared:application:compileKotlinMetadata --no-daemon --console=plain --stacktrace
./gradlew :shared:application:test --no-daemon --console=plain --stacktrace
./gradlew ktlintCheck detekt --no-daemon --console=plain --stacktrace
git diff --check
git status
```

## Architecture Constraints

- UI must use Application use cases only.
- Loan-engine calculations must not run inside Composables or ViewModels.
- Money and rate calculations must remain deterministic and avoid floating-point money arithmetic.
- Ledger history remains immutable and authoritative.
- Baseline loan creation must not silently invent disbursement or payment transactions.
- Schema changes require a verified blocker and explicit review.
- Kotlin Multiplatform `commonMain` compatibility must be preserved.
