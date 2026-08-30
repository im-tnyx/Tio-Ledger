# Account Creation V2 Reference Note

## Relationship To Accounts V1

- `docs/references/notes/account.md` remains the canonical Accounts v1 reference. Accounts v1 is a read-only, display-focused baseline and does not include account creation, editing, transaction, loan, budget, report, SMS, or settings workflows.
- Account Creation is an approved v2 extension on top of that baseline. It adds a creation workflow only; it does not change or replace the v1 read/display contract.

## Entry Point

- Accounts screen floating action button ("Add account").

## Workflow

- Dialog-based creation flow, launched from the Accounts FAB.
- Cancel or dismiss closes the dialog and does not persist anything: no account is created, no draft is saved.
- Successful creation closes the dialog and refreshes the Accounts list.

## Supported V2 Account Types

- Cash
- Bank

No other account type is exposed by this flow. Extending creation to additional account types (for example `LOAN_LINKED`) requires its own approved reference and is out of scope for this note.

## Required Fields

- Account name is required.
- Validation is owned by `CreateAccountUseCase` in the application layer. The UI does not duplicate or own validation rules; it only surfaces the result.

## Currency Semantics

- **Account Creation v2 temporary default currency: USD.**
- This is a documented product limitation of v2, not a currency inferred from device locale, device region, or user input. No approved user-selectable or configured default-currency flow exists yet in this repository.
- This note does not introduce a new currency subsystem, currency selection UI, or locale-based inference. Changing the default currency behavior requires its own approved reference and application-layer change.

## Explicitly Out Of Scope

- No opening balance is created by this flow.
- No ledger entry is created merely by creating an account.

## Approval Record

This note approves the Accounts FAB plus dialog-based Cash/Bank account creation workflow described above for implementation in PR #59 (`codex/android-account-creation`), satisfying the "UI should follow approved references under `docs/references/`" rule in `AGENTS.md`.
