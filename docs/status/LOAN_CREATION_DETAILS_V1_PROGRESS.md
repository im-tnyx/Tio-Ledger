# Loan Creation and Loan Details v1 Progress

Status: Feature implementation complete; focused Engine, Data, Application, Bootstrap/Koin, and UI gates validated — final shell/full-repository gate pending
Issue: #12
Branch: `feat/loan-creation-details-v1`
PR: #13 (draft)

## Objective

Replace the Loans placeholder with production loan creation, list, and details workflows backed by Domain, Application, loan-engine, Data, SQLDelight, Bootstrap/Koin, and shared UI layers.

## Architecture Findings

- The frozen schema already supports loans and persisted EMI schedules; no schema or migration change is required.
- Currency is derived from the linked loan account and must match the disbursed account.
- Loan creation persists contractual data and the baseline schedule but does not invent ledger disbursement or payment transactions.
- Loan posting, EMI reconciliation, prepayment, closure, and payoff remain separate future milestones.
- UI consumes Application use cases/read models only.

## Implemented Loan Engine

- Stateless monthly fixed-rate reducing-balance `LoanCalculator`.
- Integer-only schedule-first EMI calculation using annual basis-point rates.
- Explicit half-up minor-unit interest rounding.
- Overflow-safe arithmetic, exact final-payment adjustment, and zero-balance closure.
- Month-end and leap-year due-date handling.
- Regression coverage for a representative 875-basis-point fixture, zero interest, rounding boundaries, date boundaries, invalid terms, overflow, and schedule invariants.
- The 875-basis-point value is test data only; production rates remain dynamic.

## Implemented Domain And Data

- Added `Loan`, `LoanInstallment`, `LoanDetails`, schema-aligned enums, `LoanRepository`, `LoanCreated`, `LoanNotFound`, and `DuplicateLoanId`.
- Added SQLDelight insert/list/details/ordered-schedule queries.
- Added linked-account currency derivation and row mappers.
- Added `SQLDelightLoanRepository` with typed missing/duplicate results.
- Added atomic loan-plus-complete-schedule persistence.
- Added ownership, deterministic ordering, reconstruction, duplicate/missing, and rollback integration tests.
- Canonical repository details read contract is `LoanRepository.findDetails`.

## Implemented Application

- Added `CreateLoanUseCase`, `ListLoansUseCase`, and `GetLoanDetailsUseCase`.
- Added immutable `LoanOverview` and `LoanDetailsView` read models derived from persisted rows.
- Added validation for normalized IDs/names, principal, basis-point rate, tenure, timestamps, active account eligibility, distinct accounts, and matching currencies.
- Date-only values persist at deterministic UTC start-of-day timestamps.
- Installment IDs are generated through shared `IdGenerator`.
- Engine failures map to typed Application validation/ledger failures.
- `LoanCreated` is emitted only after successful atomic persistence.
- Added focused creation, schedule, account/currency, calculator-failure, persisted-summary, ordering, and repository-failure tests.

## Implemented Bootstrap And Koin

- Bootstrap depends on `shared:loan-engine`.
- `LoanCalculator` is registered with `MonthlyReducingBalanceLoanCalculator`.
- `LoanRepository` is registered with `SQLDelightLoanRepository`.
- All three loan use cases are registered.
- Startup diagnostics and Android bootstrap graph tests include loan dependencies.

## Approved UI Reference And Specification

- Checked-in Loan screenshot was unavailable at milestone start.
- Approved fallback uses issue #12 acceptance criteria plus existing production Accounts/Budgets/Categories and Tio design-system patterns.
- Workflow, hierarchy, navigation, deviations, accessibility, acceptance checklist, and pixel-review plan are documented in `docs/references/notes/loan.md`.
- No proprietary source, XML, resource, asset, string, color, dimension, animation, or implementation detail is copied.

## Implemented Shared UI

- Added `LoansViewModel` for list/account loading, creation state, account pickers, validation, save feedback, and persisted-list refresh.
- Added `LoanDetailsViewModel` for typed details loading, account-label resolution through Application, retry, and persisted presentation mapping.
- Added integer-only principal parsing and percentage-to-basis-point conversion with at most two decimal places.
- Added active `LOAN_LINKED` and matching-currency non-loan asset account filtering; Application remains the final validator.
- Added loading, empty, error, populated, success, create-dialog, and account-picker states.
- Added dedicated Loan Details presentation with contractual terms, summary values, linked account labels, and persisted amortization cards.
- Added typed `MainRoute.LoanDetails(loanId)` and root-host list/details/back wiring.
- Added UI Koin registrations, light/dark previews, ViewModel tests, and typed navigation tests.

## Final Cross-Layer Review

- Confirmed the branch is ahead of `main` and behind by zero commits.
- Confirmed the changed-file set is limited to the Loan milestone and one narrow shell navigation defect fix.
- Found that `TioAppShell` previously passed no navigation callback, leaving production root navigation at the default no-op behavior.
- Added internal active-route state in `TioAppShell` so Loans list-to-details, details-back, and existing root navigation callbacks operate in the Android host.
- The shell fix requires the final focused/full rerun before review readiness.

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
```

### Focused UI gate

```text
./gradlew :shared:ui:compileKotlinMetadata
BUILD SUCCESSFUL

./gradlew :shared:ui:test
BUILD SUCCESSFUL in 1m 11s
231 actionable tasks: 17 executed, 214 up-to-date

./gradlew ktlintCheck detekt
BUILD SUCCESSFUL in 22s
74 actionable tasks: 4 executed, 70 up-to-date

git diff --check
(no output)

git status
branch up to date; nothing to commit; working tree clean
```

## Approved V1 Scope

### Loan creation

- Name, positive principal, non-negative annual rate, tenure in months, and start date.
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

## Final Validation Gate

```text
./gradlew :shared:ui:compileKotlinMetadata :shared:ui:test --no-daemon --console=plain --stacktrace
./gradlew build --no-daemon --console=plain --stacktrace
./gradlew check --no-daemon --console=plain --stacktrace
./gradlew ktlintCheck detekt --no-daemon --console=plain --stacktrace
./gradlew :shared:database:verifyCommonMainTioLedgerDatabaseMigration --no-daemon --no-parallel --max-workers=1 --console=plain --stacktrace
git diff --check
git status
```

## Architecture Constraints

- UI uses Application use cases only.
- Loan engine calculations never run inside Composables or ViewModels.
- Money/rate calculations remain deterministic and avoid floating-point money arithmetic.
- Ledger history remains immutable and authoritative.
- Baseline creation does not silently create disbursement/payment transactions.
- Schema changes require a verified blocker and explicit review.
- Kotlin Multiplatform `commonMain` compatibility is preserved.
