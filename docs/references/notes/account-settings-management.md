# Account Settings Management

## Screen Name

Account Settings Management (`More → Configuration → Accounts Setting → Accounts Setting`)

## Primary Reference

- `docs/references/Screenshot_20260630_164204_Money Manager.jpg` (Accounts Setting grouped management list)

## Supporting References

- `docs/references/accounts/README.md` and `docs/references/notes/account.md` for the existing Accounts list.
- `docs/references/notes/account-creation-v2.md` for the shared creation dialog and Main Currency contract.
- `docs/references/Screenshot_20260630_164106_Money Manager.jpg` for the approved Accounts list hierarchy.

## Workflow Summary

1. More opens Configuration; Configuration opens Accounts Setting.
2. Accounts Setting offers Account Group management and an Accounts Setting row.
3. Accounts Setting opens a non-primary account-management destination with its own settings-style grouped list. It is distinct from the primary Accounts tab and reuses only the existing `AccountsViewModel`/Application read and write use cases.
4. The account-management destination lists active account names beneath their persisted group headings, offers the existing Add account workflow, and exposes explicit archive actions. It does not render the Accounts tab's balance summary, search field, account-row balances, or account-type totals.
5. Account creation requires a persisted Account Group. The selected group policy is the only source of `AccountType`; there is no independent type selector.
6. A group-to-account assignment is synchronously persisted and validated before the existing `CreateAccountUseCase` is invoked. If assignment preparation fails, no account save is attempted.
7. A custom group that still has assigned accounts cannot be deleted. Reassignment or archival is intentionally deferred until a separately approved design exists.
8. Only the Account Settings management destination exposes an account archive action. Its confirmation makes the account inactive through the existing `ArchiveAccountUseCase`; it does not delete or reassign transaction splits, ledger entries, loans, or balances.
9. Archived accounts remain visible under Account Settings → Deleted accounts with their original group and ledger-derived balance. This destination is read-only; it does not hard-delete or restore accounts.
10. Credit Card accounts save statement billing day, payment due day, optional payment-account reference, and description as local account settings metadata. The payment-account reference is informational; it does not post payments. The Accounts tab keeps multiple cards under one Credit Card group and gives cards separate `Balance Payable` and `Outst. Balance` columns. Both columns currently show the existing ledger-derived balance; the group header shows the Application-provided per-currency totals in both columns. Cards with a saved billing day are grouped by that day; cards without a saved day have no synthetic date heading. The repeated values are a reference-alignment presentation, not separate statement calculations. Dates do not calculate statements or change balances.

## Information Hierarchy

1. Account Settings: Account Group, Accounts Setting, existing unsupported rows.
2. Account-management destination: title/back/add actions, persisted group headings, account-name settings rows, archive action, loading/error/empty states, and the existing Add account affordance.
3. Add account dialog: Group, Name, Main Currency context, validation/error text, Cancel, Save.

## Functional Specification

- Groups are loaded from the persisted `AccountGroupStore`, including custom groups.
- A missing, deleted, or invalid selected group rejects Save with a visible validation message.
- `AccountType` is derived exclusively from `AccountGroupPolicy`: `CASH → CASH`, `BANK → BANK`, `CREDIT_CARD → CREDIT_CARD`, `LOAN → LOAN_LINKED`, `INVESTMENT → INVESTMENT`, `OTHER → WALLET`.
- Debit Cards remain `BANK` assets when associated with a bank group; this change adds no Debit Card `AccountType` and never maps one to `CREDIT_CARD`.
- The existing persisted Main Currency reaches `CreateAccountCommand.currencyCode` unchanged.
- Main Currency remains the default for new accounts and display formatting; changing it never rewrites existing account currencies or historical monetary data.
- Account creation continues to use `CreateAccountUseCase`; Account Settings has no second account repository, balance source, or ledger write path.
- The Account Settings management list uses the existing loaded account projection but has an independent Compose presentation. The primary Accounts tab retains its approved summary/search/balance layout unchanged.
- Existing accounts without an assignment remain visible through the established type-based fallback. Their type, ledger class, balances, and historical ledger entries are never changed.
- **Account deletion intent:** the visible action is implemented as archive, not a destructive database delete or a transfer to a shared "Deleted Account". The account keeps its ID and historical attribution; it disappears from active account lists while transactions and ledger entries remain attached to it. The user must explicitly confirm the archive action.
- An archive failure leaves the account active, keeps the confirmation visible, and shows an error. Repeated confirmation while an archive is in flight is ignored.
- Archived account list calls the existing summary use case with `includeArchived = true`, filters `isArchived`, and preserves original group identity and ledger-derived balances.
- Credit Card billing day, payment due day, optional payment-account reference, and description persist per account through the existing account-settings store. Settlement/Payment Date selection uses a bottom sheet with plain day numbers 1–31. The Accounts tab presents separate Balance Payable and Outst. Balance columns using the same existing ledger-derived balance in both; header totals reuse the Application-provided per-currency totals. No statement calculation or automatic payment is performed.
- Account Group policy cannot be changed while accounts remain assigned to that group.
- No SQLDelight schema, ledger, loan-engine, balance, payment, or transaction contract changes in this slice.

## Navigation Definition

- `More → Configuration → Accounts Setting → Accounts Setting` opens the dedicated Account Settings management list, not the primary Accounts tab.
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
- Account archive: explain that active-list visibility changes only; historical transactions, ledger entries, balances, group assignment, and loan references are preserved without reassignment.

## Tio UI Specification

- Reuse the existing Tio Ledger account read/use-case and Add account dialog, but render a separate management list; do not embed or re-label the primary Accounts tab.
- The management destination has a settings-specific app bar with a back affordance, Add account action, persisted group headings, and simple navigable account rows; it uses theme-driven colors only.
- Account group headings show a clear group title and aggregate amount without a synthetic category caption or inline account count. Credit Card follows its reference hierarchy with Balance Payable and Outst. Balance columns; both display the existing ledger-derived balance. Card rows display saved billing/payment days only when available.
- Account Settings → Deleted accounts lists archived accounts read-only and preserves their financial history.
- An account archive affordance is available only from the management destination, has an accessible account-specific label, and opens a confirmation dialog that explains history preservation before persistence.
- Account Group has only its own navigation/back and Add Group actions in the app bar. Loan navigation is not an Account Group action and must not appear as a top action/tab.
- Group options expose their selected state in text/semantics, support keyboard/switch access, and remain reachable at large text sizes.
- Save/Cancel, loading, validation, and save failures remain visible and accessible in light and dark themes.

## Credit Card Reference Gap (Follow-up Required)

The supplied Credit Card reference establishes fields/labels such as Amount, Payment, Description, Settlement Date, Payment Date, Balance Payable, Outstanding Balance, and Automatic Payment. The current persisted account and Application contracts do not define statement or payment semantics. The user-authorized scope stores only billing day and payment due day in existing local account-settings persistence and displays them as informational dates.

This slice does **not** add mutable card balances, billing formulas, payment-account behavior, automatic payments, or a statement lifecycle. Those require a separate ledger-first proposal and financial regression tests. Loan accounts continue to use the existing Loan Engine and approved Loan workflows.

## Acceptance Checklist

- [ ] Account management is reachable from the Accounts Setting row and is visibly distinct from the primary Accounts tab.
- [ ] The management list follows the approved grouped settings hierarchy without the Accounts tab's balance summary, search field, or balance/totals rows.
- [ ] Back navigation follows the definition above.
- [ ] Group selection is mandatory; invalid/deleted selection cannot create an account.
- [ ] Each policy maps to its required `AccountType`; no independent type selector exists.
- [ ] Custom-group creation and assignment survive store recreation.
- [ ] A new account is not saved unless its validated assignment is persisted first.
- [ ] In-use custom-group deletion is rejected without losing assignment data.
- [ ] Failed and repeated saves preserve account/ledger invariants.
- [ ] Archiving an account requires confirmation, removes it only from active lists, and preserves its ID, group assignment, transactions, ledger entries, and derived balance.
- [ ] Archive failure and repeated confirmation preserve the active account and show an actionable error/loading state.
- [ ] Account Group does not expose a Loan action/tab in its top app bar.
- [ ] Legacy unassigned accounts remain compatible without reclassification.
- [ ] No ledger entry, balance mutation, Loan Engine change, or schema migration occurs.
- [ ] Pixel, theme, responsive, and accessibility review are recorded separately after a real device/screenshot pass.

## Intentional Deviations

- The management destination reuses existing account data/use cases and Add account dialog, while providing a distinct original Compose presentation required by the Account Settings reference hierarchy.
- Credit Card financial detail fields are deferred because the current financial contract does not support them.
- Group assignment remains in the existing Account Group persisted infrastructure. Moving it into frozen SQLDelight persistence would require a versioned migration and separate architecture approval.
- A common "Deleted Account" was intentionally not introduced: reassigning historical records would violate the immutable-ledger and historical-attribution rules. Archive retains the original account identity instead.

## Accessibility Considerations

- Back, group selection, Add account, Save, and Cancel have clear semantic labels.
- Validation and deletion-block messages use text, not color alone.
- Light/dark, large text, and switch/keyboard operation require runtime verification before acceptance is claimed.

## JADX Boundary

This note uses checked-in screenshots and existing Tio Ledger contracts. It does not reproduce proprietary code, XML, resources, strings, dimensions, colors, icons, or assets.
