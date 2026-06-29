# SMS Assisted Transaction Capture

## Purpose

SMS-assisted capture helps users record transactions faster by parsing bank, credit card, UPI, and wallet messages. It is an assistant, not an automation system. The app must never create a transaction from SMS without explicit user confirmation.

The SMS Parser is an experimental capability and must remain behind a feature flag until it is mature enough for default availability.

## Product Rules

- Never automatically create a transaction from an SMS.
- Always show a review screen before saving.
- Pre-fill detected fields for user review.
- Allow editing amount, account, category, merchant, notes, tags, and date.
- If parser confidence is low, request more input instead of guessing.
- Prefer deterministic, offline parsing.
- Keep raw messages private and avoid unnecessary persistence.

## Supported Message Families

Initial parser rules should target:

- Bank debit alerts.
- Bank credit alerts.
- Credit card spend alerts.
- UPI payment and receipt alerts.
- Wallet debit and credit alerts.
- ATM withdrawal alerts.
- Failed transaction messages as non-transaction candidates.

## Capture Flow

```text
SMS text
  -> Platform ingestion
  -> Shared deterministic parser
  -> Transaction suggestion
  -> Editable review screen
  -> User confirmation
  -> Save transaction
```

## Suggested Fields

The parser may infer:

- Direction: income, expense, or transfer candidate.
- Amount and currency.
- Account hint.
- Merchant or counterparty.
- Payment rail such as UPI, card, bank, or wallet.
- Transaction date and time.
- Category suggestion.
- Notes.
- Tags.
- Confidence score and missing fields.

## Confidence Model

Confidence should be explainable and deterministic. Suggested levels:

- High: amount, direction, date, and account or rail are clearly detected.
- Medium: amount and direction are clear, but account, merchant, or category is uncertain.
- Low: amount or direction is ambiguous.

Low-confidence suggestions should ask the user for missing information rather than silently choosing defaults.

## Parser Design

The parser should use layered deterministic rules:

1. Normalize message text.
2. Detect non-transaction alerts and failed transactions.
3. Detect amount and currency.
4. Detect debit, credit, refund, reversal, or transfer direction.
5. Detect account/card/wallet hints.
6. Detect merchant or counterparty.
7. Detect transaction date and time.
8. Assign category suggestion using local rules.
9. Return a suggestion with confidence and parse evidence.

Avoid machine learning in the initial implementation unless it can run offline, preserve privacy, and remain explainable.

## Privacy

- Parsing should happen on-device.
- Raw SMS text should not leave the device.
- Raw SMS text should not be persisted by default.
- Parser telemetry, if ever added, must not include sensitive message content.

## Feature Flag

The SMS Parser must be controlled by a typed feature flag. When disabled:

- SMS permissions are not requested.
- SMS ingestion surfaces are hidden.
- Manual transaction entry remains fully functional.
- Existing confirmed transactions remain unaffected.

## Platform Notes

Android can support platform-level SMS-assisted workflows subject to permissions and store policy requirements.

iOS does not have the same general SMS access model, so iOS should use permitted alternatives such as manual paste, share/import flows, or notification-driven user entry where available.

## UX Requirements

The review screen should feel like normal transaction entry with helpful fields already filled. It should not feel like a separate automation workflow.

Required actions:

- Save.
- Edit fields.
- Reject suggestion.
- Mark account or merchant mapping when useful.

## Testing Requirements

- Parser fixtures for each supported message family.
- Negative fixtures for OTP, promotional, failed, and balance-only messages.
- Confidence-level tests.
- Date and amount parsing edge cases.
- Privacy tests ensuring raw messages are not saved as transactions by default.
