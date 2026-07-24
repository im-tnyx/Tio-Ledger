# SMS-Assisted Transaction Review Flow v1 Progress

Status: Typed foundation, deterministic parser, and Application orchestration validated — Bootstrap/Koin wiring pending
Issue: #15
Branch: `feat/sms-transaction-review-v1`
PR: #16 (draft)

## Objective

Implement a deterministic, privacy-preserving SMS-assisted transaction review workflow that prepares an editable suggestion and saves a normal ledger-backed transaction only after explicit user confirmation.

## Confirmed Product Rules

- SMS input is assistance, never automatic transaction creation.
- Parsing must be deterministic, offline where possible, and explainable.
- Raw message text must not be persisted by default.
- Low-confidence results expose missing information instead of inventing defaults.
- Rejecting a suggestion creates no ledger transaction.
- The capability remains behind a conservative typed feature flag.

## Completed Audit

- [x] Canonical issue #15 created.
- [x] Feature branch `feat/sms-transaction-review-v1` created from merged `main`.
- [x] Draft PR #16 opened.
- [x] ADR-0011 confirms human-confirmed SMS-assisted capture.
- [x] ADR-0016 requires the SMS parser to remain behind a feature flag.
- [x] Architecture keeps platform SMS ingestion in `apps/*` and shared parsing/review state in shared Kotlin code.
- [x] Module design assigns transaction suggestion and confirmation models to Domain.
- [x] Existing Transaction Entry already saves through `RecordIncomeUseCase`, `RecordExpenseUseCase`, and `RecordTransferUseCase`.
- [x] Existing Transaction Entry provides reusable account/category loading, amount parsing, validation, picker, date-selection, and persistence-error patterns.
- [x] Android production SMS permission/receiver rollout and iOS import alternatives are separate platform slices.
- [x] Frozen persistence does not require a schema change for v1 because unconfirmed suggestions and raw messages are not stored.

## Implemented Typed Foundation

- Added `FeatureFlag.SMS_ASSISTED_TRANSACTION_REVIEW`.
- Added immutable `FeatureFlagProvider` contract and conservative `StaticFeatureFlagProvider` that disables all experimental features by default.
- Added tests for disabled-by-default and explicit enablement behavior.
- Added Domain contracts for direction, payment rail, confidence, missing fields, detected evidence, suggestions, ignored reasons, unsupported reasons, parse requests, parse results, and `SmsTransactionParser`.
- Parse results intentionally contain no raw message text.
- Parser input requires an explicit received timestamp and time-zone ID for deterministic date handling.

## Implemented Deterministic Parser

- Added `DeterministicSmsTransactionParser` as pure shared Kotlin code.
- Negative classification runs before financial extraction for OTP/security, failed/declined, balance-only, and promotional messages.
- Money parsing supports INR, USD, EUR, and GBP markers using integer minor units only.
- Explicit numeric dates and times are converted with the caller-provided time-zone ID; invalid or absent timestamps fall back to the received timestamp while remaining marked as missing.
- Direction detection distinguishes income, expense, and explicit transfer candidates.
- Payment-rail detection covers bank, card, UPI, wallet, and ATM messages.
- Account hints and merchant/counterparty labels are extracted without copying raw message text into results.
- Confidence is explainable and missing fields remain explicit.

## Parser Fixture Coverage

Positive fixtures:

- Bank debit alert.
- Bank credit alert.
- Credit-card spend alert.
- UPI payment alert.
- UPI receipt alert.
- Wallet debit alert.
- ATM withdrawal alert.
- Explicit transfer candidate.
- Default-currency amount fallback.
- Partial/low-confidence detection.

Negative and privacy fixtures:

- OTP/security code.
- Promotional message.
- Failed/declined transaction.
- Balance-only alert.
- Empty and unrelated messages.
- Invalid time-zone fallback.
- Raw-message non-retention.

## Implemented Application Orchestration

- Added `PrepareSmsTransactionReviewUseCase` with feature-flag enforcement before parser invocation.
- Added typed preparation outcomes for editable review, ignored messages, and unsupported messages.
- Ignored and unsupported messages do not load account/category reference data.
- Review preparation loads active accounts and non-deleted categories only.
- Account options are restricted to the detected currency when available.
- Account hints select an account only when exactly one normalized suffix match exists; ambiguous hints remain unresolved.
- Category options are filtered by income/expense direction, but no category is silently selected.
- Added `ConfirmSmsTransactionUseCase`; it accepts edited structured fields and never accepts raw SMS text.
- Confirmation requires an explicit `userConfirmed` signal, positive amount, valid currency, account, timestamp, and direction-specific fields.
- Confirmed income, expense, and transfer candidates delegate to existing ledger-backed transaction recording use cases.
- Transfer confirmation requires a distinct destination account.
- Added focused tests for disabled flags, ignored-message short circuit, eligible options, exact/ambiguous account matching, repository failures, explicit confirmation, routing, and transfer validation.

## Architecture Direction

```text
platform-provided message text
    -> shared deterministic parser
    -> typed parse result / rejection reason / confidence / evidence
    -> Application review preparation
    -> editable shared UI state
    -> explicit Save or Reject
    -> existing transaction recording use case
```

### Domain

- Transaction suggestion, confidence, payment rail, parse evidence, missing-field, and rejection models.
- Deterministic parser implementation.
- No Android/iOS APIs and no persistence implementation.

### Application

- Parse/prepare-review orchestration.
- Feature-flag boundary.
- Account/category matching without silent defaults.
- Confirmed-save orchestration through existing transaction recording use cases.

### Shared UI

- Editable review state aligned with existing Transaction Entry patterns.
- Confidence and missing-field messaging.
- Save, reject, retry, validation, persistence-error, and success states.
- No raw-message persistence.

### Platform

- v1 foundation accepts provided message text.
- Android permission, inbox/receiver ingestion, and store-policy work remain out of scope.
- iOS paste/share/import adapters remain out of scope.

## Validation Evidence

Validated typed-foundation head: `7669a6d6aaffa4d640d8a4eb04a1b8869bc8a387`

```text
./gradlew :shared:core:test :shared:domain:test --no-daemon --console=plain --stacktrace
BUILD SUCCESSFUL in 28s
66 actionable tasks: 23 executed, 43 up-to-date

./gradlew ktlintCheck detekt --no-daemon --console=plain --stacktrace
BUILD SUCCESSFUL in 18s
76 actionable tasks: 7 executed, 69 up-to-date

git diff --check
(no output)

git status
nothing to commit, working tree clean
```

Validated parser head: `b5c60af2935ea2bc706f79046839e09f2e16fb7e`

```text
./gradlew :shared:domain:compileKotlinMetadata :shared:domain:test --no-daemon --console=plain --stacktrace
BUILD SUCCESSFUL in 39s
62 actionable tasks: 17 executed, 45 up-to-date

./gradlew ktlintCheck detekt --no-daemon --console=plain --stacktrace
BUILD SUCCESSFUL in 21s
78 actionable tasks: 3 executed, 75 up-to-date

git diff --check
(no output)

git status
nothing to commit, working tree clean
```

Application compile/test head: `218730dde0212c6b41c04db32c1618a21eee3203`

```text
./gradlew :shared:application:compileKotlinMetadata :shared:application:test --no-daemon --console=plain --stacktrace
BUILD SUCCESSFUL in 1m 12s
146 actionable tasks: 33 executed, 113 up-to-date
```

Application static-analysis head: `4b68f6f434da2d97a01e5b7627a71586ac7f5493`

```text
./gradlew ktlintCheck detekt --no-daemon --console=plain --stacktrace
BUILD SUCCESSFUL in 17s
78 actionable tasks: 3 executed, 75 up-to-date

git diff --check
(no output)

git status
On branch feat/sms-transaction-review-v1
Your branch is up to date with 'origin/feat/sms-transaction-review-v1'.
```

## Implementation Sequence

1. [x] Finalize the fallback UI/reference specification.
2. [x] Introduce the narrow typed feature-flag API.
3. [x] Add Domain parser/review contracts.
4. [x] Add deterministic parser and fixture tests.
5. [x] Validate and fix the parser gate.
6. [x] Add Application review preparation and confirmed-save orchestration.
7. [x] Validate and fix the Application gate.
8. [ ] Add Bootstrap/Koin registration and diagnostics.
9. [ ] Add shared review UI, navigation, previews, and ViewModel tests.
10. [ ] Run focused and full repository validation.
11. [ ] Mark the PR ready and merge only after explicit approval.

## Explicitly Out Of Scope

- Automatic transaction posting.
- Background inbox scanning.
- Android SMS permission or receiver rollout.
- iOS direct SMS access.
- Cloud parsing or remote message-content telemetry.
- Machine-learning parsing or categorization.
- Bulk inbox import.
- Persisting raw SMS text by default.
- Long-lived merchant/account mapping automation.

## Architecture Constraints

- UI calls Application use cases only.
- Parser logic remains deterministic and multiplatform-safe.
- No `Double`-based money parsing.
- Confirmed transactions use existing ledger-backed transaction workflows.
- Unconfirmed suggestions are not transactions.
- No schema change without a verified blocker and explicit review.
