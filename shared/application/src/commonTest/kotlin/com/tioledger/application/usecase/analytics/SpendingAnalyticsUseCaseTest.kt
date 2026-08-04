package com.tioledger.application.usecase.analytics

import com.tioledger.analytics.SpendingAnalyticsCalculator
import com.tioledger.application.model.ApplicationError
import com.tioledger.application.model.ApplicationResult
import com.tioledger.core.model.CurrencyCode
import com.tioledger.core.model.LedgerError
import com.tioledger.core.model.LedgerResult
import com.tioledger.core.model.Money
import com.tioledger.domain.model.AccountType
import com.tioledger.domain.model.LedgerEntryType
import com.tioledger.domain.model.TransactionHistoryRecord
import com.tioledger.domain.model.TransactionHistorySplit
import com.tioledger.domain.model.TransactionType
import com.tioledger.domain.repository.TransactionHistoryRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpendingAnalyticsUseCaseTest {
    @Test
    fun returnsPeriodScopedCurrencyReportsWithCashFlowBuckets() {
        val useCase =
            GetSpendingAnalyticsUseCase(
                transactionHistoryRepository =
                    FakeTransactionHistoryRepository(
                        LedgerResult.Success(
                            listOf(
                                record("income", TransactionType.INCOME, "acc-bank", "Bank", "INR", 90_000L, "salary", "Salary"),
                                record("expense", TransactionType.EXPENSE, "acc-wallet", "Wallet", "INR", 5_000L, "food", "Food"),
                            ),
                        ),
                    ),
                calculator = SpendingAnalyticsCalculator(),
            )

        val result = useCase(SpendingReportPeriod.MONTHLY, JULY_19_2026_UTC, "UTC")

        assertTrue(result is ApplicationResult.Success)
        val report = result.outcome.value
        assertEquals(SpendingReportPeriod.MONTHLY, report.period)
        assertEquals(1, report.currencyReports.size)
        val currencyReport = report.currencyReports.single()
        assertEquals("INR", currencyReport.currencyCode)
        assertEquals(Money(90_000L, CurrencyCode("INR")), currencyReport.income)
        assertEquals(Money(5_000L, CurrencyCode("INR")), currencyReport.expense)
        assertEquals("Food", currencyReport.categoryBreakdown.single().categoryName)
        assertEquals(31, currencyReport.cashFlowBuckets.size)
        val july19 = currencyReport.cashFlowBuckets[18]
        assertEquals(Money(90_000L, CurrencyCode("INR")), july19.income)
        assertEquals(Money(5_000L, CurrencyCode("INR")), july19.expense)
        assertEquals(Money(85_000L, CurrencyCode("INR")), july19.net)
    }

    @Test
    fun mapsRepositoryFailure() {
        val result =
            GetSpendingAnalyticsUseCase(
                transactionHistoryRepository =
                    FakeTransactionHistoryRepository(
                        LedgerResult.Failure(LedgerError.Unknown("db unavailable")),
                    ),
                calculator = SpendingAnalyticsCalculator(),
            )(
                period = SpendingReportPeriod.WEEKLY,
                anchorTimestamp = JULY_19_2026_UTC,
                timeZoneId = "UTC",
            )

        assertTrue(result is ApplicationResult.Failure)
        assertTrue(result.error is ApplicationError.Repository)
    }

    private fun record(
        id: String,
        type: TransactionType,
        accountId: String,
        accountName: String,
        currencyCode: String,
        amount: Long,
        categoryId: String?,
        categoryName: String?,
    ): TransactionHistoryRecord =
        TransactionHistoryRecord(
            id = id,
            timestamp = JULY_19_2026_UTC,
            description = null,
            type = type,
            splits =
                listOf(
                    TransactionHistorySplit(
                        id = "$id-split",
                        accountId = accountId,
                        accountName = accountName,
                        accountType = AccountType.BANK,
                        amount = Money(amount, CurrencyCode(currencyCode)),
                        categoryId = categoryId,
                        categoryName = categoryName,
                        entryType = LedgerEntryType.DEBIT,
                    ),
                ),
        )

    private class FakeTransactionHistoryRepository(
        private val result: LedgerResult<List<TransactionHistoryRecord>>,
    ) : TransactionHistoryRepository {
        override fun findAll(): LedgerResult<List<TransactionHistoryRecord>> = result
    }

    private companion object {
        const val JULY_19_2026_UTC = 1_784_419_200_000L
    }
}
