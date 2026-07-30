package com.tioledger.analytics

import com.tioledger.core.model.CurrencyCode
import com.tioledger.core.model.Money
import com.tioledger.domain.model.TransactionHistoryRecord
import com.tioledger.domain.model.TransactionHistorySplit
import com.tioledger.domain.model.TransactionType
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

data class SpendingAnalyticsWindow(
    val startInclusive: Long,
    val endExclusive: Long,
)

enum class SpendingAnalyticsPeriod {
    WEEKLY,
    MONTHLY,
    YEARLY,
}

data class SpendingCategoryTotal(
    val categoryId: String?,
    val categoryName: String,
    val amount: Money,
)

data class SpendingAccountTotal(
    val accountId: String,
    val accountName: String,
    val amount: Money,
)

data class SpendingCurrencySummary(
    val currency: CurrencyCode,
    val incomeTotal: Money,
    val expenseTotal: Money,
    val netTotal: Money,
    val categoryTotals: List<SpendingCategoryTotal>,
    val accountTotals: List<SpendingAccountTotal>,
)

data class SpendingAnalyticsSnapshot(
    val period: SpendingAnalyticsPeriod,
    val window: SpendingAnalyticsWindow,
    val currencySummaries: List<SpendingCurrencySummary>,
)

class SpendingAnalyticsCalculator {
    fun calculate(
        period: SpendingAnalyticsPeriod,
        anchorTimestamp: Long,
        timeZoneId: String,
        transactions: List<TransactionHistoryRecord>,
    ): SpendingAnalyticsSnapshot {
        require(anchorTimestamp >= 0L) { "anchorTimestamp must be zero or greater" }
        require(timeZoneId.isNotBlank()) { "timeZoneId must not be blank" }

        val window = currentWindow(period, anchorTimestamp, timeZoneId)
        val periodTransactions =
            transactions.filter { record ->
                record.timestamp >= window.startInclusive && record.timestamp < window.endExclusive
            }

        val incomeSplits =
            periodTransactions
                .asSequence()
                .filter { it.type == TransactionType.INCOME }
                .mapNotNull { it.primarySplit() }
                .filter { it.amount.isPositive() }
                .toList()

        val expenseSplits =
            periodTransactions
                .asSequence()
                .filter { it.type == TransactionType.EXPENSE }
                .mapNotNull { it.primarySplit() }
                .filter { it.amount.isPositive() }
                .toList()

        val categoryEntries =
            periodTransactions
                .asSequence()
                .filter { it.type == TransactionType.EXPENSE }
                .mapNotNull { it.categoryExpenseEntry() }
                .toList()

        val accountEntries =
            periodTransactions
                .asSequence()
                .filter { it.type == TransactionType.EXPENSE }
                .mapNotNull { it.accountExpenseEntry() }
                .toList()

        val currencies =
            buildSet {
                incomeSplits.forEach { add(it.amount.currency) }
                expenseSplits.forEach { add(it.amount.currency) }
            }.sortedBy(CurrencyCode::code)

        val currencySummaries =
            currencies.map { currency ->
                val incomeTotal =
                    incomeSplits
                        .asSequence()
                        .filter { it.amount.currency == currency }
                        .map(TransactionHistorySplit::amount)
                        .fold(Money.zero(currency)) { total, amount -> total + amount }
                val expenseTotal =
                    expenseSplits
                        .asSequence()
                        .filter { it.amount.currency == currency }
                        .map(TransactionHistorySplit::amount)
                        .fold(Money.zero(currency)) { total, amount -> total + amount }
                val netTotal = incomeTotal - expenseTotal
                SpendingCurrencySummary(
                    currency = currency,
                    incomeTotal = incomeTotal,
                    expenseTotal = expenseTotal,
                    netTotal = netTotal,
                    categoryTotals =
                        categoryEntries
                            .asSequence()
                            .filter { it.amount.currency == currency }
                            .groupBy { it.categoryId to it.categoryName }
                            .map { (key, entries) ->
                                SpendingCategoryTotal(
                                    categoryId = key.first,
                                    categoryName = key.second,
                                    amount =
                                        entries
                                            .map(CategoryExpenseEntry::amount)
                                            .fold(Money.zero(currency)) { total, amount -> total + amount },
                                )
                            }
                            .sortedWith(
                                compareByDescending<SpendingCategoryTotal> { it.amount.amount }
                                    .thenBy { it.categoryName.lowercase() },
                            ),
                    accountTotals =
                        accountEntries
                            .asSequence()
                            .filter { it.amount.currency == currency }
                            .groupBy { it.accountId to it.accountName }
                            .map { (key, entries) ->
                                SpendingAccountTotal(
                                    accountId = key.first,
                                    accountName = key.second,
                                    amount =
                                        entries
                                            .map(AccountExpenseEntry::amount)
                                            .fold(Money.zero(currency)) { total, amount -> total + amount },
                                )
                            }
                            .sortedWith(
                                compareByDescending<SpendingAccountTotal> { it.amount.amount }
                                    .thenBy { it.accountName.lowercase() },
                            ),
                )
            }

        return SpendingAnalyticsSnapshot(
            period = period,
            window = window,
            currencySummaries = currencySummaries,
        )
    }

    private fun currentWindow(
        period: SpendingAnalyticsPeriod,
        anchorTimestamp: Long,
        timeZoneId: String,
    ): SpendingAnalyticsWindow {
        val timeZone = TimeZone.of(timeZoneId)
        val anchorDate =
            Instant
                .fromEpochMilliseconds(anchorTimestamp)
                .toLocalDateTime(timeZone)
                .date
        val (startDate, endDate) = periodDates(period, anchorDate)
        return SpendingAnalyticsWindow(
            startInclusive = startDate.atStartOfDayIn(timeZone).toEpochMilliseconds(),
            endExclusive = endDate.atStartOfDayIn(timeZone).toEpochMilliseconds(),
        )
    }

    private fun periodDates(
        period: SpendingAnalyticsPeriod,
        anchorDate: LocalDate,
    ): Pair<LocalDate, LocalDate> =
        when (period) {
            SpendingAnalyticsPeriod.WEEKLY -> {
                val start = LocalDate.fromEpochDays(anchorDate.toEpochDays() - anchorDate.dayOfWeek.ordinal)
                start to LocalDate.fromEpochDays(start.toEpochDays() + DAYS_PER_WEEK)
            }
            SpendingAnalyticsPeriod.MONTHLY -> {
                val start = LocalDate(anchorDate.year, anchorDate.monthNumber, 1)
                val end =
                    if (anchorDate.monthNumber == MONTHS_PER_YEAR) {
                        LocalDate(anchorDate.year + 1, 1, 1)
                    } else {
                        LocalDate(anchorDate.year, anchorDate.monthNumber + 1, 1)
                    }
                start to end
            }
            SpendingAnalyticsPeriod.YEARLY -> LocalDate(anchorDate.year, 1, 1) to LocalDate(anchorDate.year + 1, 1, 1)
        }

    private fun TransactionHistoryRecord.primarySplit(): TransactionHistorySplit? = splits.firstOrNull()

    private fun TransactionHistoryRecord.categoryExpenseEntry(): CategoryExpenseEntry? {
        val split = splits.firstOrNull { it.categoryId != null } ?: primarySplit() ?: return null
        if (!split.amount.isPositive()) return null
        return CategoryExpenseEntry(
            categoryId = split.categoryId,
            categoryName = split.categoryName?.trim().takeUnless { it.isNullOrEmpty() } ?: UNCATEGORIZED_LABEL,
            amount = split.amount,
        )
    }

    private fun TransactionHistoryRecord.accountExpenseEntry(): AccountExpenseEntry? {
        val split = primarySplit() ?: return null
        if (!split.amount.isPositive()) return null
        return AccountExpenseEntry(
            accountId = split.accountId,
            accountName = split.accountName,
            amount = split.amount,
        )
    }

    private data class CategoryExpenseEntry(
        val categoryId: String?,
        val categoryName: String,
        val amount: Money,
    )

    private data class AccountExpenseEntry(
        val accountId: String,
        val accountName: String,
        val amount: Money,
    )

    private companion object {
        const val DAYS_PER_WEEK = 7
        const val MONTHS_PER_YEAR = 12
        const val UNCATEGORIZED_LABEL = "Uncategorized"
    }
}
