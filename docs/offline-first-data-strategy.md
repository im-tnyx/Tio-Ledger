# Offline-First Data Strategy

## Source Of Truth

The local SQLDelight database is the source of truth. All user-visible finance data should be readable and writable offline.

Within the local database, immutable ledger entries are the accounting source of truth. Account balances are derived from ledger entries.

## Persistence Technology

SQLDelight is preferred because it provides:

- Kotlin Multiplatform support.
- Type-safe SQL.
- Explicit migrations.
- Native drivers for Android and iOS.
- Clear query ownership.

Room can be considered only for Android-specific surfaces that do not need shared persistence, but the default persistence layer should be SQLDelight.

## Entity Design

Every sync-relevant table should include:

- Stable ID.
- Created timestamp.
- Updated timestamp.
- Deleted timestamp where soft deletion is useful.
- Revision or version field if later sync conflict resolution requires it.

## Local Transaction Boundaries

Use database transactions for operations that must remain consistent:

- Creating a loan and its initial schedule metadata.
- Recording an EMI payment and related transaction.
- Recording a transfer between accounts.
- Applying a prepayment and persisting recalculated loan projection metadata.
- Updating category mappings used by budgets.
- Writing multi-entry ledger operations such as transfers, EMI payments, refunds, and loan disbursements.

## Ledger Storage

Financial operations should be stored as immutable ledger entries. A user-facing transaction can create one or more entries depending on operation type.

Examples:

- Expense: account outflow associated with an expense category.
- Income: account inflow associated with an income category.
- Transfer: outflow from source account and inflow to destination account.
- Loan disbursement: loan liability and receiving account movement.
- EMI payment: principal reduction, interest posting, and payment account outflow.
- Refund: reversal or linked inflow against an original expense.

Balance snapshots may be introduced for performance only if they are derived, versioned, and reconcilable from ledger entries.

## Repository Behavior

Repositories should:

- Expose domain models, not database rows.
- Own mapping between storage and domain.
- Return reactive streams where screens need live updates.
- Avoid doing financial calculations inline when an engine API exists.
- Keep write methods idempotent where possible.

## Future Sync Compatibility

The initial app can be local-only. However, the architecture should allow a future sync layer:

```text
Repository
  -> Local data source
  -> Sync metadata store
  -> Remote data source, future
```

Future sync must not require UI rewrites or engine rewrites.

## Backup And Export

Planned extension points:

- Local encrypted backup.
- CSV export.
- JSON export using kotlinx.serialization.
- Restore with schema version validation.

## Data Integrity Rules

- Never silently mutate account balances without an auditable record.
- Store money in minor units and currency code.
- Store user-entered loan terms separately from generated projections.
- Generated loan schedules can be cached, but must be reproducible from loan terms and payment events.
- Migrations must be tested before release.
- SMS-parsed suggestions are not persisted as transactions unless the user explicitly confirms them.
- Avoid storing raw SMS content unless the user intentionally saves it as a transaction note.
- Historical transactions and ledger entries are immutable.
- Corrections use reversal, adjustment, or replacement flows instead of in-place mutation.
- Every stored balance projection must be derivable from ledger entries.
