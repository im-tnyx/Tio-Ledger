# SMS-Assisted Transaction Review Flow v1 Progress

Status: Milestone started — issue, branch, architecture audit, and reference specification setup in progress
Issue: #15
Branch: `feat/sms-transaction-review-v1`
PR: pending

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
- [x] ADR-0011 confirms human-confirmed SMS-assisted capture.
- [x] ADR-0016 requires the SMS parser to remain behind a feature flag.
- [x] Architecture keeps platform SMS ingestion in `apps/*` and shared parsing/review state in shared Kotlin code.
- [x] Module design assigns transaction suggestion and confirmation models to Domain.
- [x] Existing Transaction Entry already saves through `RecordIncomeUseCase`, `RecordExpenseUseCase`, and `RecordTransferUseCase`.
- [x] Existing Transaction Entry provides reusable account/category loading, amount parsing, validation, picker, date-selection, and persistence-error patterns.
- [x] Android production SMS permission/receiver rollout and iOS import alternatives are separate platform slices.
- [x] Frozen persistence does not require a schema change for v1 because unconfirmed suggestions and raw messages are not stored.

## Initial Architecture Direction

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

## Proposed Parser Result Contract

The exact names remain subject to compile-oriented implementation review, but the result must distinguish:

- transaction suggestion
- ignored/non-transaction message
- unsupported/insufficient message
- confidence level
- detected values
- missing required values
- deterministic evidence explaining each detection

## Positive Fixture Families

- Bank debit alert.
- Bank credit alert.
- Credit-card spend alert.
- UPI payment alert.
- UPI receipt alert.
- Wallet debit/credit alert.
- ATM withdrawal alert.

## Negative Fixture Families

- OTP/security code.
- Promotional message.
- Failed/declined transaction.
- Balance-only alert.
- Ambiguous amount or direction.

## Initial Implementation Sequence

1. Finalize the fallback UI/reference specification.
2. Verify or introduce the narrow typed feature-flag API.
3. Add Domain parser/review contracts.
4. Add deterministic parser and fixture tests.
5. Add Application review preparation and confirmed-save orchestration.
6. Add Bootstrap/Koin registration and diagnostics.
7. Add shared review UI, navigation, previews, and ViewModel tests.
8. Run focused and full repository validation.
9. Mark the PR ready and merge only after explicit approval.

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
