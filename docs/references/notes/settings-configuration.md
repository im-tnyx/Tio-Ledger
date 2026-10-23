# Settings And Configuration Reference Plan

## Screen And Status

The reference product's `More`/Settings hierarchy includes many more capabilities than Tio Ledger's current Android `MainRoute.Settings`. This note catalogs the checked-in evidence and separates planned product decisions from existing behavior. It does not approve a new Settings UI or preference storage.

## Primary References

- [Settings hub](<../Screenshot_20260630_164134_Money Manager.jpg>)
- [Configuration list](<../Screenshot_20260630_164139_Money Manager.jpg>)
- [Income Category](<../Screenshot_20260630_164241_Money Manager.jpg>) and [Expenses Category](<../Screenshot_20260630_164246_Money Manager.jpg>)
- [Budget Setting](<../Screenshot_20260630_164302_Money Manager.jpg>)
- [Backup/restore/export/reset](<../Screenshot_20260630_164455_Money Manager.jpg>)

Supporting screenshots: [Account Group](<../Screenshot_20260630_164151_Money Manager.jpg>), [Accounts Setting](<../Screenshot_20260630_164204_Money Manager.jpg>), and [PC Manager](<../Screenshot_20260630_164449_Money Manager.jpg>).

## Reference Hierarchy Observed

The top-level reference Settings hub presents Configuration, PC Manager, Backup, Style, Help, and Feedback. Configuration groups account-management entries; income/expense category and budget entries; currency and start-date preferences; interaction/display preferences; and other controls including alarms, style, and language. The images do not define persistence, permissions, backup format, reset semantics, or security safeguards.

## Current Tio Ledger Boundary

The existing typed `MainRoute.Settings` is non-primary and renders Android reminder switches plus notification-delivery state through `AndroidReminderSettingsService`. The approved [reminder note](settings-reminders.md) governs that behavior and must not be weakened. `MainRoute.Categories`, `MainRoute.Budgets`, and `MainRoute.Accounts` are already separate destinations. `MainActivity` follows the device's theme; there is no persisted Appearance override. [Issue #73](https://github.com/im-tnyx/Tio-Ledger/issues/73) tracks System/Light/Dark selection. Backup/export appears only as a Phase 8 roadmap objective; PC Manager and most reference Configuration options have no approved Tio contract.

## Planning Matrix

| Reference group | Proposed handling | Approval still required |
| --- | --- | --- |
| Reminders/notification delivery | Preserve the existing Settings section and Android permission boundary. | Separate #43 acceptance/device status remains open. |
| Appearance/Style | Specify one Tio Appearance section with System default, Light, Dark; reuse the existing theme. | Approve #73 reference note, persistence and UI before implementation. Do not import other Style options. |
| Income/Expense categories | Reuse the existing Categories owner and plan edit/archive/hierarchy separately. | Decide Settings shortcut versus dedicated sub-route; protect referenced history. |
| Budget Setting | Keep Budgets as current financial owner. | Decide whether Settings needs an entry; no duplicate budget computation. |
| Accounts/Account Group | Preserve current Accounts contracts. | Separate account management/type-group requirements and financial safety review. |
| Currency/date/display/language preferences | Inventory only. | Product definitions, storage scope, locale and cross-platform behavior. |
| Backup/import/export/reset | Phase 8 candidate; no UI promise yet. | Recovery format, integrity, privacy, permissions, confirmation, rollback and migration design. |
| PC Manager, Help, Feedback | Inventory only. | Product priority, security/privacy and support ownership. |

## Navigation And Tio UI Specification Gate

Keep the approved five primary bottom destinations. Extend the existing non-primary `MainRoute.Settings` only through separately approved sections or links; do not add a parallel Settings route or copy the reference four-tab shell. A future Settings IA specification must define section order, entry/exit paths, labels, loading/error states, preference write-failure truthfulness, accessibility, and responsive/light/dark behavior. It must explicitly state which reference rows are omitted and why. No new setting should imply a working feature before its runtime contract exists.

## Acceptance For The Next Documentation Decision

- [ ] Owner selects the first bounded Settings addition, separately from the full reference catalog.
- [ ] Its screen note has a functional specification, navigation definition and acceptance checklist.
- [ ] Existing reminder switches and permission action retain their behavior.
- [ ] Preference storage is platform/local or shared only as justified; never in financial tables by convenience.
- [ ] Category/budget/account links do not duplicate business logic or persistence.
- [ ] Backup/reset remains absent until a recoverable, tested data-safety design is approved.
- [ ] Accessibility and pixel review plans cover selected state, large text, touch targets, light/dark and screen-reader order.

## Proprietary Boundary

This note uses screenshots only for hierarchy and workflow discovery. It does not copy source, layout, strings, assets, icons, dimensions, colors, animations, or proprietary implementation details.
