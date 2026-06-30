package com.tioledger.data.repository

import com.tioledger.core.model.LedgerError
import com.tioledger.core.model.LedgerResult
import com.tioledger.data.resolver.SystemAccountResolver
import com.tioledger.database.TioLedgerDatabase
import com.tioledger.domain.model.PostingTarget
import com.tioledger.domain.model.TransactionRecord
import com.tioledger.domain.repository.TransactionRepository

class SQLDelightTransactionRepository(
    private val database: TioLedgerDatabase,
) : TransactionRepository {
    override fun record(record: TransactionRecord): LedgerResult<TransactionRecord> {
        val result =
            runDatabaseCatching {
                var isDuplicate = false
                database.transaction {
                    val existing =
                        database.transactionsQueries
                            .selectTransactionById(record.transaction.id)
                            .executeAsOneOrNull()
                    if (existing != null) {
                        isDuplicate = true
                        rollback()
                    }

                    database.transactionsQueries.insertTransaction(
                        id = record.transaction.id,
                        timestamp = record.transaction.timestamp,
                        description = record.transaction.description,
                        type = record.transaction.type.name,
                        merchant_id = record.transaction.merchantId,
                        created_by = record.transaction.createdBy,
                        is_recurring = if (record.transaction.isRecurring) 1L else 0L,
                        created_at = record.transaction.createdAt,
                        updated_at = record.transaction.updatedAt,
                        entity_version = record.transaction.entityVersion.toLong(),
                        sync_version = record.transaction.syncVersion.toLong(),
                        device_id = record.transaction.deviceId,
                        updated_by = record.transaction.updatedBy,
                        deleted_at = record.transaction.deletedAt,
                    )

                    for (split in record.splits) {
                        database.transactionsQueries.insertTransactionSplit(
                            id = split.id,
                            transaction_id = split.transactionId,
                            account_id = split.accountId,
                            category_id = split.categoryId,
                            amount = split.amount.amount,
                            notes = split.notes,
                            created_at = split.createdAt,
                        )
                    }

                    for (entry in record.ledgerEntries) {
                        val mappedAccountId =
                            when (val target = entry.target) {
                                is PostingTarget.Account -> target.accountId
                                is PostingTarget.Virtual -> {
                                    SystemAccountResolver.getMappedAccountId(target, entry.sourceType)
                                }
                            }

                        database.ledgerQueries.insertLedgerEntry(
                            id = entry.id,
                            transaction_id = entry.transactionId,
                            split_id = entry.splitId,
                            account_id = mappedAccountId,
                            amount = entry.amount.amount,
                            entry_type = entry.entryType.name,
                            source_type = entry.sourceType.name,
                            description = entry.description,
                            created_at = entry.createdAt,
                        )
                    }
                }
                if (isDuplicate) {
                    throw IllegalStateException("DUPLICATE_TX:${record.transaction.id}")
                }
                record
            }

        return when (result) {
            is com.tioledger.data.result.DataResult.Success -> {
                LedgerResult.Success(result.value)
            }
            is com.tioledger.data.result.DataResult.Failure -> {
                val err = result.error
                if (err is com.tioledger.data.result.DataError.DatabaseFailure &&
                    err.message.contains("DUPLICATE_TX")
                ) {
                    LedgerResult.Failure(LedgerError.DuplicateTransactionId(record.transaction.id))
                } else {
                    result.toLedgerResult()
                }
            }
        }
    }
}
