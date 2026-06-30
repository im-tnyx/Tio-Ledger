package com.tioledger.data.repository

import com.tioledger.core.model.LedgerError
import com.tioledger.core.model.LedgerResult
import com.tioledger.data.mapper.toDomain
import com.tioledger.database.TioLedgerDatabase
import com.tioledger.domain.model.Account
import com.tioledger.domain.repository.AccountRepository

class SQLDelightAccountRepository(
    private val database: TioLedgerDatabase,
) : AccountRepository {
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
