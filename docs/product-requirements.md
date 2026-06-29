# Product Requirements

## Product Summary

Tio Ledger is a personal finance application for tracking accounts, transactions, budgets, loans, and financial insights across Android, iOS, and Wear OS. It should feel fast, private, reliable offline, and trustworthy for calculations that affect real financial decisions.

The application should remain functionally familiar to Money Manager. Tio Ledger modernizes the implementation, accessibility, typography, responsiveness, and visual polish while preserving the core workflows and accounting-first information architecture that existing users understand.

## Primary User Goals

- Record income and expenses quickly.
- Preserve fast, familiar entry workflows.
- Understand balances across cash, bank, credit, and custom accounts.
- Track budgets by category and period.
- Manage loans with accurate EMI schedules and prepayment planning.
- See principal versus interest breakdowns over time.
- Receive useful reminders without noisy notifications.
- Use essential actions from Wear OS.

## Core Functional Areas

### Accounts

- Create, edit, archive, and reorder accounts.
- Support account types such as cash, bank, credit card, wallet, investment placeholder, and loan-linked account.
- Track opening balance and current computed balance.
- Keep account state auditable through transactions rather than silent balance mutation.
- Compute balances from immutable ledger entries.

### Transactions

- Record income, expense, and transfer transactions.
- Support categories, notes, timestamps, payment method, account, tags, and attachments later.
- Support recurring transaction templates in a future phase.
- Maintain immutable transaction IDs for sync readiness.
- Support SMS-assisted transaction capture where available.
- Never create a transaction from SMS without explicit user confirmation.
- Allow users to edit all pre-filled fields before saving.

### Ledger

- Treat the app as a financial ledger, not only an expense tracker.
- Represent every financial operation as one or more ledger entries.
- Support ledger entry types for expense, income, transfer, loan disbursement, EMI payment, interest posting, investment, and refund.
- Keep historical transactions immutable; corrections should use reversal or adjustment entries.
- Ensure every account balance is derivable from ledger entries.

### Categories

- Provide default categories with user customization.
- Support parent-child category hierarchy where useful.
- Allow category-level budgeting and analytics.

### Budgets

- Create monthly, weekly, yearly, and custom-period budgets.
- Track planned versus actual spend.
- Warn users before overspending.
- Support rollover as an optional future extension.

### Loans

- Create loans using principal, annual interest rate, tenure, start date, EMI rules, and payment frequency.
- Generate amortization schedules.
- Track principal, interest, outstanding balance, and payment status.
- Support prepayments.
- Estimate interest savings and tenure reduction from prepayments.
- Support comparisons between original and revised schedules.

### Analytics

- Show spending trends by category and account.
- Show cash flow over time.
- Show debt payoff progress and interest paid.
- Keep analytics derived from local persisted data.

### Notifications

- Remind users about upcoming EMI payments, budget limits, and recurring transaction prompts.
- Keep notification scheduling platform-specific while business rules stay shared.

### Wear OS

- Prioritize quick capture, balance glance, upcoming EMI reminder, and budget warning.
- Avoid complex analytics on watch.
- Reuse shared domain and use case logic.

### SMS Assisted Capture

- Parse bank, credit card, UPI, and wallet messages where platform access allows.
- Work offline with deterministic rules wherever possible.
- Pre-fill amount, account, category, merchant, notes, tags, and date when detected.
- Show parser confidence and request user input when confidence is low.
- Require explicit confirmation before saving any transaction.
- Keep raw SMS content private and avoid unnecessary persistence.

### Smart Automation

- Automation may assist with SMS parsing, category suggestions, merchant detection, loan suggestions, duplicate detection, OCR, and insights.
- Automation must never silently modify financial data.
- Every suggestion must be reviewable and editable.
- User confirmation is required before committing changes.
- Experimental automation must be isolated behind feature flags.

### UX Reference Requirements

- Major UI screens must be implemented from approved reference screenshots stored in `docs/references/`.
- Screenshots are implementation references, not mood-board inspiration.
- If local screenshots are unavailable, use the official Money Manager website, official Play Store screenshots, or approved Tio Ledger mockups in that priority order.
- Do not redesign major workflows without an approved reference.
- Modernization should focus on spacing, typography, animation, accessibility, responsiveness, and platform fit while keeping recognizable interaction patterns.
- Every production UI screen must have an approved screenshot reference, functional specification, navigation definition, and acceptance checklist.
- Any intentional deviation from a reference must be documented with justification and approved before implementation.

## Non-Goals For Initial Release

- Bank account aggregation.
- Real-time cloud collaboration.
- Fully automatic SMS transaction creation.
- Silent automation that modifies financial data.
- Stored balances that cannot be reconciled from the ledger.
- Investment portfolio valuation.
- Tax filing workflows.
- Multi-user household accounting.
- Full double-entry accounting.

## Quality Attributes

- Offline reliability: all critical workflows work without network.
- Data integrity: financial records must be durable and recoverable.
- Testability: engines and use cases must have high unit coverage.
- Portability: platform code should be thin.
- Performance: common screens should load from local data quickly.
- Privacy: user data should remain local unless explicit sync is added.
