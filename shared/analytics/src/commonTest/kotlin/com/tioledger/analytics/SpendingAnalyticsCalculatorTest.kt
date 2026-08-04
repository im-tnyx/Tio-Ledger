package com.tioledger.analytics

import com.tioledger.core.model.CurrencyCode
import com.tioledger.core.model.Money
import com.tioledger.domain.model.AccountType
import com.tioledger.domain.model.LedgerEntryType
import com.tioledger.domain.model.TransactionHistoryRecord
import com.tioledger.domain.model.TransactionHistorySplit
import com.tioledger.domain.model.TransactionType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.test.Test
import kotlin.test.assertEquals

class SpendingAnalyticsCalculatorTest {
    private val calculator = SpendingAnalyticsCalculator()

    @Test
    fun groupsIncomeExpenseCategoriesAccountsAndCashFlowByCurrency() {
        val result =
            calculator.calculate(
                period = SpendingAnalyticsPeriod.MONTHLY,
                anchorTimestamp = JULY_19_2026_UTC,
                timeZoneId = "UTC",
                transactions =
                    listOf(
                        record(
                            id = "income-inr",
                            timestamp = JULY_19_2026_UTC,
                            type = TransactionType.INCOME,
                            split = split("bank-1", "Salary Bank", "INR", 125_000L, categoryId = "salary", categoryName = "Salary"),
                        ),
                        record(
                            id = "expense-food",
                            timestamp = JULY_19_2026_UTC,
                            type = TransactionType.EXPENSE,
                            split = split("wallet-1", "Wallet", "INR", 2_500L, categoryId = "food", categoryName = "Food"),
                        ),
                        record(
                            id = "expense-travel",
                            timestamp = JULY_19_2026_UTC,
                            type = TransactionType.EXPENSE,
                            split = split("wallet-1", "Wallet", "INR", 3_000L, categoryId = "travel", categoryName = "Travel"),
                        ),
                        record(
                            id = "expense-usd",
                            timestamp = JULY_19_2026_UTC,
                            type = TransactionType.EXPENSE,
                            split = split("card-1", "Travel Card", "USD", 4_000L, categoryId = null, categoryName = null),
                        ),
                        record(
                            id = "transfer",
                            timestamp = JULY_19_2026_UTC,
                            type = TransactionType.TRANSFER,
                            split = split("wallet-1", "Wallet", "INR", 999L),
                        ),
                        record(
                            id = "repayment",
                            timestamp = JULY_19_2026_UTC,
                            type = TransactionType.REPAYMENT,
                            split = split("loan-1", "Loan", "INR", 9_999L),
                        ),
                    ),
            )

        assertEquals(2, result.currencySummaries.size)

        val inr = result.currencySummaries.first { it.currency == CurrencyCode("INR") }
        assertEquals(Money(125_000L, CurrencyCode("INR")), inr.incomeTotal)
        assertEquals(Money(5_500L, CurrencyCode("INR")), inr.expenseTotal)
        assertEquals(Money(119_500L, CurrencyCode("INR")), inr.netTotal)
        assertEquals(listOf("Travel", "Food"), inr.categoryTotals.map { it.categoryName })
        assertEquals(listOf("Wallet"), inr.accountTotals.map { it.accountName })
        assertEquals(Money(5_500L, CurrencyCode("INR")), inr.accountTotals.single().amount)
        assertEquals(31, inr.cashFlowBuckets.size)
        val july19 = inr.cashFlowBuckets[18]
        assertEquals(Money(125_000L, CurrencyCode("INR")), july19.incomeTotal)
        assertEquals(Money(5_500L, CurrencyCode("INR")), july19.expenseTotal)
        assertEquals(Money(119_500L, CurrencyCode("INR")), july19.netTotal)

        val usd = result.currencySummaries.first { it.currency == CurrencyCode("USD") }
        assertEquals(Money.zero(CurrencyCode("USD")), usd.incomeTotal)
        assertEquals(Money(4_000L, CurrencyCode("USD")), usd.expenseTotal)
        assertEquals("Uncategorized", usd.categoryTotals.single().categoryName)
        assertEquals("Travel Card", usd.accountTotals.single().accountName)
        assertEquals(Money(-4_000L, CurrencyCode("USD")), usd.cashFlowBuckets[18].netTotal)
    }

    @Test
    fun weeklyWindowStartsOnMondayAndProducesSevenDailyBuckets() {
        val result =
            calculator.calculate(
                period = SpendingAnalyticsPeriod.WEEKLY,
                anchorTimestamp = JULY_19_2026_UTC,
                timeZoneId = "UTC",
                transactions =
                    listOf(
                        record(
                            id = "inside",
                            timestamp = JULY_13_2026_UTC,
                            type = TransactionType.EXPENSE,
                            split = split("wallet-1", "Wallet", "INR", 1_000L),
                        ),
                        record(
                            id = "outside",
                            timestamp = JULY_20_2026_UTC,
                            type = TransactionType.EXPENSE,
                            split = split("wallet-1", "Wallet", "INR", 2_000L),
                        ),
                    ),
            )

        assertEquals(JULY_13_2026_UTC, result.window.startInclusive)
        assertEquals(JULY_20_2026_UTC, result.window.endExclusive)
        val inr = result.currencySummaries.single()
        assertEquals(Money(1_000L, CurrencyCode("INR")), inr.expenseTotal)
        assertEquals(7, inr.cashFlowBuckets.size)
        assertEquals(Money(1_000L, CurrencyCode("INR")), inr.cashFlowBuckets.first().expenseTotal)
        assertEquals(Money.zero(CurrencyCode("INR")), inr.cashFlowBuckets.last().expenseTotal)
    }

    @Test
    fun monthlyBucketsUseRequestedTimeZoneAndIncludeZeroDays() {
        val timeZoneId = "Asia/Kolkata"
        val july1 = startOfDay(2026, 7, 1, timeZoneId)
        val july3 = startOfDay(2026, 7, 3, timeZoneId)
        val result =
            calculator.calculate(
                period = SpendingAnalyticsPeriod.MONTHLY,
                anchorTimestamp = startOfDay(2026, 7, 19, timeZoneId),
                timeZoneId = timeZoneId,
                transactions =
                    listOf(
                        record(
                            id = "income-first-day",
                            timestamp = july1,
                            type = TransactionType.INCOME,
                            split = split("bank", "Bank", "INR", 10_000L),
                        ),
                        record(
                            id = "expense-third-day",
                            timestamp = july3,
                            type = TransactionType.EXPENSE,
                            split = split("wallet", "Wallet", "INR", 2_500L),
                        ),
                        record(
                            id = "outside-before-window",
                            timestamp = july1 - 1L,
                            type = TransactionType.INCOME,
                            split = split("bank", "Bank", "INR", 99_000L),
                        ),
                    ),
            )

        val buckets = result.currencySummaries.single().cashFlowBuckets
        assertEquals(31, buckets.size)
        assertEquals(july1, buckets.first().startInclusive)
        assertEquals(Money(10_000L, CurrencyCode("INR")), buckets[0].incomeTotal)
        assertEquals(Money.zero(CurrencyCode("INR")), buckets[1].netTotal)
        assertEquals(Money(-2_500L, CurrencyCode("INR")), buckets[2].netTotal)
    }

    @Test
    fun yearlyBucketsUseCalendarMonthsAndIgnoreNonCashFlowTypes() {
        val result =
            calculator.calculate(
                period = SpendingAnalyticsPeriod.YEARLY,
                anchorTimestamp = startOfDay(2026, 7, 19),
                timeZoneId = "UTC",
                transactions =
                    listOf(
                        record(
                            id = "january-income",
                            timestamp = startOfDay(2026, 1, 10),
                            type = TransactionType.INCOME,
                            split = split("bank", "Bank", "INR", 50_000L),
                        ),
                        record(
                            id = "march-expense",
                            timestamp = startOfDay(2026, 3, 5),
                            type = TransactionType.EXPENSE,
                            split = split("wallet", "Wallet", "INR", 12_000L),
                        ),
                        record(
                            id = "march-transfer",
                            timestamp = startOfDay(2026, 3, 6),
                            type = TransactionType.TRANSFER,
                            split = split("wallet", "Wallet", "INR", 8_000L),
                        ),
                    ),
            )

        val buckets = result.currencySummaries.single().cashFlowBuckets
        assertEquals(12, buckets.size)
        assertEquals(Money(50_000L, CurrencyCode("INR")), buckets[0].incomeTotal)
        assertEquals(Money.zero(CurrencyCode("INR")), buckets[1].netTotal)
        assertEquals(Money(-12_000L, CurrencyCode("INR")), buckets[2].netTotal)
        assertEquals(startOfDay(2027, 1, 1), buckets.last().endExclusive)
    }

    private fun record(
        id: String,
        timestamp: Long,
        type: TransactionType,
        split: TransactionHistorySplit,
    ): TransactionHistoryRecord =
        TransactionHistoryRecord(
            id = id,
            timestamp = timestamp,
            description = null,
            type = type,
            splits = listOf(split),
        )

    private fun split(
        accountId: String,
        accountName: String,
        currency: String,
        amount: Long,
        categoryId: String? = null,
        categoryName: String? = null,
    ): TransactionHistorySplit =
        TransactionHistorySplit(
            id = "$accountId-$amount",
            accountId = accountId,
            accountName = accountName,
            accountType = AccountType.WALLET,
            amount = Money(amount, CurrencyCode(currency)),
            categoryId = categoryId,
            categoryName = categoryName,
            entryType = LedgerEntryType.CREDIT,
        )

    private fun startOfDay(
        year: Int,
        month: Int,
        day: Int,
        timeZoneId: String = "UTC",
    ): Long =
        LocalDate(year, month, day)
            .atStartOfDayIn(TimeZone.of(timeZoneId))
            .toEpochMilliseconds()

    private companion object {
        const val JULY_13_2026_UTC = 1_783_900_800_000L
        const val JULY_19_2026_UTC = 1_784_419_200_000L
        const val JULY_20_2026_UTC = 1_784_505_600_000L
    }
}
