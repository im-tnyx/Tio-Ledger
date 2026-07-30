package com.tioledger.analytics

import com.tioledger.core.model.CurrencyCode
import com.tioledger.core.model.Money
import com.tioledger.domain.model.AccountType
import com.tioledger.domain.model.LedgerEntryType
import com.tioledger.domain.model.TransactionHistoryRecord
import com.tioledger.domain.model.TransactionHistorySplit
import com.tioledger.domain.model.TransactionType
import kotlin.test.Test
import kotlin.test.assertEquals

class SpendingAnalyticsCalculatorTest {
    private val calculator = SpendingAnalyticsCalculator()

    @Test
    fun groupsIncomeExpenseCategoriesAndAccountsByCurrency() {
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

        val usd = result.currencySummaries.first { it.currency == CurrencyCode("USD") }
        assertEquals(Money.zero(CurrencyCode("USD")), usd.incomeTotal)
        assertEquals(Money(4_000L, CurrencyCode("USD")), usd.expenseTotal)
        assertEquals("Uncategorized", usd.categoryTotals.single().categoryName)
        assertEquals("Travel Card", usd.accountTotals.single().accountName)
    }

    @Test
    fun weeklyWindowStartsOnMondayAndEndsExclusively() {
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
        assertEquals(Money(1_000L, CurrencyCode("INR")), result.currencySummaries.single().expenseTotal)
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

    private companion object {
        const val JULY_13_2026_UTC = 1_783_900_800_000L
        const val JULY_19_2026_UTC = 1_784_419_200_000L
        const val JULY_20_2026_UTC = 1_784_505_600_000L
    }
}
