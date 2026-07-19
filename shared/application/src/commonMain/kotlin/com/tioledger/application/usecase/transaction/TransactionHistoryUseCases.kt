package com.tioledger.application.usecase.transaction

import com.tioledger.application.model.ApplicationError
import com.tioledger.application.model.ApplicationResult
import com.tioledger.application.model.UseCaseOutcome
import com.tioledger.core.model.LedgerResult
import com.tioledger.core.model.Money
import com.tioledger.domain.model.AccountType
import com.tioledger.domain.model.LedgerEntryType
import com.tioledger.domain.model.TransactionHistoryRecord
import com.tioledger.domain.model.TransactionHistorySplit
import com.tioledger.domain.model.TransactionType
import com.tioledger.domain.repository.TransactionHistoryRepository

data class TransactionSummary(
    val id: String,
    val timestamp: Long,
    val description: String?,
    val type: TransactionType,
    val amount: Money,
    val sourceAccountId: String,
    val sourceAccountName: String,
    val destinationAccountId: String?,
    val destinationAccountName: String?,
    val categoryId: String?,
    val categoryName: String?,
)

class ListTransactionsUseCase(
    private val transactionHistoryRepository: TransactionHistoryRepository,
) {
    operator fun invoke(): ApplicationResult<List<TransactionSummary>> =
        when (val result = transactionHistoryRepository.findAll()) {
            is LedgerResult.Success ->
                ApplicationResult.Success(
                    UseCaseOutcome(
                        value =
                            result.value
                                .mapNotNull(TransactionHistoryRecord::toSummary)
                                .sortedWith(
                                    compareByDescending<TransactionSummary> { it.timestamp }
                                        .thenByDescending { it.id },
                                ),
                    ),
                )
            is LedgerResult.Failure -> ApplicationResult.Failure(ApplicationError.Repository(result.error))
        }
}

private fun TransactionHistoryRecord.toSummary(): TransactionSummary? {
    val firstSplit = splits.firstOrNull() ?: return null
    val sourceSplit =
        if (type == TransactionType.TRANSFER) {
            splits.firstOrNull(TransactionHistorySplit::decreasesBalance) ?: firstSplit
        } else {
            firstSplit
        }
    val destinationSplit =
        if (type == TransactionType.TRANSFER) {
            splits.firstOrNull { it.id != sourceSplit.id }
        } else {
            null
        }
    val categorySplit = splits.firstOrNull { it.categoryId != null }

    return TransactionSummary(
        id = id,
        timestamp = timestamp,
        description = description?.trim()?.takeIf { it.isNotEmpty() },
        type = type,
        amount = sourceSplit.amount,
        sourceAccountId = sourceSplit.accountId,
        sourceAccountName = sourceSplit.accountName,
        destinationAccountId = destinationSplit?.accountId,
        destinationAccountName = destinationSplit?.accountName,
        categoryId = categorySplit?.categoryId,
        categoryName = categorySplit?.categoryName,
    )
}

private fun TransactionHistorySplit.decreasesBalance(): Boolean =
    when (accountType) {
        AccountType.CREDIT_CARD,
        AccountType.LOAN_LINKED,
        -> entryType == LedgerEntryType.DEBIT

        AccountType.CASH,
        AccountType.BANK,
        AccountType.WALLET,
        AccountType.INVESTMENT,
        -> entryType == LedgerEntryType.CREDIT
    }
