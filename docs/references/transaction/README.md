# Transaction Entry Screen References

## Reference Sources

### Primary References

- [Expense entry](<../Screenshot_20260630_164401_Money Manager.jpg>)
- [Transfer entry](<../Screenshot_20260630_164416_Money Manager.jpg>)
- [Income entry](<../Screenshot_20260630_164425_Money Manager.jpg>)
- [Daily history](<../Screenshot_20260630_163942_Money Manager.jpg>) and the other views in [Transaction History Views](../notes/transaction-history-views.md)

### Supporting References

- Official Money Manager Play Store: https://play.google.com/store/apps/details?id=com.realbyteapps.moneymanagerfree
- Official Money Manager website: https://realbyteapps.com/
- Money Manager standard transaction entry workflow patterns

### Historical And Current Status

The initial reference package used for Transaction Entry v1 did not contain form screenshots. The repository now contains the three checked-in form screenshots above. They show visible type, account, category, amount, date, and note relationships; they do not prove hidden validation, posting, or picker behavior. `TransactionEntryScreen` and `TransactionEntryViewModel` now implement an integrated income/expense/transfer flow through Application use cases. Visual deviations from the newly available screenshots need a separate review rather than retroactive approval.

## Transaction Entry UI Patterns

Based on Money Manager standard patterns:

### Transaction Types
- Expense (default)
- Income  
- Transfer

### Required Fields
- Amount input (prominent, top of form)
- Account selector
- Category selector
- Date selector (defaults to today)

### Optional Fields
- Note/description
- Tags remain a visible placeholder in the current entry UI, not a supported posting field.

### Actions
- Save button (primary action)
- Cancel/back navigation

### States
- Empty state (initial)
- Loading state (if needed)
- Error state (validation feedback)

## Compliance Note

This implementation follows the reference-source policy in `docs/references/README.md`:
- No copied code, XML, or proprietary implementation details
- Original Tio Ledger implementation
- Architecture-compliant
- Design system adherence
