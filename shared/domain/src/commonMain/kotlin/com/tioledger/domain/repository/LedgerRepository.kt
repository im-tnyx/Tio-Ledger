package com.tioledger.domain.repository

import com.tioledger.core.model.LedgerResult
import com.tioledger.domain.model.LedgerEntry

interface LedgerRepository {
    fun findEntriesByAccount(accountId: String): LedgerResult<List<LedgerEntry>>

    fun findEntriesByTransaction(transactionId: String): LedgerResult<List<LedgerEntry>>
}
