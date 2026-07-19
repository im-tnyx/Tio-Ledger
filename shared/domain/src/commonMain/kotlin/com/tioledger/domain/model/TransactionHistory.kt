package com.tioledger.domain.model

import com.tioledger.core.model.Money

data class TransactionHistoryRecord(
    val id: String,
    val timestamp: Long,
    val description: String?,
    val type: TransactionType,
    val splits: List<TransactionHistorySplit>,
)

data class TransactionHistorySplit(
    val id: String,
    val accountId: String,
    val accountName: String,
    val accountType: AccountType,
    val amount: Money,
    val categoryId: String?,
    val categoryName: String?,
    val entryType: LedgerEntryType?,
)
