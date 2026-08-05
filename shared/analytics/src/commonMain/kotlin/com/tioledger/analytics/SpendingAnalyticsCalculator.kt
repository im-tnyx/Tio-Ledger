package com.tioledger.analytics

import com.tioledger.core.model.CurrencyCode
import com.tioledger.core.model.Money
import com.tioledger.domain.model.TransactionHistoryRecord
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

data class SpendingCashFlowBucket(
    val startInclusive: Long,
    val endExclusive: Long,
    val incomeTotal: Money,
    val expenseTotal: Money,
    val netTotal: Money,
)

data class SpendingCurrencySummary(
    val currency: CurrencyCode,
    val incomeTotal: Money,
    val expenseTotal: Money,
    val netTotal: Money,
    val cashFlowBuckets: List<SpendingCashFlowBucket>,
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
        val bucketWindows = bucketWindows(period, window, timeZoneId)
        val periodTransactions =
            transactions.filter { record ->
                record.timestamp >= window.startInclusive && record.timestamp < window.endExclusive
            }

        val incomeEntries =
            periodTransactions
                .asSequence()
                .filter { it.type == com.tioledger.domain.model.TransactionType.INCOME }
                .mapNotNull { record -> record.cashFlowEntry() }
                .toList()

        val expenseEntries =
            periodTransactions
                .asSequence()
                .filter { it.type == com.tioledger.domain.model.TransactionType.EXPENSE }
                .mapNotNull { record -> record.cashFlowEntry() }
                .toList()

        val categoryEntries =
            periodTransactions
                .asSequence()
                .filter { it.type == com.tioledger.domain.model.TransactionType.EXPENSE }
                .mapNotNull { it.categoryExpenseEntry() }
                .toList()

        val accountEntries =
            periodTransactions
                .asSequence()
                .filter { it.type == com.tioledger.domain.model.TransactionType.EXPENSE }
                .mapNotNull { it.accountExpenseEntry() }
                .toList()

        val currencies =
            buildSet {
                incomeEntries.forEach { add(it.amount.currency) }
                expenseEntries.forEach { add(it.amount.currency) }
            }.sortedBy(CurrencyCode::code)

        val currencySummaries =
            currencies.map { currency ->
                val currencyIncomeEntries = incomeEntries.filter { it.amount.currency == currency }
                val currencyExpenseEntries = expenseEntries.filter { it.amount.currency == currency }
                val incomeTotal =
                    currencyIncomeEntries
                        .asSequence()
                        .map(CashFlowEntry::amount)
                        .fold(Money.zero(currency)) { total, amount -> total + amount }
                val expenseTotal =
                    currencyExpenseEntries
                        .asSequence()
                        .map(CashFlowEntry::amount)
                        .fold(Money.zero(currency)) { total, amount -> total + amount }
                val netTotal = incomeTotal - expenseTotal
                SpendingCurrencySummary(
                    currency = currency,
                    incomeTotal = incomeTotal,
                    expenseTotal = expenseTotal,
                    netTotal = netTotal,
                    cashFlowBuckets =
                        bucketWindows.map { bucket ->
                            val bucketIncome = currencyIncomeEntries.totalWithin(bucket, currency)
                            val bucketExpense = currencyExpenseEntries.totalWithin(bucket, currency)
                            SpendingCashFlowBucket(
                                startInclusive = bucket.startInclusive,
                                endExclusive = bucket.endExclusive,
                                incomeTotal = bucketIncome,
                                expenseTotal = bucketExpense,
                                netTotal = bucketIncome - bucketExpense,
                            )
                        },
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

    private fun bucketWindows(
        period: SpendingAnalyticsPeriod,
        window: SpendingAnalyticsWindow,
        timeZoneId: String,
    ): List<AnalyticsBucketWindow> {
        val timeZone = TimeZone.of(timeZoneId)
        val startDate =
            Instant
                .fromEpochMilliseconds(window.startInclusive)
                .toLocalDateTime(timeZone)
                .date
        val endDate =
            Instant
                .fromEpochMilliseconds(window.endExclusive)
                .toLocalDateTime(timeZone)
                .date

        return when (period) {
            SpendingAnalyticsPeriod.WEEKLY,
            SpendingAnalyticsPeriod.MONTHLY,
            ->
                (startDate.toEpochDays() until endDate.toEpochDays()).map { epochDay ->
                    val bucketStart = LocalDate.fromEpochDays(epochDay)
                    val bucketEnd = LocalDate.fromEpochDays(epochDay + 1)
                    AnalyticsBucketWindow(
                        startInclusive = bucketStart.atStartOfDayIn(timeZone).toEpochMilliseconds(),
                        endExclusive = bucketEnd.atStartOfDayIn(timeZone).toEpochMilliseconds(),
                    )
                }
            SpendingAnalyticsPeriod.YEARLY ->
                (1..MONTHS_PER_YEAR).map { monthNumber ->
                    val bucketStart = LocalDate(startDate.year, monthNumber, 1)
                    val bucketEnd =
                        if (monthNumber == MONTHS_PER_YEAR) {
                            LocalDate(startDate.year + 1, 1, 1)
                        } else {
                            LocalDate(startDate.year, monthNumber + 1, 1)
                        }
                    AnalyticsBucketWindow(
                        startInclusive = bucketStart.atStartOfDayIn(timeZone).toEpochMilliseconds(),
                        endExclusive = bucketEnd.atStartOfDayIn(timeZone).toEpochMilliseconds(),
                    )
                }
        }
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

    private fun TransactionHistoryRecord.cashFlowEntry(): CashFlowEntry? {
        val split = primarySplit() ?: return null
        if (!split.amount.isPositive()) return null
        return CashFlowEntry(
            timestamp = timestamp,
            amount = split.amount,
        )
    }

    private fun TransactionHistoryRecord.primarySplit() = splits.firstOrNull()

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

    private fun List<CashFlowEntry>.totalWithin(
        bucket: AnalyticsBucketWindow,
        currency: CurrencyCode,
    ): Money =
        asSequence()
            .filter { entry ->
                entry.timestamp >= bucket.startInclusive && entry.timestamp < bucket.endExclusive
            }
            .map(CashFlowEntry::amount)
            .fold(Money.zero(currency)) { total, amount -> total + amount }

    private data class CashFlowEntry(
        val timestamp: Long,
        val amount: Money,
    )

    private data class AnalyticsBucketWindow(
        val startInclusive: Long,
        val endExclusive: Long,
    )

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
