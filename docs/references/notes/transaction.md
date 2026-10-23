# Transaction Entry Screen Reference Note

## Screen Name

Transaction Entry Screen v1

## Primary Reference

- [Expense entry](<../Screenshot_20260630_164401_Money Manager.jpg>)
- [Transfer entry](<../Screenshot_20260630_164416_Money Manager.jpg>)
- [Income entry](<../Screenshot_20260630_164425_Money Manager.jpg>)

These screenshots were not available to the original v1 reference package. They now supersede fallback sources for visible form hierarchy, but do not retroactively validate or approve the production screen. The [transaction reference index](../transaction/README.md) and [history-views note](transaction-history-views.md) keep form and history surfaces distinct.

## Supporting References

- Official Money Manager Play Store: https://play.google.com/store/apps/details?id=com.realbyteapps.moneymanagerfree
- Official Money Manager website: https://realbyteapps.com/
- Existing Tio Ledger UI Foundation components
- Material Design 3 guidelines

## Workflow Summary

Transaction Entry Screen v1 presents a form for capturing transaction data:

1. User taps FAB on transaction list screen
2. Screen opens with transaction type selector (Expense/Income/Transfer)
3. User enters amount via numeric input
4. User selects account from picker
5. User selects category from picker
6. User selects date (defaults to today)
7. User optionally adds note
8. User optionally adds tags (placeholder for v2)
9. User taps Save or Cancel
10. Success: navigates back to transaction list
11. Error: displays validation feedback

Current runtime is no longer UI-only: `TransactionEntryViewModel` invokes `RecordIncomeUseCase`, `RecordExpenseUseCase`, or `RecordTransferUseCase` on Save. The UI owns no ledger calculation or posting rule. Reference screenshots do not alter the approved Application/ledger contracts.

## Information Hierarchy

1. **Transaction Type Selector** (top, tabs or segmented control)
   - Expense (default)
   - Income
   - Transfer

2. **Amount Input** (prominent, large)
   - Numeric keyboard
   - Currency symbol prefix
   - Validation feedback

3. **Account Selector**
   - Current account display
   - Tap to open account picker

4. **Category Selector**
   - Current category display
   - Tap to open category picker
   - Icon + name

5. **Date Selector**
   - Current date display
   - Tap to open date picker
   - Defaults to today

6. **Note Field**
   - Optional text input
   - Multiline support
   - Placeholder text

7. **Tags Section** (placeholder)
   - Visual placeholder for future enhancement
   - Visible placeholder only; not a supported posting field

8. **Action Buttons**
   - Save (primary)
   - Cancel/Back (secondary)

## Intentional Deviations

1. **Layered posting**: the UI delegates saving to Application use cases; financial calculation and ledger posting stay outside Compose.

2. **State-Driven Rendering**: All data comes from state objects, not hardcoded.

3. **Modern Material Design 3**: Using Tio Ledger design tokens and components, not pixel-perfect Money Manager recreation.

4. **Accessibility-First**: Semantic labels, content descriptions, focus order, and touch target sizes per Material Design guidelines.

## Reason For Deviations

1. **Architecture Compliance**: Frozen layers must not be modified. Presentation layer must remain decoupled from business logic.

2. **Phased implementation history**: the earlier UI-only milestone was followed by Transaction Entry Integration v1. This note describes the current integrated flow, not a future promise.

3. **Design System Adherence**: Tio Ledger uses its own design system built on Material Design 3, not Money Manager's legacy styling.

4. **Cross-Platform Foundation**: Compose Multiplatform implementation must work on Android, iOS, and Wear OS.

## Accessibility Considerations

1. **Amount Input**: Content description "Transaction amount input", large touch target, clear focus indicator

2. **Selectors**: Semantic labels for account, category, date pickers

3. **Validation**: Error messages announced via accessibility services

4. **Form Focus Order**: Logical top-to-bottom flow

5. **Touch Targets**: Minimum 48dp per Material Design guidelines

6. **Dynamic Type**: Text scales with system font size settings

7. **Light/Dark Theme**: Full support via Tio design tokens

8. **Screen Reader**: All interactive elements have content descriptions

## JADX Boundary

No copied Java/Kotlin source, XML layouts, resources, drawables, icons, strings, colors, dimensions, animations, assets, or proprietary implementation details are included in this note or implementation.

All implementation is original Tio Ledger code following project architecture, design system, and accessibility guidelines.
