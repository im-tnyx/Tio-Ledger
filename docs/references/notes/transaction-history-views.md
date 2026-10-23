# Transaction History Views Reference And Planning Note

## Screen And Status

Reference area: Transactions `Daily`, `Calendar`, `Monthly`, `Total`, and `Note`. This note documents checked-in screenshots and the implementation gap. It is **not** approval to build a five-tab copy or change the canonical Tio navigation.

## Primary References

- [Daily empty state](<../Screenshot_20260630_163942_Money Manager.jpg>)
- [Calendar month grid](<../Screenshot_20260630_163948_Money Manager.jpg>)
- [Monthly and weekly rows](<../Screenshot_20260630_163952_Money Manager.jpg>)
- [Total budget summary](<../Screenshot_20260630_163957_Money Manager.jpg>) and [Total continuation](<../Screenshot_20260630_164004_Money Manager.jpg>)
- [Note empty state](<../Screenshot_20260630_164013_Money Manager.jpg>)

## Observed Workflow And Information Hierarchy

The reference keeps the selected period, view selector, content, and transaction-add affordance together within the Transactions area. `Daily` shows period totals and a chronological history or empty state. `Calendar` changes the same content area to a month grid with a selected date. `Monthly` summarizes months with smaller time-group rows. `Total` combines budget progress, account-related summary, and an export entry. `Note` presents a separate note-oriented collection. The images do not prove date-selection rules, row expansion behavior, note storage, or export format.

## Tio Ledger Runtime Boundary

`TransactionsScreen` currently renders a flat persisted-history list with loading, empty, error, retry, add, and SMS-review entry states. `TransactionEntryScreen` supports income, expense, transfer, date/account/category/amount, and an optional note. `ReportsScreen` has separate Week/Month/Year analytics. There is no `Daily`/`Calendar`/`Monthly`/`Total`/`Note` history selector or calendar grid in the current route. `Budgets` and `Accounts` are separate destinations. These are deliberate descriptions of source state, not claims of visual parity.

## Planning Decisions Required Before UI Specification

1. Decide which of the five reference views belong in Tio Ledger Transactions, which reuse existing Budgets/Accounts/Reports, and which are deferred. Do not duplicate financial totals or add a sixth primary bottom destination.
2. Define period, timezone, week-start, month-start, and day-selection semantics from the application/domain contracts before showing financial aggregates. The screenshots do not determine these rules.
3. Specify whether `Note` means transaction-note filtering or a separate note record type. Do not create a new financial or note data model from the image alone.
4. Specify export separately with format, privacy, permissions, and recovery expectations. The screenshot's export entry is not a safe implementation contract.
5. For each approved view, record navigation entry/exit, empty/loading/error states, accessibility, light/dark and large-text behavior, and focused tests before Compose work.

## Reference-Driven Acceptance Checklist For A Future Slice

- [ ] Owner approves the selected view(s) and their relationship to existing Tio routes.
- [ ] An Application-owned read model defines all monetary totals; Compose does not calculate money or mutate ledger history.
- [ ] Date/period controls have deterministic tests, including month boundaries and timezone changes.
- [ ] Empty and populated states are distinct and accessible without color alone.
- [ ] Transactions, Reports, Budgets, and Accounts navigation remains coherent.
- [ ] Pixel and accessibility reviews run on actual target configurations before claiming parity.

## Deviations And Accessibility

Tio Ledger's five primary destinations, separate Reports/Budgets/Accounts routes, ledger-first rules, and shared design system supersede a literal reference-tab copy. These differences require an explicit product decision before a new production UI slice. Any future selector must expose selected state to screen readers; calendar dates need date/amount semantics and predictable focus; large text must not hide period controls or financial totals.

No proprietary code, layout, resource, asset, color, dimension, or animation is imported from the reference.
