package com.tioledger.domain.repository

import com.tioledger.core.model.LedgerResult
import com.tioledger.domain.model.TransactionHistoryRecord

interface TransactionHistoryRepository {
    fun findAll(): LedgerResult<List<TransactionHistoryRecord>>
}
