package com.tioledger.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TransactionRecord(
    val transaction: Transaction,
    val splits: List<TransactionSplit>,
    val ledgerEntries: List<LedgerEntry>,
)
