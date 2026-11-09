package com.tioledger.data.repository

import com.tioledger.core.model.LedgerError
import com.tioledger.core.model.LedgerResult
import com.tioledger.data.mapper.toDomain
import com.tioledger.data.resolver.SystemAccountResolver
import com.tioledger.data.result.DataResult
import com.tioledger.database.TioLedgerDatabase
import com.tioledger.domain.model.Account
import com.tioledger.domain.model.PostingTarget
import com.tioledger.domain.model.TransactionRecord
import com.tioledger.domain.repository.AccountRepository

class SQLDelightAccountRepository(
    private val database: TioLedgerDatabase,
) : AccountRepository {
    override fun findAll(includeArchived: Boolean): LedgerResult<List<Account>> {
        val result =
            runDatabaseCatching {
                database.accountsQueries
                    .selectAllAccounts()
                    .executeAsList()
                    .map { it.toDomain() }
                    .filter { includeArchived || !it.isArchived }
            }
        return result.toLedgerResult()
    }

    override fun findById(accountId: String): LedgerResult<Account> {
        val result =
            runDatabaseCatching {
                database.accountsQueries
                    .selectAccountById(accountId)
                    .executeAsOneOrNull()
            }
        return when (result) {
            is com.tioledger.data.result.DataResult.Success -> {
                val account = result.value
                if (account != null) {
                    LedgerResult.Success(account.toDomain())
                } else {
                    LedgerResult.Failure(LedgerError.AccountNotFound(accountId))
                }
            }
            is com.tioledger.data.result.DataResult.Failure -> {
                result.toLedgerResult()
            }
        }
    }

    override fun create(account: Account): LedgerResult<Account> {
        val existingResult =
            runDatabaseCatching {
                database.accountsQueries
                    .selectAccountById(account.id)
                    .executeAsOneOrNull()
            }
        if (existingResult is com.tioledger.data.result.DataResult.Success && existingResult.value != null) {
            return LedgerResult.Failure(LedgerError.DuplicateAccountId(account.id))
        }

        val result =
            runDatabaseCatching {
                database.accountsQueries.insertAccount(
                    id = account.id,
                    name = account.name,
                    type = account.type.name,
                    currency_code = account.currencyCode,
                    is_archived = if (account.isArchived) 1L else 0L,
                    display_order = account.displayOrder.toLong(),
                    created_at = account.createdAt,
                    updated_at = account.updatedAt,
                    entity_version = account.entityVersion.toLong(),
                    sync_version = account.syncVersion.toLong(),
                    device_id = account.deviceId,
                    updated_by = account.updatedBy,
                    deleted_at = account.deletedAt,
                )
                account
            }
        return result.toLedgerResult { LedgerError.DuplicateAccountId(account.id) }
    }

    override fun createWithOpeningBalance(
        account: Account,
        openingBalance: TransactionRecord,
    ): LedgerResult<Account> {
        var duplicateAccount = false
        var duplicateTransaction = false
        val result =
            runDatabaseCatching {
                database.transaction {
                    if (database.accountsQueries.selectAccountById(account.id).executeAsOneOrNull() != null) {
                        duplicateAccount = true
                        rollback()
                    }
                    if (
                        database.transactionsQueries
                            .selectTransactionById(openingBalance.transaction.id)
                            .executeAsOneOrNull() != null
                    ) {
                        duplicateTransaction = true
                        rollback()
                    }

                    if (!duplicateAccount && !duplicateTransaction) {
                        database.accountsQueries.insertAccount(
                            id = account.id,
                            name = account.name,
                            type = account.type.name,
                            currency_code = account.currencyCode,
                            is_archived = if (account.isArchived) 1L else 0L,
                            display_order = account.displayOrder.toLong(),
                            created_at = account.createdAt,
                            updated_at = account.updatedAt,
                            entity_version = account.entityVersion.toLong(),
                            sync_version = account.syncVersion.toLong(),
                            device_id = account.deviceId,
                            updated_by = account.updatedBy,
                            deleted_at = account.deletedAt,
                        )
                        database.transactionsQueries.insertTransaction(
                            id = openingBalance.transaction.id,
                            timestamp = openingBalance.transaction.timestamp,
                            description = openingBalance.transaction.description,
                            type = openingBalance.transaction.type.name,
                            merchant_id = openingBalance.transaction.merchantId,
                            created_by = openingBalance.transaction.createdBy,
                            is_recurring = if (openingBalance.transaction.isRecurring) 1L else 0L,
                            created_at = openingBalance.transaction.createdAt,
                            updated_at = openingBalance.transaction.updatedAt,
                            entity_version = openingBalance.transaction.entityVersion.toLong(),
                            sync_version = openingBalance.transaction.syncVersion.toLong(),
                            device_id = openingBalance.transaction.deviceId,
                            updated_by = openingBalance.transaction.updatedBy,
                            deleted_at = openingBalance.transaction.deletedAt,
                        )
                        openingBalance.splits.forEach { split ->
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
                        openingBalance.ledgerEntries.forEach { entry ->
                            val mappedAccountId =
                                when (val target = entry.target) {
                                    is PostingTarget.Account -> target.accountId
                                    is PostingTarget.Virtual -> SystemAccountResolver.getMappedAccountId(target, entry.sourceType)
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
                }
                if (duplicateAccount) throw IllegalStateException("DUPLICATE_ACCOUNT:${account.id}")
                if (duplicateTransaction) throw IllegalStateException("DUPLICATE_TX:${openingBalance.transaction.id}")
                account
            }
        return when (result) {
            is DataResult.Success -> LedgerResult.Success(result.value)
            is DataResult.Failure -> {
                when {
                    duplicateAccount -> LedgerResult.Failure(LedgerError.DuplicateAccountId(account.id))
                    duplicateTransaction -> LedgerResult.Failure(LedgerError.DuplicateTransactionId(openingBalance.transaction.id))
                    else -> result.toLedgerResult()
                }
            }
        }
    }

    override fun update(account: Account): LedgerResult<Account> {
        val existingResult =
            runDatabaseCatching {
                database.accountsQueries
                    .selectAccountById(account.id)
                    .executeAsOneOrNull()
            }
        if (existingResult is com.tioledger.data.result.DataResult.Success && existingResult.value == null) {
            return LedgerResult.Failure(LedgerError.AccountNotFound(account.id))
        }

        val result =
            runDatabaseCatching {
                database.accountsQueries.updateAccount(
                    name = account.name,
                    type = account.type.name,
                    currency_code = account.currencyCode,
                    is_archived = if (account.isArchived) 1L else 0L,
                    display_order = account.displayOrder.toLong(),
                    updated_at = account.updatedAt,
                    entity_version = account.entityVersion.toLong(),
                    sync_version = account.syncVersion.toLong(),
                    device_id = account.deviceId,
                    updated_by = account.updatedBy,
                    deleted_at = account.deletedAt,
                    id = account.id,
                )
                account
            }
        return result.toLedgerResult { LedgerError.AccountNotFound(account.id) }
    }
}
