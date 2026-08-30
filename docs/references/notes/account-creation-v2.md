# Account Creation V2

## Scope / Relationship To Accounts V1

- `docs/references/notes/account.md` remains the approved read/display baseline for the surrounding Accounts screen (summary, search, grouped list, empty/loading/error states). That contract is unchanged by this note.
- Account Creation v2 is an **additive** workflow: a single entry affordance and a creation surface layered on top of the existing Accounts screen.
- It does not rewrite the Accounts v1 information hierarchy outside the creation affordance itself. Summary placement, search placement, grouping, and row density stay exactly as approved in `account.md`.

## Reference Readiness

- **Primary surrounding-screen reference:** the existing approved Accounts v1 screenshot set under `docs/references/accounts/` (`docs/references/accounts/README.md`). These screenshots show the Accounts list, summary, and search — they are **not** screenshots of the creation dialog and must not be described as such.
- **Creation-dialog screenshot:** unavailable. No checked-in approved screenshot of an account-creation dialog exists in this repository, and no approved Tio Ledger mockup for this dialog exists either. This note relies on the next fallback tier — the JADX supporting technical reference — plus product requirements, per the fallback order in `docs/references/README.md` (approved screenshots → approved Tio mockups → JADX technical reference → official website → Play Store screenshots).
- **Supporting technical reference:** `docs/references/realbyteapps/resources/res/layout/config_account_list.xml` and `config_account_edit.xml`, inspected within the JADX boundary in `docs/references/README.md` (workflow, navigation, hierarchy, terminology, layout grouping, dialog flow, interaction patterns only — no copied XML, strings, colors, dimensions, or resources). Observed, in original words:
  - The reference account-list screen exposes an explicit "add" affordance in its title bar that opens a dedicated account-entry surface.
  - That entry surface orders fields as: name, an amount/currency area where currency is picked from a visible, explicit multi-option control next to the amount (not inferred or hidden), account-type-specific fields (for example a credit-limit block shown only for credit-type accounts), and a save action pinned at the bottom.
  - Currency is always a visible, explicit, user-driven choice in the reference workflow — never a silent default.
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
- Approved use of the Money Manager JADX decompiled reference strictly as a supporting technical source for account-creation workflow, navigation, hierarchy, terminology, grouping, dialog flow, and interaction patterns, subject to the existing repository JADX boundary (no copied code, XML, resources, drawables, icons, strings, colors, dimensions, animations, or assets).
- Approved explicit user-driven currency selection at account-creation time.
- Approved `CASH`, `BANK`, and `LOAN_LINKED` as the supported v2 account types.

This note is the resulting documentation artifact of that approval. It records the approval; it does not grant it. No section of this note should be read as self-approving — the authority is the product-owner instruction above, cross-referenced against the product requirement and application contracts cited under Reference Readiness.

## Reference Boundary

Permitted to learn from the JADX supporting reference (`docs/references/realbyteapps/`):

- That account creation is reached from an explicit "add" affordance on the Accounts screen.
- That currency selection is presented as a visible, explicit, user-driven control rather than inferred or hidden.
- That account-type-specific fields exist and only the fields relevant to the selected type should be shown.
- General field ordering: name before type-specific detail, currency near the amount/balance area, save pinned at the end of the flow.

Must never be imported from the JADX reference, this note, or any implementation derived from it:

- Java/Kotlin source, XML layouts, resources, drawables, icons, strings, colors, dimensions, animations, or assets.
- Proprietary visual layout, spacing, or pixel measurements.
- Reference-specific fields out of v2 scope: credit limit, opening-balance amount entry, account grouping/category, SMS-linking configuration, show/hide toggles, ad banner placement.
- Any Realbyte-specific business rule.

## Workflow Summary

1. User enters creation from the Accounts screen via the floating action button ("Add account").
2. A dialog surface opens over the Accounts screen.
3. User enters the account name (required, free text).
4. User selects the supported account type: `Cash`, `Bank`, or `Loan-linked`.
5. User explicitly selects the account currency from a small, deterministic set of supported currency options. No option is silently assumed on save; a default is pre-selected for convenience but remains visibly changeable before the user submits.
6. On Save, the ViewModel builds a `CreateAccountCommand` from the current draft (name, type, selected currency, generated id, timestamp) and delegates to the existing `CreateAccountUseCase`. The UI performs no validation of its own beyond disabling submission while a save is in flight.
7. If the use case returns a validation failure, the dialog stays open, the field(s) remain editable, and the error is shown inline. No account is created and no UI state is fabricated as successful.
8. On success, the dialog closes, the full creation draft resets to its defaults, and the Accounts list is refreshed from the same read path Accounts v1 already uses (`ListAccountSummariesUseCase`).
9. Cancel or dismiss (tap outside, system back) closes the dialog, discards the entire draft, and persists nothing — no account, no ledger entry, no partial state.

## Information Hierarchy

1. Dialog title ("Add account").
2. Name field.
3. Account-type selector (`Cash`, `Bank`, `Loan-linked`).
4. Currency selector.
5. Inline validation/error text, shown only when present.
6. Cancel and Save actions, with Save reflecting a loading state while a submission is in flight.

Ordering matches the reference's name-then-type-then-currency-then-save pattern without reproducing its layout measurements, spacing, or visual styling — those come from the existing Tio Ledger design system.

## Functional Specification

- **Required field:** account name. Blank name is rejected.
- **Supported account types:** `CASH`, `BANK`, `LOAN_LINKED`. No other `AccountType` value is exposed by this flow; enum membership alone is not a reason to expose a type (see Loan-linked Contract).
- **Currency:** explicit user selection from a small deterministic set of supported currency codes (see Currency Contract). The selection reaches `CreateAccountCommand.currencyCode` unchanged.
- **Validation ownership:** unchanged — `CreateAccountUseCase` (application layer) owns all validation. The UI/ViewModel does not duplicate business validation; it only reflects success/failure.
- **One save per submission:** while a save is in flight (`isSaving`), the Save control is disabled and repeated taps do not start a second submission or create a duplicate account.
- **Successful refresh:** a successful creation always triggers a fresh read through `ListAccountSummariesUseCase` so the new account appears immediately.
- **Cancel/dismiss semantics:** discards the entire draft (name, type, currency, error) with no persistence side effect.
- **No opening-balance posting:** this flow creates an account record only. It does not create, prompt for, or imply an opening-balance transaction or ledger posting.
- **No ledger entry from creation alone:** creating an account, by itself, never creates a `LedgerEntry`. Ledger entries are created only by transaction/loan/budget workflows that are out of scope here.
- **No silent financial mutation:** every state change in this flow is either a local, reversible draft edit or a single explicit user-confirmed submission through the existing use case.

## Currency Contract

- Currency is **explicit user input**, selected from a small, deterministic, presentation-level list of supported currency codes before Save is enabled to succeed.
- No locale inference. The device locale/region is never read to choose or pre-fill currency.
- No silent USD persistence. The previous version of this note stated a temporary hardcoded-USD limitation; that limitation is removed as of this revision. A currency is still pre-selected when the dialog opens (for the same reason account type defaults to `Cash`: a usable starting state), but it is visibly shown and must be actively confirmed or changed by the user before submission reaches the use case.
- Reuses the existing `CurrencyCode` value type (`shared/core`) and the existing `validateCurrencyCode` rule in `CreateAccountUseCase` — no new currency parsing or validation logic is introduced.
- This PR does not introduce a global or persisted default-currency subsystem, a currency settings screen, or a user-wide currency preference. The presentation-level supported-currency list lives in the UI layer only, as the narrowest possible implementation of explicit selection.
- This PR does not add or change any workflow for editing an account's currency after creation. `UpdateAccountUseCase` is untouched. Changing currency post-creation requires its own separately approved workflow.

## Loan-linked Contract

- `LOAN_LINKED` is exposed in Account Creation v2 **only** because the already-approved Loans workflow (`CreateLoanUseCase`, `LoansScreen.kt`) requires an existing, active `LOAN_LINKED` account as a precondition, and Account Creation v2 is currently the only production caller of `CreateAccountUseCase`. Without this, a fresh install has no way to satisfy that precondition.
- This is a narrow, evidence-backed exposure, not a general policy of surfacing every `AccountType` value. `CREDIT_CARD`, `WALLET`, and `INVESTMENT` remain out of scope for v2 because no existing approved workflow currently depends on being able to create them from this dialog.
- This note does not alter Loan Engine semantics, `CreateLoanUseCase` validation, amortization rules, or any loan calculation. Creating a `LOAN_LINKED` account here produces an ordinary account record; it does not create a loan, a disbursement, or any ledger entry.

## Tio UI Specification

- Use the existing Tio Ledger Compose design system (`shared/ui` components, tokens) exclusively. No visual measurements are copied from the JADX reference.
- **Dialog/surface behavior:** a Material `AlertDialog`-style surface anchored to the Accounts screen, dismissible by Cancel, the dismiss request (scrim tap/back), or successful save.
- **Name field:** a labeled text field with an explicit accessible label, disabled while saving.
- **Account-type selector:** a set of selectable chips/options, one active at a time, each exposing selected/unselected state through both visual treatment and accessibility semantics (not decoration alone).
- **Currency selector:** the same selectable-chip pattern as account type, one active currency at a time, exposing current selection through accessibility semantics.
- **Save/Cancel behavior:** Save is disabled while saving and reflects a loading label; Cancel is disabled while saving so an in-flight submission cannot be abandoned into an inconsistent state.
- **Loading state:** Save shows a saving indication; the rest of the dialog remains visible so the user retains context.
- **Validation state:** inline error text below the affected control(s), in the theme's error color, always paired with text (never color alone).
- **Error state:** repository/save failures show a message describing the save operation, not a reused list-load message.
- **Light/dark compatibility:** uses only theme-driven `MaterialTheme` colors; no hardcoded colors.
- **Large-text behavior:** text fields, chip labels, and error text must wrap rather than clip or truncate destructively at large font scales.

## Navigation Definition

- Entry point: the existing Accounts floating action button.
- Creation remains part of the Accounts workflow; it is not a new route, screen, or back-stack destination.
- Cancel returns to the Accounts screen exactly as it was before the dialog opened (unchanged state).
- Success returns to the Accounts screen with a refreshed list.
- No new primary navigation destination is introduced.
- The canonical five bottom-navigation destinations (Dashboard, Accounts, Transactions, Categories, Budgets) remain unchanged.

## Error And Edge States

- **Blank name:** rejected by `CreateAccountUseCase`; inline `name: must not be blank` message; draft (including any selected type/currency) remains editable.
- **Unsupported/invalid currency:** cannot occur through normal interaction because selection is constrained to the deterministic supported-currency set, each a valid 3-letter code accepted by `validateCurrencyCode`; if the use case still rejects it, the failure is surfaced the same way as any other validation failure.
- **Repository write failure:** save-specific inline error text (not the list-load message); the dialog stays open with the draft intact so the user can retry without re-entering everything.
- **Repeated Save taps:** ignored while `isSaving` is true; guarantees exactly one in-flight submission.
- **Dialog dismissal (Cancel, scrim tap, back):** discards the full draft; no persistence.
- **Failure retains editable draft:** confirmed for both validation and repository failures — only `isSaving` and the error message change; name/type/currency selections are preserved so the user is not forced to redo their input.

## Accessibility Requirements

- The Accounts FAB has an accessible label describing its action ("Add account").
- The name text field has an explicit, programmatically associated label.
- The account-type selector exposes which option is currently selected to accessibility services (not just a visual leading icon).
- The currency selector exposes its current selection to accessibility services the same way.
- Save and Cancel have clear, accessible labels, including while Save is in a loading state.
- Validation and repository error text is exposed to screen readers, not conveyed by color alone.
- Focus/reading order follows the visual hierarchy: title, name, type, currency, error (if present), actions.
- All interactive controls remain reachable via keyboard/switch access.
- Text scales with system large-text settings without clipping or overlapping controls.
- No state in this flow is communicated by color alone.

## Intentional Deviations

1. **Modal dialog instead of a dedicated full screen.** The JADX reference reaches account entry through a dedicated navigable screen with several additional fields (credit limit, memo, SMS linking, show/hide, category). Tio Ledger v2 scope is limited to name, type, and currency, so a modal dialog anchored to Accounts is used instead of a new screen/route.
2. **No opening-balance entry.** The reference's amount/currency block implies an opening balance can be entered at creation time. Tio Ledger v2 explicitly excludes opening-balance entry; accounts start at a computed zero balance derived from the ledger, consistent with the ledger-first architecture.
3. **Three account types instead of the full reference set.** Only `Cash`, `Bank`, and `Loan-linked` are exposed. Credit-card, wallet, and investment creation are deferred until their own workflows need them (see Loan-linked Contract).
4. **Currency presented as a small fixed set, not free text.** The reference offers a broader currency picker; Tio Ledger v2 uses a small deterministic list appropriate to a narrow v2 slice rather than building a full currency-settings subsystem in this change.

## Reasons For Deviations

- Ledger-first architecture (`docs/architecture.md`) requires balances to be derived from ledger entries, not entered directly on an account; an opening-balance field would conflict with that invariant without its own approved posting workflow.
- Engineering guidelines call for small, focused, reviewable changes and reuse of existing contracts; a full currency-settings subsystem or additional account-type fields would be out of proportion to the approved v2 scope.
- The `LOAN_LINKED` inclusion is evidence-driven (an existing approved workflow's hard prerequisite), not a general policy of exposing every enum value, keeping the change narrow while still unblocking a real, already-merged workflow.

## Pixel Review Plan

Phone-width review covering:

- Light theme, default text scale.
- Dark theme, default text scale.
- Large text scale (name field, chip labels, error text do not clip or overlap).
- Validation state (blank-name error visible and readable).
- Long account/currency labels (wrapping behavior, no destructive clipping).
- `Cash`, `Bank`, and `Loan-linked` type selections (each renders a clear selected state).
- Currency selection across at least two different currencies (selected state clear, non-selected options remain legible).
- Saving state (Save shows loading, dialog remains stable, no layout jump).
- Error state after a simulated repository failure (message readable, draft intact).

## Accessibility Review Checklist

- [ ] TalkBack announces the FAB's accessible label before opening the dialog.
- [ ] TalkBack announces the name field's label and current value.
- [ ] TalkBack announces which account type is currently selected.
- [ ] TalkBack announces which currency is currently selected.
- [ ] Validation error text is announced when it appears.
- [ ] Repository-failure error text is announced when it appears.
- [ ] Save and Cancel are reachable and clearly labeled via keyboard/switch access.
- [ ] Large text scaling does not clip or overlap any control in the dialog.
- [ ] No state in the dialog is communicated by color alone.
- [ ] Light/dark contrast is reviewed for text, chips, and error color.

## Functional Acceptance Checklist

- [ ] Entry is from the existing Accounts FAB only; no new route is introduced.
- [ ] Canonical five bottom-navigation destinations remain unchanged.
- [ ] Name is required; blank name is rejected with an inline message.
- [ ] `Cash`, `Bank`, and `Loan-linked` are all selectable and each reaches `CreateAccountCommand.type` correctly.
- [ ] Currency is an explicit, visible, user-changeable selection that reaches `CreateAccountCommand.currencyCode` unchanged; no currency is silently persisted.
- [ ] Validation ownership remains in `CreateAccountUseCase`; the ViewModel performs no duplicate business validation.
- [ ] Exactly one save operation occurs per user submission; repeated taps while saving do not create duplicates.
- [ ] Successful creation closes the dialog, resets the draft, and refreshes the Accounts list.
- [ ] Cancel/dismiss persists nothing and leaves Accounts state unchanged.
- [ ] Draft fully resets (name, type, currency, error) after both cancel and successful creation.
- [ ] Account creation alone never creates an opening-balance posting or any ledger entry.
- [ ] Repository/save failures show create-specific feedback, not the list-load error message.
- [ ] Pixel review is completed.
- [ ] Accessibility review is completed.
- [ ] No financial schema, ledger, database, or Loan Engine semantics were changed.

## Deviation Log

| Area | Reference behavior/evidence | Tio Ledger v2 decision | Approval basis |
| --- | --- | --- | --- |
| Entry surface | `config_account_list.xml` add affordance opens a dedicated full-screen entry surface (`config_account_edit.xml`) | Modal dialog anchored to the existing Accounts screen | Narrower v2 field set does not need a new screen/route; product-owner approval, 2026-08-30 |
| Opening balance | `config_account_edit.xml` `amountBlock` implies balance entry at creation | No opening-balance entry; balances derive from the ledger | `docs/architecture.md` Ledger-First Accounting; ADR-0013 |
| Account types | Reference edit surface adapts fields per type across its full type set | Only `CASH`, `BANK`, `LOAN_LINKED` exposed | `docs/product-requirements.md` Accounts section; `LOAN_LINKED` additionally required by `CreateLoanUseCase`/`LoansScreen.kt:151`; product-owner approval, 2026-08-30 |
| Currency | Reference presents currency as a visible multi-option control near the amount field | Explicit small deterministic currency selector, no locale inference, no silent default persisted | `CurrencyCode`/`validateCurrencyCode` existing contracts; product-owner approval, 2026-08-30 |

## Manual Validation Evidence (2026-08-30)

Performed on a wiped Pixel_9 (API 36) emulator against the implementation described in this note:

- Verified: FAB entry, Cancel discards nothing, blank-name validation, Cash creation, Bank creation, Loan-linked creation, INR and EUR currency selection reaching persistence unchanged, full draft reset after both cancel and success, search/list behavior with multiple currencies present, and duplicate-submission protection.
- Verified: dark theme renders correctly with proper contrast and no hardcoded colors — checked with `MainActivity`'s `darkTheme` flag temporarily forced to `true` for this session only, since the flag is hardcoded to `false` app-wide today (a pre-existing, unrelated gap — see PR discussion). That override was not committed.
- Verified: at 1.3x system font scale, the type and currency chip rows remain fully reachable via horizontal scroll rather than being destructively clipped, though neither row currently shows a scroll affordance hint. Tracked as a minor follow-up, not a blocker.
- Verified: after switching `TioFilterChip` to the real Material3 `FilterChip`, the accessibility tree exposes `checkable=true`/`checked=true|false` correctly for both the selected and unselected account-type and currency chips (confirmed via `uiautomator dump`, which reflects exactly the state a screen reader announcement is driven from).
- Verified at the code level (not through live navigation, since no screen currently routes to `MainRoute.Loans` — a pre-existing, unrelated gap, not introduced by this change): `LoansViewModel.loanAccountOptions` filters the same `ListAccountSummariesUseCase` read this dialog also uses, by `type == LOAN_LINKED`, so a Loan-linked account created here does satisfy the Loans prerequisite.
- Not performed: literal TalkBack audio narration, and a dedicated formal Pixel/accessibility sign-off pass. The Pixel Review Plan and Accessibility Review Checklist below remain open until that pass happens.

## Approval Record

Account Creation v2 — the Accounts FAB, dialog-based creation flow, `CASH`/`BANK`/`LOAN_LINKED` account types, and explicit currency selection described in this note — is authorized by direct product-owner instruction on 2026-08-30, which also permitted the supporting JADX analysis recorded under Reference Readiness within the repository's existing JADX boundary (`docs/references/README.md`).

This note does not itself grant approval, and it does not claim Pixel review or accessibility review have passed — both remain open checklist items above until a reviewer actually completes them. Production Compose implementation must satisfy every item in the Functional Acceptance Checklist before this workflow is considered done per `docs/definition-of-done.md`.
