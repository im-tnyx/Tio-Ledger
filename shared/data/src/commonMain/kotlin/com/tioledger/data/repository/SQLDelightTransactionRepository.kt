package com.tioledger.data.repository

import com.tioledger.core.model.CurrencyCode
import com.tioledger.core.model.LedgerError
import com.tioledger.core.model.LedgerResult
import com.tioledger.core.model.Money
import com.tioledger.data.resolver.SystemAccountResolver
import com.tioledger.database.TioLedgerDatabase
import com.tioledger.domain.model.AccountType
import com.tioledger.domain.model.LedgerEntryType
import com.tioledger.domain.model.PostingTarget
import com.tioledger.domain.model.TransactionHistoryRecord
import com.tioledger.domain.model.TransactionHistorySplit
import com.tioledger.domain.model.TransactionRecord
import com.tioledger.domain.model.TransactionType
import com.tioledger.domain.repository.TransactionHistoryRepository
import com.tioledger.domain.repository.TransactionRepository

class SQLDelightTransactionRepository(
    private val database: TioLedgerDatabase,
) : TransactionRepository, TransactionHistoryRepository {
    override fun findAll(): LedgerResult<List<TransactionHistoryRecord>> {
        val result =
            runDatabaseCatching {
                database.transactionsQueries
                    .selectTransactionHistoryRows {
                        transactionId,
                        timestamp,
                        description,
                        transactionType,
                        splitId,
                        amount,
                        accountId,
                        accountName,
                        accountType,
                        currencyCode,
                        categoryId,
                        categoryName,
                        ledgerEntryType,
                        ->
                        TransactionHistoryRow(
                            transactionId = transactionId,
                            timestamp = timestamp,
                            description = description,
                            transactionType = TransactionType.valueOf(transactionType),
                            splitId = splitId,
                            amount = amount,
                            accountId = accountId,
                            accountName = accountName,
                            accountType = AccountType.valueOf(accountType),
                            currencyCode = currencyCode,
                            categoryId = categoryId,
                            categoryName = categoryName,
                            entryType = ledgerEntryType?.let(LedgerEntryType::valueOf),
                        )
                    }
                    .executeAsList()
                    .toTransactionHistoryRecords()
            }
        return result.toLedgerResult()
    }

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

private data class TransactionHistoryRow(
    val transactionId: String,
    val timestamp: Long,
    val description: String?,
    val transactionType: TransactionType,
    val splitId: String,
    val amount: Long,
    val accountId: String,
    val accountName: String,
    val accountType: AccountType,
    val currencyCode: String,
    val categoryId: String?,
    val categoryName: String?,
    val entryType: LedgerEntryType?,
)

private fun List<TransactionHistoryRow>.toTransactionHistoryRecords(): List<TransactionHistoryRecord> =
    groupBy(TransactionHistoryRow::transactionId)
        .values
        .map { rows ->
            val transaction = rows.first()
            TransactionHistoryRecord(
                id = transaction.transactionId,
                timestamp = transaction.timestamp,
                description = transaction.description,
                type = transaction.transactionType,
                splits =
                    rows.map { row ->
                        TransactionHistorySplit(
                            id = row.splitId,
                            accountId = row.accountId,
                            accountName = row.accountName,
                            accountType = row.accountType,
                            amount = Money(row.amount, CurrencyCode(row.currencyCode)),
                            categoryId = row.categoryId,
                            categoryName = row.categoryName,
                            entryType = row.entryType,
                        )
                    },
            )
        }
        .sortedWith(
            compareByDescending<TransactionHistoryRecord> { it.timestamp }
                .thenByDescending { it.id },
        )
