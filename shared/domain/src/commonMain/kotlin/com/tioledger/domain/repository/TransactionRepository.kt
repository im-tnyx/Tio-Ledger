package com.tioledger.domain.repository

import com.tioledger.core.model.LedgerResult
import com.tioledger.domain.model.TransactionRecord

interface TransactionRepository {
    fun record(record: TransactionRecord): LedgerResult<TransactionRecord>
}
