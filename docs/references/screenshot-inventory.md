# Checked-in Money Manager Screenshot Inventory

This is an evidence map for the 24 screenshots at the root of `docs/references/`, captured on 2026-06-30. It records what is visible, not an approval to copy the reference product or implement every pictured feature. Four additional files under `accounts/` are copies of root screenshots 164106, 164124, 164134, and 164139; they are not four more distinct views. Product requirements, Tio Ledger architecture, and a screen-specific approved specification still govern implementation.

| Screenshot | Visible reference area | Tio Ledger mapping at `main@e50ae964` |
| --- | --- | --- |
| [163942](<Screenshot_20260630_163942_Money Manager.jpg>) | Transactions: Daily, empty month, summary, add action | Flat Transactions history and entry exist; Daily tab is not implemented. |
| [163948](<Screenshot_20260630_163948_Money Manager.jpg>) | Transactions: Calendar month grid and selected day | Calendar transaction view is not implemented. |
| [163952](<Screenshot_20260630_163952_Money Manager.jpg>) | Transactions: Monthly totals and expandable weekly rows | Monthly/weekly history view is not implemented. |
| [163957](<Screenshot_20260630_163957_Money Manager.jpg>) | Transactions: Total, budget progress and account summary | Budgets and Accounts exist as separate Tio destinations; this combined Total view does not. |
| [164004](<Screenshot_20260630_164004_Money Manager.jpg>) | Continuation of Total, including account comparison and export entry | Combined Total/export workflow is not implemented; backup/export is Phase 8 planning. |
| [164013](<Screenshot_20260630_164013_Money Manager.jpg>) | Transactions: Note tab, empty state and add action | Transaction entry has a note field; a separate Note collection/view is not implemented. |
| [164023](<Screenshot_20260630_164023_Money Manager.jpg>) | Stats: Income, monthly period, empty state | Reports exists with Week/Month/Year and text-first results; this Stats surface is not reproduced. |
| [164027](<Screenshot_20260630_164027_Money Manager.jpg>) | Stats: Expenses, monthly period, empty state | Reports has expense breakdowns; this Stats surface is not reproduced. |
| [164048](<Screenshot_20260630_164048_Money Manager.jpg>) | Stats: annual expense category pie and ranked amounts | Reports has text category breakdown; chart and reference period presentation need separate approval. |
| [164058](<Screenshot_20260630_164058_Money Manager.jpg>) | Stats: annual income category pie and ranked amounts | Reports has text-first income totals; an income-category chart is not implemented. |
| [164106](<Screenshot_20260630_164106_Money Manager.jpg>) | Accounts: asset/liability summary and grouped account rows | Accounts destination exists; see [Accounts note](notes/account.md) for its approved scope and deviations. |
| [164124](<Screenshot_20260630_164124_Money Manager.jpg>) | Accounts: Total Stats charts | Account Total Stats chart is not implemented; loan/reports analytics have separate contracts. |
| [164134](<Screenshot_20260630_164134_Money Manager.jpg>) | More/Settings hub: Configuration, PC Manager, Backup, Style, Help, Feedback | Tio Settings is a non-primary route with reminder controls; hub/options need product decisions. |
| [164139](<Screenshot_20260630_164139_Money Manager.jpg>) | Configuration list: accounts, categories, budgets, currency, other preferences | See [Settings configuration plan](notes/settings-configuration.md); no blanket feature approval. |
| [164151](<Screenshot_20260630_164151_Money Manager.jpg>) | Account Group management | No matching Tio management screen; account type/group contract needs separate review. |
| [164204](<Screenshot_20260630_164204_Money Manager.jpg>) | Account Settings grouped by account type | Tio Accounts list and creation differ; edit/archive/reorder scope needs its own decision. |
| [164241](<Screenshot_20260630_164241_Money Manager.jpg>) | Income Category management, subcategory toggle, add/edit/remove/reorder affordances | Categories v1 lists and creates; management actions are not exposed. See [Category note](notes/category.md). |
| [164246](<Screenshot_20260630_164246_Money Manager.jpg>) | Expense Category management with child-category summaries | Categories v1 does not expose hierarchy or management actions. See [Category note](notes/category.md). |
| [164302](<Screenshot_20260630_164302_Money Manager.jpg>) | Budget Setting: total and category budgets | Tio Budgets create/edit exists on a separate primary route; see [Budgets note](notes/budgets.md). |
| [164401](<Screenshot_20260630_164401_Money Manager.jpg>) | Expense entry with account, category, amount and note | Tio Transaction Entry supports expense; see [Transaction note](notes/transaction.md). |
| [164416](<Screenshot_20260630_164416_Money Manager.jpg>) | Transfer entry with source/destination accounts | Tio Transaction Entry supports transfer; reference extras require separate review. |
| [164425](<Screenshot_20260630_164425_Money Manager.jpg>) | Income entry with account, category and amount | Tio Transaction Entry supports income. |
| [164449](<Screenshot_20260630_164449_Money Manager.jpg>) | PC Manager local-network access | Not a Tio Ledger approved feature; security/privacy design is required before any proposal. |
| [164455](<Screenshot_20260630_164455_Money Manager.jpg>) | Backup, restore, export/import, reset | Backup/export is a Phase 8 objective; formats, restore safety, and destructive reset are undecided. |

## Interpretation And Next Decisions

- The reference product has four primary tabs (`Trans.`, `Stats`, `Accounts`, `More`). Tio Ledger already has five approved primary destinations: Dashboard, Accounts, Transactions, Categories, and Budgets. This inventory does **not** approve replacing that navigation contract.
- A screenshot proves visible hierarchy and affordances, not hidden behavior, persistence rules, accessibility, or pixel-level implementation. Do not infer those from the image.
- The top priority planning gaps are the five Transactions views, Category management, and a coherent Settings information architecture. Appearance is already tracked by [issue #73](https://github.com/im-tnyx/Tio-Ledger/issues/73). Android reminder delivery remains tracked by [issue #43](https://github.com/im-tnyx/Tio-Ledger/issues/43).
- Account Group, Account Settings, chart variants, PC Manager, backup/import/export, reset, and numerous Configuration preferences are evidence for product triage, not automatic requirements. Backup/restore/reset in particular need explicit data-integrity and security decisions.
- No checked-in root screenshot establishes Dashboard content. The `Dashboard` route currently renders a placeholder; see the [Dashboard note](notes/dashboard.md) before claiming a production screen.
- Before implementation, each selected slice still needs the [reference workflow](README.md): an approved screen note, functional specification, navigation definition, acceptance checklist, deviation approval, pixel review, and accessibility review.
