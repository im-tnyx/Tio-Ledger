# Account Creation V2

## Scope / Relationship To Accounts V1

- `docs/references/notes/account.md` remains the approved read/display baseline for the surrounding Accounts screen (summary, search, grouped list, empty/loading/error states). That contract is unchanged by this note.
- Account Creation v2 is an **additive** workflow: the existing Accounts entry affordance opens a full-screen Add Account form; Edit Account remains a separate full-screen screen.
- It does not rewrite the Accounts v1 information hierarchy outside the creation affordance itself. Summary placement, search placement, grouping, and row density stay exactly as approved in `account.md`.

## Reference Readiness

- **Primary surrounding-screen reference:** the existing approved Accounts v1 screenshot set under `docs/references/accounts/` (`docs/references/accounts/README.md`). These screenshots show the Accounts list, summary, and search.
- **Account form reference:** the owner supplied a full-screen Credit Card account screenshot in the working session. It establishes the full-screen form hierarchy and group-specific card fields; no machine-local attachment path is recorded here.
- **Supporting technical reference:** `docs/references/realbyteapps/resources/res/layout/config_account_list.xml` and `config_account_edit.xml`, inspected within the JADX boundary in `docs/references/README.md` (workflow, navigation, hierarchy, terminology, layout grouping, interaction patterns only — no copied XML, strings, colors, dimensions, or resources). Observed, in original words:
  - The reference account-list screen exposes an explicit "add" affordance in its title bar that opens a dedicated account-entry surface.
  - That entry surface orders fields around the account name, type-specific details, and a save action pinned at the bottom. Tio Ledger does not copy the reference's amount/currency editor.
- **Product source:** `docs/product-requirements.md` → "Core Functional Areas → Accounts" explicitly requires "Create, edit, archive, and reorder accounts" and lists supported account types "such as cash, bank, credit card, wallet, investment placeholder, and loan-linked account." This is the product basis for `CASH`, `BANK`, and `LOAN_LINKED` in v2.
- **Existing implementation/application contracts that constrain this workflow:**
  - `CreateAccountUseCase` / `CreateAccountCommand` in `shared/application/src/commonMain/kotlin/com/tioledger/application/usecase/account/AccountUseCases.kt` — owns all validation (id, name, currency code format, timestamp) and account creation; already currency-agnostic (`validateCurrencyCode` accepts any well-formed 3-letter code).
  - `CurrencyCode` in `shared/core/src/commonMain/kotlin/com/tioledger/core/model/CurrencyCode.kt` — a 3-letter uppercase value type with no built-in enumeration of "supported" currencies.
  - `AccountType` in `shared/domain/src/commonMain/kotlin/com/tioledger/domain/model/LedgerModels.kt` — includes `CASH`, `BANK`, `CREDIT_CARD`, `WALLET`, `LOAN_LINKED`, `INVESTMENT`.
  - `CreateLoanUseCase` (`shared/application/.../loan/LoanUseCases.kt:256-265`) requires an existing, non-archived `LOAN_LINKED` account before a loan can be created — this is the concrete downstream dependency that makes `LOAN_LINKED` a prerequisite, not an arbitrary enum exposure.
  - `LoansScreen.kt:151` already shows `"Create an active LOAN_LINKED account before adding a loan."` as its empty-state message, confirming the app expects this path to exist somewhere.

## Approval Basis

Account Creation v2 is authorized by explicit product-owner instruction given directly in this repository's working session on 2026-08-30, which:

- Approved Account Creation v2 as a Tio Ledger product extension.
- Approved use of the Money Manager JADX decompiled reference strictly as a supporting technical source for account-creation workflow, navigation, hierarchy, terminology, grouping, and interaction patterns, subject to the existing repository JADX boundary (no copied code, XML, resources, drawables, icons, strings, colors, dimensions, animations, or assets).
- Approved use of the persisted Main Currency setting for new accounts; currency is not edited in the Add Account flow.
- Approved use of the selected `AccountGroupPolicy`/`AccountType` for account creation, including cash, bank, credit card, loan-linked, investment, and wallet/other groups.

This note is the resulting documentation artifact of that approval. It records the approval; it does not grant it. No section of this note should be read as self-approving — the authority is the product-owner instruction above, cross-referenced against the product requirement and application contracts cited under Reference Readiness.

On 2026-11-10, the product owner confirmed that Add Account remains a full-screen form matching the supplied account form reference, with visible fields adapting to the selected Account Group. Unsupported Automatic Payment, Include in totals, and Show/Hide controls may be shown disabled until their behavior is implemented.

## Reference Boundary

Permitted to learn from the JADX supporting reference (`docs/references/realbyteapps/`):

- That account creation is reached from an explicit "add" affordance on the Accounts screen.
- That account creation uses the current persisted Main Currency setting rather than opening a second currency editor.
- That only fields supported by the current `CreateAccountCommand` are shown.
- General field ordering: group/type, name, and save action.

Must never be imported from the JADX reference, this note, or any implementation derived from it:

- Java/Kotlin source, XML layouts, resources, drawables, icons, strings, colors, dimensions, animations, or assets.
- Proprietary visual layout, spacing, or pixel measurements.
- Unsupported financial behavior remains out of scope: credit limit, SMS-linking, automatic payments, statement formulas, and active totals/visibility controls. The screenshot-aligned settings may be rendered disabled.
- Any Realbyte-specific business rule.

## Workflow Summary

1. User enters creation from the Accounts screen via the floating action button ("Add account").
2. The full-screen Add Account route opens.
3. User enters the account name (required, free text).
4. User selects an account group; its policy supplies the stored `AccountType` (cash, bank, credit card, loan-linked, investment, or wallet/other).
5. The current Main Currency setting supplies the account currency; there is no per-account currency selector in this flow.
6. On Save, the route validates the selected persisted group, derives `CreateAccountCommand.type` from its policy, and durably prepares the group assignment before delegating to the existing `CreateAccountUseCase`. No independent type selection is accepted.
7. If the use case returns a validation failure, the screen remains open, the field(s) remain editable, and the error is shown inline. No account is created and no UI state is fabricated as successful.
8. On success, the screen returns to Accounts, the full creation draft resets to its defaults, and the Accounts list is refreshed from the same read path Accounts v1 already uses (`ListAccountSummariesUseCase`).
9. Cancel or system back returns to Accounts, discards the entire draft, and persists nothing — no account, no ledger entry, no partial state.

## Information Hierarchy

1. Full-screen app bar with back and "Add Account" title.
2. Account Group selector and Name field.
3. Current Balance field.
4. Fields applicable to the selected group. Credit Card exposes Payment, Description, Settlement Date, Payment Date, Balance Payable, Outst. Balance, and Automatic Payment.
5. Include in totals and Show/Hide Setting remain visible but disabled until their behavior is supported.
6. Inline validation/error text, shown only when present, followed by Save with a loading state while a submission is in flight.

Ordering follows the existing Tio Ledger group-then-name-then-save pattern without reproducing reference layout measurements, spacing, or visual styling — those come from the existing Tio Ledger design system.

## Functional Specification

- **Required field:** account name. Blank name is rejected.
- **Supported account types:** the selected, still-valid persisted `AccountGroupPolicy` determines the stored `AccountType`. The form adapts to the selected policy; currently only Credit Card billing dates, optional payment-account reference, and description are persisted as group-specific metadata. A group assignment must be persisted before account creation starts, and a failed account save removes its prepared assignment.
- **Currency:** the current persisted Main Currency setting reaches `CreateAccountCommand.currencyCode` unchanged. Add Account does not expose a second currency selector.
- **Validation ownership:** unchanged — `CreateAccountUseCase` (application layer) owns all validation. The UI/ViewModel does not duplicate business validation; it only reflects success/failure.
- **One save per submission:** while a save is in flight (`isSaving`), the Save control is disabled and repeated taps do not start a second submission or create a duplicate account.
- **Successful refresh:** a successful creation always triggers a fresh read through `ListAccountSummariesUseCase` so the new account appears immediately.
- **Cancel/dismiss semantics:** discards the entire draft (name, type, error) with no persistence side effect.
- **Optional initial balance:** Add Account shows a current-balance field that defaults to zero. If left empty/zero, creation persists only the account record. If a positive amount is entered, the account and its opening-balance ledger transaction are persisted atomically.
- **Current-balance correction is an edit workflow:** editing an existing account's current balance records only the difference as a normal `INCOME` or `EXPENSE` transaction. Asset increases and liability decreases post income; asset decreases and liability increases post expense. Category is optional and remains unset. Existing ledger entries are never rewritten. Re-saving an unchanged balance creates no transaction.
- **Analytics limitation pending product decision:** `SpendingAnalyticsCalculator` currently includes these correction transactions in income/expense and cash-flow totals because persisted transaction data has no durable correction classification. Excluding them safely requires an approved persistent classification/contract change; using `ADJUSTMENT` instead would change the owner-approved income/expense workflow. Do not treat correction-inclusive reports as actual cash flow until that decision is made.
- **Ledger entry only for a supplied balance:** creation without a balance does not create a `LedgerEntry`; a supplied positive balance creates its opening-balance transaction as part of the same database operation.
- **No silent financial mutation:** every state change in this flow is either a local, reversible draft edit or a single explicit user-confirmed submission through the existing use case.

## Currency Contract

- Currency comes from the persisted Main Currency setting exposed by `CurrencyPreferenceStore`.
- No locale inference and no account-local currency selector are used in Add Account.
- The selected Main Currency code reaches `CreateAccountCommand.currencyCode` unchanged.
- Reuses the existing `CurrencyCode` value type (`shared/core`) and the existing `validateCurrencyCode` rule in `CreateAccountUseCase` — no new currency parsing or validation logic is introduced.
- Changing the Main Currency is a separate Settings workflow. Add Account does not edit or persist currency preferences.

## Loan-linked Contract

- `LOAN_LINKED` is included in Account Creation v2 because the already-approved Loans workflow (`CreateLoanUseCase`, `LoansScreen.kt`) requires an existing, active `LOAN_LINKED` account as a precondition, and Account Creation v2 is currently the only production caller of `CreateAccountUseCase`. Without this, a fresh install has no way to satisfy that precondition.
- Every persisted `AccountGroupPolicy` remains selectable, including `CREDIT_CARD`, `WALLET`, and `INVESTMENT`; the selected policy supplies the stored `AccountType`. Credit Card fields are conditional on that group. Other unsupported group-specific options remain inactive until a supported application contract exists.
- This note does not alter Loan Engine semantics, `CreateLoanUseCase` validation, amortization rules, or any loan calculation. Creating a `LOAN_LINKED` account here produces an ordinary account record; it does not create a loan, a disbursement, or any ledger entry.

## Tio UI Specification

- Use the existing Tio Ledger Compose design system (`shared/ui` components, tokens) exclusively. No visual measurements are copied from the JADX reference.
- **Screen behavior:** full-screen Add Account form using the existing Accounts navigation route. Back/Cancel discards the draft; successful save returns to Accounts.
- **Name field:** a labeled text field with an explicit accessible label, disabled while saving.
- **Current Balance:** Add Account defaults to `0.00` and allows an optional positive starting amount. Saving a supplied amount atomically writes the account and opening-balance ledger posting. Edit Account makes current balance editable; Save records only the signed difference as an income or expense transaction and preserves existing entries.
- **Account-group selector:** persisted group chips/options, one active at a time, each exposing selected/unselected state through both visual treatment and accessibility semantics (not decoration alone). The policy-derived type is not separately editable.
- **Currency:** no account-local currency control is rendered; the active Main Currency remains owned by Currency Settings.
- **Save/Cancel behavior:** Save is disabled while saving and reflects a loading label; Cancel is disabled while saving so an in-flight submission cannot be abandoned into an inconsistent state.
- **Credit Card fields:** Payment account, Description, Settlement Date, Payment Date, Balance Payable, Outst. Balance, and Automatic Payment appear only for the Credit Card group. Settlement/Payment dates remain selectable as day numbers from a bottom sheet.
- **Credit Card summary gap:** The latest owner-supplied account form reference shows separate Balance Payable and Outst. Balance values for different billing windows. The current account/Application contract has no approved statement-cycle calculation; the current UI repeats the ledger-derived balance in both rows and therefore does not match that reference. Do not invent or activate separate calculations until their transaction/date inclusion and sign rules are explicitly approved.
- **Deferred settings:** Automatic Payment is visible and disabled. Include in totals is visible and disabled. Show/Hide Setting is visible and disabled. These controls do not change payment behavior, totals, or account visibility.
- **Loading state:** Save shows a saving indication; the rest of the screen remains visible so the user retains context.
- **Validation state:** inline error text below the affected control(s), in the theme's error color, always paired with text (never color alone).
- **Error state:** repository/save failures show a message describing the save operation, not a reused list-load message.
- **Light/dark compatibility:** uses only theme-driven `MaterialTheme` colors; no hardcoded colors.
- **Large-text behavior:** text fields, chip labels, and error text must wrap rather than clip or truncate destructively at large font scales.

## Navigation Definition

- Entry points: the existing Accounts floating action button and the non-primary Account Settings management destination defined in [Account Settings management](account-settings-management.md).
- Creation uses the existing full-screen Add Account route; no primary bottom-navigation destination is added.
- Cancel/back returns to Accounts and discards the draft.
- Success returns to the Accounts screen with a refreshed list.
- No new primary navigation destination is introduced.
- The current four bottom-navigation destinations (Transactions, Stats, Accounts, More) remain unchanged.

## Error And Edge States

- **Blank name:** rejected by `CreateAccountUseCase`; inline `name: must not be blank` message; draft (including the selected type) remains editable.
- **Unsupported/invalid currency:** the Main Currency setting is validated by its own use case; if the account use case still rejects the current code, the failure is surfaced the same way as any other validation failure.
- **Repository write failure:** save-specific inline error text (not the list-load message); the screen stays open with the draft intact so the user can retry without re-entering everything.
- **Repeated Save taps:** ignored while `isSaving` is true; guarantees exactly one in-flight submission.
- **Screen dismissal (Cancel/back):** discards the full draft; no persistence.
- **Failure retains editable draft:** confirmed for invalid/deleted groups, assignment persistence, validation, and repository failures — only `isSaving` and the error message change; name/group selections are preserved so the user is not forced to redo their input.

## Accessibility Requirements

- The Accounts FAB has an accessible label describing its action ("Add account").
- The name text field has an explicit, programmatically associated label.
- The account-group selector exposes which option is currently selected to accessibility services (not just a visual leading icon).
- The current Main Currency remains accessible from Currency Settings; Add Account does not duplicate that control.
- Save and Cancel have clear, accessible labels, including while Save is in a loading state.
- Validation and repository error text is exposed to screen readers, not conveyed by color alone.
- Focus/reading order follows the visual hierarchy: title, group, name, balance, applicable group fields, error (if present), actions.
- All interactive controls remain reachable via keyboard/switch access.
- Text scales with system large-text settings without clipping or overlapping controls.
- No state in this flow is communicated by color alone.

## Intentional Deviations

1. **Full-screen Add Account form.** The owner confirmed the existing full-screen account form should remain and adapt its fields to the selected Account Group. This keeps Add and Edit as non-primary account routes and does not add bottom navigation.
2. **Optional opening balance; ledger correction on edit.** The reference's amount/currency block implies an opening balance can be entered at creation time. Tio Ledger defaults new accounts to zero and lets the user provide a positive initial amount; account plus opening transaction are committed atomically. A later explicit current-balance edit posts only its delta through the existing income/expense transaction use cases; the account balance remains a ledger projection.
3. **Group policy instead of a second type picker.** The selected group supplies the stored `AccountType`; unsupported detail fields are not fabricated or silently discarded.
4. **Currency owned by Settings.** The reference offers an account-local picker; Tio Ledger uses the existing persisted Main Currency setting so account creation has one source of truth.

## Reasons For Deviations

- Ledger-first architecture (`docs/architecture.md`) requires balances to be derived from ledger entries. Initial and corrected balances therefore create ledger postings rather than storing mutable amounts or changing historical entries. Initial account and opening posting use one database transaction.
- Engineering guidelines call for small, focused, reviewable changes and reuse of existing contracts; a full currency-settings subsystem or additional account-type fields would be out of proportion to the approved v2 scope.
- The `LOAN_LINKED` inclusion is evidence-driven (an existing approved workflow's hard prerequisite), not a general policy of exposing every enum value, keeping the change narrow while still unblocking a real, already-merged workflow.

## Pixel Review Plan

Phone-width review covering:

- Light theme, default text scale.
- Dark theme, default text scale.
- Large text scale (name field, chip labels, error text do not clip or overlap).
- Validation state (blank-name error visible and readable).
- Long account/currency labels (wrapping behavior, no destructive clipping).
- All persisted group policies, including custom groups, render a clear selected state.
- Saving state (Save shows loading, screen remains stable, no layout jump).
- Error state after a simulated repository failure (message readable, draft intact).

## Accessibility Review Checklist

- [ ] TalkBack announces the FAB's accessible label before opening the full-screen form.
- [ ] TalkBack announces the name field's label and current value.
- [ ] TalkBack announces which account type is currently selected.
- [ ] TalkBack users can reach the current Main Currency from Currency Settings; Add Account does not duplicate that control.
- [ ] Validation error text is announced when it appears.
- [ ] Repository-failure error text is announced when it appears.
- [ ] Save and Cancel are reachable and clearly labeled via keyboard/switch access.
- [ ] Large text scaling does not clip or overlap any control in the form.
- [ ] No state in the form is communicated by color alone.
- [ ] Light/dark contrast is reviewed for text, chips, and error color.

## Functional Acceptance Checklist

- [ ] Entry is available from the existing Accounts FAB and from the non-primary Account Settings management destination; no additional creation route is introduced.
- [ ] The four primary bottom-navigation destinations remain Transactions, Stats, Accounts, and More.
- [ ] Name is required; blank name is rejected with an inline message.
- [ ] Each persisted group policy is selectable and reaches `CreateAccountCommand.type` correctly; invalid/deleted groups are rejected before account persistence.
- [ ] The current Main Currency setting reaches `CreateAccountCommand.currencyCode` unchanged; Add Account does not silently override it.
- [ ] Validation ownership remains in `CreateAccountUseCase`; the ViewModel performs no duplicate business validation.
- [ ] Exactly one save operation occurs per user submission; repeated taps while saving do not create duplicates.
- [ ] Successful creation returns from the form, resets the draft, and refreshes the Accounts list.
- [ ] Cancel/dismiss persists nothing and leaves Accounts state unchanged.
- [ ] Draft fully resets (name, type, error) after both cancel and successful creation.
- [ ] Empty/zero starting balance creates only the account; positive starting balance atomically creates the opening-balance posting with it.
- [ ] Account edits post only the current-balance delta as income/expense; historical ledger entries remain immutable.
- [ ] Repository/save failures show create-specific feedback, not the list-load error message.
- [ ] Pixel review is completed.
- [ ] Accessibility review is completed.
- [ ] No financial schema, ledger, database, or Loan Engine semantics were changed.
- [ ] Prepared group assignments are removed when account persistence fails, and in-use groups cannot be deleted.

## Deviation Log

| Area | Reference behavior/evidence | Tio Ledger v2 decision | Approval basis |
| --- | --- | --- | --- |
| Entry surface | Owner-supplied account form reference and `config_account_list.xml` add affordance use a dedicated full-screen entry surface | Full-screen Add Account form, with group-specific fields and disabled unsupported controls | Product-owner confirmation, 2026-11-10 |
| Balance entry | `config_account_edit.xml` `amountBlock` implies balance entry at creation | Optional opening balance is atomic with account creation; later edits post only the delta as income/expense | Product-owner clarification, 2026-11-09; `docs/architecture.md` Ledger-First Accounting; ADR-0013 |
| Account types | Reference edit surface adapts fields per type across its full type set | Selected Account Group policy supplies the stored type; unsupported detail fields are deferred | Existing `AccountGroupPolicy` and `CreateAccountCommand` contracts |
| Currency | Reference presents currency as a visible multi-option control near the amount field | Existing persisted Main Currency setting is the single source for new-account currency | `CurrencyPreferenceStore`/`CurrencyCode` existing contracts; product-owner approval, 2026-08-30 |

## Manual Validation Evidence (2026-08-30)

Performed on a wiped Pixel_9 (API 36) emulator against the implementation described in this note:

- Verified: FAB entry, Cancel discards nothing, blank-name validation, group-policy-to-account-type mapping, current Main Currency reaching persistence unchanged, full draft reset after both cancel and success, and duplicate-submission protection.
- Verified: dark theme renders correctly with proper contrast and no hardcoded colors — checked with `MainActivity`'s `darkTheme` flag temporarily forced to `true` for this session only, since the flag is hardcoded to `false` app-wide today (a pre-existing, unrelated gap — see PR discussion). That override was not committed.
- Verified: at large text scale, the group sheet uses bounded scrolling so every group and the Cancel action remain reachable.
- Verified: the Add Account flow does not duplicate the global currency selector; it consumes the current Main Currency setting.
- Verified at the code level: the primary Accounts screen's always-available `Loans` shortcut opens `MainRoute.Loans`, including when no accounts exist or all loan-linked accounts are already associated. `LoansViewModel.loanAccountOptions` continues to require an active `LOAN_LINKED` account for the existing Create Loan workflow.
- Not performed: literal TalkBack audio narration, and a dedicated formal Pixel/accessibility sign-off pass. The Pixel Review Plan and Accessibility Review Checklist below remain open until that pass happens.

## Approval Record

Account Creation v2 — the Accounts FAB, full-screen creation flow, Account Group policy/type mapping, and Main Currency setting integration described in this note — was initially authorized by direct product-owner instruction on 2026-08-30. The full-screen form and disabled-option presentation were confirmed by the product owner on 2026-11-10. Supporting JADX analysis remains within the repository's existing boundary (`docs/references/README.md`).

This note does not itself grant approval, and it does not claim Pixel review or accessibility review have passed — both remain open checklist items above until a reviewer actually completes them. Production Compose implementation must satisfy every item in the Functional Acceptance Checklist before this workflow is considered done per `docs/definition-of-done.md`.
