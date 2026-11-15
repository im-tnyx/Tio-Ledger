package com.tioledger.domain.repository

import com.tioledger.core.model.LedgerError
import com.tioledger.core.model.LedgerResult
import com.tioledger.domain.model.Account
import com.tioledger.domain.model.TransactionRecord

interface AccountRepository {
    fun findAll(includeArchived: Boolean = false): LedgerResult<List<Account>>

    fun findById(accountId: String): LedgerResult<Account>

    fun create(account: Account): LedgerResult<Account>

    /** Persist an account and its initial ledger posting as one local database operation. */
    fun createWithOpeningBalance(
        account: Account,
        openingBalance: TransactionRecord,
    ): LedgerResult<Account> = LedgerResult.Failure(LedgerError.StorageUnavailable)

    fun update(account: Account): LedgerResult<Account>
}
