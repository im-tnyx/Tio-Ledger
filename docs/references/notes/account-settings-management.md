# Account Settings Management

## Screen Name

Account Settings Management (`More → Configuration → Accounts Setting → Accounts Setting`)

## Primary Reference

- `docs/references/Screenshot_20260630_164139_Money Manager.jpg` (Configuration hierarchy)
- `docs/references/Screenshot_20260630_164204_Money Manager.jpg` (Accounts Setting entry)

## Supporting References

- `docs/references/accounts/README.md` and `docs/references/notes/account.md` for the existing Accounts list.
- `docs/references/notes/account-creation-v2.md` for the shared creation dialog and Main Currency contract.
- `docs/references/Screenshot_20260630_164106_Money Manager.jpg` for the approved Accounts list hierarchy.

## Workflow Summary

1. More opens Configuration; Configuration opens Accounts Setting.
2. Accounts Setting offers Account Group management and an Accounts Setting row.
3. Accounts Setting opens a non-primary account-management destination that reuses the approved Accounts list and existing `AccountsViewModel`/Application use cases.
4. The account-management destination lists existing accounts and offers the existing Add account workflow.
5. Account creation requires a persisted Account Group. The selected group policy is the only source of `AccountType`; there is no independent type selector.
6. A group-to-account assignment is synchronously persisted and validated before the existing `CreateAccountUseCase` is invoked. If assignment preparation fails, no account save is attempted.
7. A custom group that still has assigned accounts cannot be deleted. Reassignment or archival is intentionally deferred until a separately approved design exists.

## Information Hierarchy

1. Account Settings: Account Group, Accounts Setting, existing unsupported rows.
2. Account-management destination: existing Accounts summary, search, grouped rows, loading/error/empty states, and Add account affordance.
3. Add account dialog: Group, Name, Main Currency context, validation/error text, Cancel, Save.

## Functional Specification

- Groups are loaded from the persisted `AccountGroupStore`, including custom groups.
- A missing, deleted, or invalid selected group rejects Save with a visible validation message.
- `AccountType` is derived exclusively from `AccountGroupPolicy`: `CASH → CASH`, `BANK → BANK`, `CREDIT_CARD → CREDIT_CARD`, `LOAN → LOAN_LINKED`, `INVESTMENT → INVESTMENT`, `OTHER → WALLET`.
- Debit Cards remain `BANK` assets when associated with a bank group; this change adds no Debit Card `AccountType` and never maps one to `CREDIT_CARD`.
- The existing persisted Main Currency reaches `CreateAccountCommand.currencyCode` unchanged.
- Account creation continues to use `CreateAccountUseCase`; Account Settings has no second account repository, balance source, or ledger write path.
- Existing accounts without an assignment remain visible through the established type-based fallback. Their type, ledger class, balances, and historical ledger entries are never changed.
- No SQLDelight schema, ledger, loan-engine, balance, payment, or transaction contract changes in this slice.

## Navigation Definition

- `More → Configuration → Accounts Setting → Accounts Setting` opens account management.
- Back from account management returns to Accounts Setting.
- Back from Account Group returns to Accounts Setting.
- Back from Accounts Setting returns to Configuration; back from Configuration returns to More.
- The four primary destinations remain Transactions, Stats, Accounts, and More. The new management destination is non-primary.

## Error And Edge States

- Empty or invalid group list: show that a valid Account Group is required; do not call `CreateAccountUseCase`.
- A group removed between dialog open and Save: reject Save and retain the name draft.
- Assignment persistence failure: reject Save before account persistence and retain the draft.
- Account persistence failure after assignment preparation: remove the prepared non-account assignment and retain the draft.
- Repeated Save: while saving, further submissions are ignored.
- Cancel/dismiss: persist neither an account nor a group assignment.
- Group in use: reject deletion and explain that the group contains accounts.

## Tio UI Specification

- Reuse the existing Tio Ledger Accounts list and dialog; do not redesign the Accounts tab.
- The management destination has a back affordance with an accessible description and uses theme-driven colors only.
- Group options expose their selected state in text/semantics, support keyboard/switch access, and remain reachable at large text sizes.
- Save/Cancel, loading, validation, and save failures remain visible and accessible in light and dark themes.

## Credit Card Reference Gap (Follow-up Required)

The supplied Credit Card reference establishes fields/labels such as Amount, Payment, Description, Settlement Date, Payment Date, Balance Payable, Outstanding Balance, and Automatic Payment. The current persisted account and Application contracts do not define their financial semantics, deterministic derivation, persistence, payment execution, or statement lifecycle.

This slice does **not** add those fields, mutable balances, billing formulas, automatic payments, or a Credit Card details workflow. A future proposal must first define ledger-first meanings, storage/migration impact, payment confirmation, and focused financial regression tests. Loan accounts continue to use the existing Loan Engine and approved Loan workflows.

## Acceptance Checklist

- [ ] Account management is reachable from the Accounts Setting row and preserves the existing Accounts list layout.
- [ ] Back navigation follows the definition above.
- [ ] Group selection is mandatory; invalid/deleted selection cannot create an account.
- [ ] Each policy maps to its required `AccountType`; no independent type selector exists.
- [ ] Custom-group creation and assignment survive store recreation.
- [ ] A new account is not saved unless its validated assignment is persisted first.
- [ ] In-use custom-group deletion is rejected without losing assignment data.
- [ ] Failed and repeated saves preserve account/ledger invariants.
- [ ] Legacy unassigned accounts remain compatible without reclassification.
- [ ] No ledger entry, balance mutation, Loan Engine change, or schema migration occurs.
- [ ] Pixel, theme, responsive, and accessibility review are recorded separately after a real device/screenshot pass.

## Intentional Deviations

- The management destination reuses the existing Accounts list instead of copying a separate reference list implementation.
- Credit Card financial detail fields are deferred because the current financial contract does not support them.
- Group assignment remains in the existing Account Group persisted infrastructure. Moving it into frozen SQLDelight persistence would require a versioned migration and separate architecture approval.

## Accessibility Considerations

- Back, group selection, Add account, Save, and Cancel have clear semantic labels.
- Validation and deletion-block messages use text, not color alone.
- Light/dark, large text, and switch/keyboard operation require runtime verification before acceptance is claimed.

## JADX Boundary

This note uses checked-in screenshots and existing Tio Ledger contracts. It does not reproduce proprietary code, XML, resources, strings, dimensions, colors, icons, or assets.
