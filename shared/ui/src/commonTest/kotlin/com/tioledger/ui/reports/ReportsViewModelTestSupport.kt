package com.tioledger.ui.reports

import com.tioledger.analytics.SpendingAnalyticsCalculator
import com.tioledger.application.usecase.analytics.GetSpendingAnalyticsUseCase
import com.tioledger.core.model.CurrencyCode
import com.tioledger.core.model.LedgerResult
import com.tioledger.core.model.Money
import com.tioledger.domain.model.AccountType
import com.tioledger.domain.model.LedgerEntryType
import com.tioledger.domain.model.TransactionHistoryRecord
import com.tioledger.domain.model.TransactionHistorySplit
import com.tioledger.domain.model.TransactionType
import com.tioledger.domain.repository.TransactionHistoryRepository

internal fun reportsViewModel(
    repository: TransactionHistoryRepository,
    nowProvider: () -> Long = { JULY_19_2026_UTC },
    timeZoneIdProvider: () -> String = { "UTC" },
): ReportsViewModel =
    ReportsViewModel(
        getSpendingAnalyticsUseCase = GetSpendingAnalyticsUseCase(repository, SpendingAnalyticsCalculator()),
        nowProvider = nowProvider,
        timeZoneIdProvider = timeZoneIdProvider,
    )

internal fun reportRecord(
    id: String,
    type: TransactionType,
    amount: Long,
    currencyCode: String = "INR",
    accountId: String = "wallet",
    accountName: String = "Wallet",
    categoryId: String? = null,
    categoryName: String? = null,
    timestamp: Long = JULY_19_2026_UTC,
): TransactionHistoryRecord =
    TransactionHistoryRecord(
        id = id,
        timestamp = timestamp,
        description = null,
        type = type,
        splits =
            listOf(
                TransactionHistorySplit(
                    id = "$id-split",
                    accountId = accountId,
                    accountName = accountName,
                    accountType = AccountType.WALLET,
                    amount = Money(amount, CurrencyCode(currencyCode)),
                    categoryId = categoryId,
                    categoryName = categoryName,
                    entryType = LedgerEntryType.CREDIT,
                ),
            ),
    )

internal class FakeReportsRepository(
    records: List<TransactionHistoryRecord> = emptyList(),
) : TransactionHistoryRepository {
    var result: LedgerResult<List<TransactionHistoryRecord>> = LedgerResult.Success(records)

    override fun findAll(): LedgerResult<List<TransactionHistoryRecord>> = result
}

internal const val JULY_19_2026_UTC = 1_784_419_200_000L
