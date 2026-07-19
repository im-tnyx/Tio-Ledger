package com.tioledger.budget.engine

import com.tioledger.core.model.CurrencyCode
import com.tioledger.core.model.Money
import com.tioledger.domain.model.AccountType
import com.tioledger.domain.model.Budget
import com.tioledger.domain.model.BudgetPeriodType
import com.tioledger.domain.model.LedgerEntryType
import com.tioledger.domain.model.TransactionHistoryRecord
import com.tioledger.domain.model.TransactionHistorySplit
import com.tioledger.domain.model.TransactionType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.test.Test
import kotlin.test.assertEquals

class BudgetCalculatorsTest {
    private val usd = CurrencyCode("USD")
    private val periodCalculator = BudgetPeriodCalculator()
    private val progressCalculator = BudgetProgressCalculator()

    @Test
    fun monthlyPeriodUsesCalendarBoundaries() {
        val period =
            periodCalculator.currentPeriod(
                periodType = BudgetPeriodType.MONTHLY,
                anchorTimestamp = dateMillis(2026, 7, 19),
                timeZoneId = "UTC",
            )

        assertEquals(dateMillis(2026, 7, 1), period.startInclusive)
        assertEquals(dateMillis(2026, 8, 1), period.endExclusive)
    }

    @Test
    fun weeklyPeriodStartsOnMonday() {
        val period =
            periodCalculator.currentPeriod(
                periodType = BudgetPeriodType.WEEKLY,
                anchorTimestamp = dateMillis(2026, 7, 19),
                timeZoneId = "UTC",
            )

        assertEquals(dateMillis(2026, 7, 13), period.startInclusive)
        assertEquals(dateMillis(2026, 7, 20), period.endExclusive)
    }

    @Test
    fun progressAggregatesOnlyMatchingExpenseSplitsInPeriodAndCurrency() {
        val budget = budget(amount = 10_000L, categoryId = "food")
        val period = BudgetPeriodWindow(dateMillis(2026, 7, 1), dateMillis(2026, 8, 1))
        val transactions =
            listOf(
                transaction("matching", dateMillis(2026, 7, 5), TransactionType.EXPENSE, "food", 2_500L, usd),
                transaction("other-category", dateMillis(2026, 7, 6), TransactionType.EXPENSE, "travel", 3_000L, usd),
                transaction("income", dateMillis(2026, 7, 7), TransactionType.INCOME, "food", 4_000L, usd),
                transaction("outside", dateMillis(2026, 8, 1), TransactionType.EXPENSE, "food", 5_000L, usd),
                transaction("other-currency", dateMillis(2026, 7, 8), TransactionType.EXPENSE, "food", 6_000L, CurrencyCode("INR")),
            )

        val progress = progressCalculator.calculate(budget, period, transactions)

        assertEquals(Money(2_500L, usd), progress.spent)
        assertEquals(Money(7_500L, usd), progress.remaining)
        assertEquals(250, progress.utilizationPermille)
        assertEquals(BudgetProgressStatus.ON_TRACK, progress.status)
    }

    @Test
    fun progressMarksWarningReachedAndExceededWithoutFloatingPointMath() {
        val period = BudgetPeriodWindow(dateMillis(2026, 7, 1), dateMillis(2026, 8, 1))

        val warning =
            progressCalculator.calculate(
                budget = budget(amount = 10_000L),
                period = period,
                transactions = listOf(transaction("warning", dateMillis(2026, 7, 5), TransactionType.EXPENSE, "food", 8_000L, usd)),
            )
        val reached =
            progressCalculator.calculate(
                budget = budget(amount = 10_000L),
                period = period,
                transactions = listOf(transaction("reached", dateMillis(2026, 7, 5), TransactionType.EXPENSE, "food", 10_000L, usd)),
            )
        val exceeded =
            progressCalculator.calculate(
                budget = budget(amount = 10_000L),
                period = period,
                transactions = listOf(transaction("exceeded", dateMillis(2026, 7, 5), TransactionType.EXPENSE, "food", 12_500L, usd)),
            )

        assertEquals(800, warning.utilizationPermille)
        assertEquals(BudgetProgressStatus.WARNING, warning.status)
        assertEquals(1_000, reached.utilizationPermille)
        assertEquals(BudgetProgressStatus.REACHED, reached.status)
        assertEquals(1_250, exceeded.utilizationPermille)
        assertEquals(BudgetProgressStatus.EXCEEDED, exceeded.status)
        assertEquals(Money(-2_500L, usd), exceeded.remaining)
    }

    private fun budget(
        amount: Long,
        categoryId: String? = null,
    ): Budget =
        Budget(
            id = "budget",
            name = "Monthly budget",
            amount = Money(amount, usd),
            categoryId = categoryId,
            periodType = BudgetPeriodType.MONTHLY,
            createdAt = 1L,
            updatedAt = 1L,
        )

    private fun transaction(
        id: String,
        timestamp: Long,
        type: TransactionType,
        categoryId: String?,
        amount: Long,
        currency: CurrencyCode,
    ): TransactionHistoryRecord =
        TransactionHistoryRecord(
            id = id,
            timestamp = timestamp,
            description = id,
            type = type,
            splits =
                listOf(
                    TransactionHistorySplit(
                        id = "$id-split",
                        accountId = "account",
                        accountName = "Account",
                        accountType = AccountType.BANK,
                        amount = Money(amount, currency),
                        categoryId = categoryId,
                        categoryName = categoryId,
                        entryType = LedgerEntryType.CREDIT,
                    ),
                ),
        )

    private fun dateMillis(
        year: Int,
        month: Int,
        day: Int,
    ): Long = LocalDate(year, month, day).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
}
