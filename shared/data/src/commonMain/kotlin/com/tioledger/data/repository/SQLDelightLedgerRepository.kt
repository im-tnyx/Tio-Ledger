package com.tioledger.data.repository

import com.tioledger.core.model.LedgerResult
import com.tioledger.data.mapper.toDomain
import com.tioledger.database.TioLedgerDatabase
import com.tioledger.domain.model.LedgerEntry
import com.tioledger.domain.repository.LedgerRepository

class SQLDelightLedgerRepository(
    private val database: TioLedgerDatabase,
) : LedgerRepository {
    override fun findEntriesByAccount(accountId: String): LedgerResult<List<LedgerEntry>> {
        val result =
            runDatabaseCatching {
                database.ledgerQueries
                    .selectLedgerEntriesByAccountId(accountId)
                    .executeAsList()
                    .map { it.toDomain() }
            }
        return result.toLedgerResult()
    }

    override fun findEntriesByTransaction(transactionId: String): LedgerResult<List<LedgerEntry>> {
        val result =
            runDatabaseCatching {
                database.ledgerQueries
                    .selectLedgerEntriesByTransactionId(transactionId)
                    .executeAsList()
                    .map { it.toDomain() }
            }
        return result.toLedgerResult()
    }
}
