# Loan Creation and Loan Details v1 Progress

Status: Engine, Domain, Data/SQLDelight, Application, and Bootstrap/Koin validated; UI specification approved — production UI implementation next
Issue: #12
Branch: `feat/loan-creation-details-v1`
PR: #13 (draft)

## Objective

Replace the Loans placeholder/navigation gap with production loan creation and loan details workflows backed by Domain, Application, loan-engine, Data, SQLDelight, Bootstrap/Koin, and shared UI layers.

## Completed Audit

- [x] Existing loan-engine and deterministic finance primitives.
- [x] Domain models, events, repository contracts, and typed errors.
- [x] Frozen SQLDelight loan/installment/payment/prepayment schema.
- [x] Data adapters and transaction boundaries.
- [x] Application orchestration and validation paths.
- [x] Bootstrap/Koin registration and diagnostics.
- [x] `MainRoute.Loans`, root navigation, and placeholder behavior.
- [x] Reference-source policy and fallback specification requirements.
- [x] Narrow v1 scope and explicit out-of-scope workflows.

## Architecture Findings

- The frozen schema already supports loans and persisted EMI schedules; no schema change is required.
- Currency is derived from the linked loan account and must match the disbursed account.
- Loan creation persists contractual data and the baseline schedule but does not invent ledger disbursement/payment transactions.
- Loan posting, EMI reconciliation, prepayment, closure, and payoff policies remain separate future milestones.
- UI consumes Application use cases/read models only.

## Implemented Loan Engine

- Stateless monthly fixed-rate reducing-balance `LoanCalculator`.
- Integer-only schedule-first EMI calculation.
- Annual basis-point rates and explicit half-up minor-unit interest rounding.
- Overflow-safe arithmetic, exact final-payment adjustment, and zero-balance closure.
- Month-end and leap-year due-date handling.
- Golden regression coverage for a representative 875-basis-point fixture plus zero-interest, rounding, date, invalid-input, overflow, and invariant cases.
- The 875-basis-point value is test data only; production rates remain dynamic.

## Implemented Domain And Data

- `Loan`, `LoanInstallment`, `LoanDetails`, schema-aligned enums, `LoanRepository`, `LoanCreated`, `LoanNotFound`, and `DuplicateLoanId`.
- SQLDelight insert/list/details/ordered-schedule queries.
- Linked-account currency derivation and row mappers.
- `SQLDelightLoanRepository` with typed missing/duplicate results.
- Atomic loan-plus-complete-schedule persistence.
- Ownership, deterministic ordering, reconstruction, duplicate/missing, and rollback integration tests.
- Repository read contract standardized on `findById`.

## Implemented Application

- `CreateLoanUseCase`, `ListLoansUseCase`, and `GetLoanDetailsUseCase`.
- Immutable `LoanOverview` and `LoanDetailsView` read models derived from persisted rows.
- Validation for normalized IDs/names, principal, basis-point rate, tenure, timestamp, active account eligibility, distinct accounts, and matching currencies.
- Date-only values persisted at deterministic UTC start-of-day timestamps.
- Installment IDs generated through shared `IdGenerator`.
- Engine failures mapped to typed Application validation/ledger failures.
- `LoanCreated` emitted only after successful atomic persistence.
- Focused tests for creation, schedules, account/currency validation, calculator failures, persisted summaries, ordering, and repository failures.

## Implemented Bootstrap And Koin

- Bootstrap depends on `shared:loan-engine`.
- `LoanCalculator` registered with `MonthlyReducingBalanceLoanCalculator`.
- `LoanRepository` registered with `SQLDelightLoanRepository`.
- All three loan use cases registered.
- Loan engine module included before Application resolution.
- Startup diagnostics and Android bootstrap graph tests include loan dependencies.

## Approved UI Reference And Specification

- Checked-in Loan screenshot: unavailable.
- Approved fallback: issue #12 acceptance criteria plus existing production Accounts/Budgets/Categories and Tio design-system patterns.
- Full workflow, hierarchy, navigation, deviations, accessibility, acceptance checklist, and pixel-review plan are documented in `docs/references/notes/loan.md`.
- No proprietary source, XML, resource, asset, string, color, dimension, animation, or implementation detail is copied.

## Local Validation Evidence

### Engine and Domain gate

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
```

### Data formatting regression gate

```text
ktlintCheck detekt
BUILD SUCCESSFUL in 47s
74 actionable tasks: 3 executed, 71 up-to-date
```

### Combined Application and Bootstrap gate

```text
./gradlew :shared:application:test :shared:bootstrap:test
BUILD SUCCESSFUL in 1m 11s
220 actionable tasks: 31 executed, 3 from cache, 186 up-to-date

./gradlew ktlintCheck detekt
BUILD SUCCESSFUL in 21s
74 actionable tasks: 4 executed, 70 up-to-date

./gradlew :shared:database:verifyCommonMainTioLedgerDatabaseMigration
BUILD SUCCESSFUL in 16s
10 actionable tasks: 1 executed, 9 up-to-date

git diff --check
(no output)

git status
branch up to date; nothing to commit; working tree clean
```

## Approved Initial V1 Scope

### Loan creation

- Name, positive principal, non-negative annual rate in basis points, tenure in months, and start date.
- Existing active `LOAN_LINKED` account.
- Different active non-loan asset disbursed account with matching currency.
- Monthly fixed-rate reducing-balance calculation and half-up minor-unit rounding.
- Atomic loan and baseline amortization schedule persistence.

### Loan list and details

- Loading, empty, repository-error, validation, persistence, success, and details states.
- Principal, rate, tenure, EMI, total interest, total payable, outstanding principal, remaining installments, next due date, linked accounts, and status.
- Persisted installment rows with due date, opening balance, payment, principal, interest, closing balance, and status.
- Typed Loan Details navigation.
- No financial calculation inside Composables or ViewModels.

### Explicitly out of scope

- Automatic disbursement posting.
- EMI payment posting/reconciliation.
- Prepayment simulation/revised schedules.
- Floating/flat interest, non-monthly frequencies, moratorium, financed fees, and custom due days.
- Editing contractual terms, closure, and payoff actions.

## Remaining Implementation Sequence

1. Add loan list/create/details UI state, ViewModel, and presentation mapping.
2. Add production Compose list/create/details screens and previews.
3. Wire typed Loan Details navigation and UI Koin registration.
4. Add ViewModel, navigation, and presentation tests.
5. Run final metadata, focused tests, full static analysis, migration, build/check, patch-integrity, and clean-tree gates.

## Architecture Constraints

- UI uses Application use cases only.
- Loan engine calculations never run inside Composables or ViewModels.
- Money/rate calculations remain deterministic and avoid floating-point money arithmetic.
- Ledger history remains immutable and authoritative.
- Baseline creation does not silently create disbursement/payment transactions.
- Schema changes require a verified blocker and explicit review.
- Kotlin Multiplatform `commonMain` compatibility is preserved.
