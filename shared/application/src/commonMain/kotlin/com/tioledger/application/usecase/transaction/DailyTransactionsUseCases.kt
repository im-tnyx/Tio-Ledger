package com.tioledger.application.usecase.transaction

import com.tioledger.analytics.SpendingAnalyticsCalculator
import com.tioledger.analytics.SpendingAnalyticsPeriod
import com.tioledger.application.model.ApplicationError
import com.tioledger.application.model.ApplicationResult
import com.tioledger.application.model.UseCaseOutcome
import com.tioledger.core.model.LedgerError
import com.tioledger.core.model.LedgerResult
import com.tioledger.core.model.Money
import com.tioledger.domain.model.TransactionHistoryRecord
import com.tioledger.domain.repository.TransactionHistoryRepository
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

data class DailyTransactionsQuery(
    val month: LocalDate,
    val timeZoneId: String,
)

data class DailyCurrencySection(
    val currencyCode: String,
    val income: Money,
    val expense: Money,
    val net: Money,
)

data class DailyDayGroup(
    val date: LocalDate,
    val transactions: List<TransactionSummary>,
)

data class DailyTransactionsSnapshot(
    val month: LocalDate,
    val timeZoneId: String,
    val startInclusive: Long,
    val endExclusive: Long,
    val currencySections: List<DailyCurrencySection>,
    val dayGroups: List<DailyDayGroup>,
)

class GetDailyTransactionsUseCase(
    private val transactionHistoryRepository: TransactionHistoryRepository,
    private val calculator: SpendingAnalyticsCalculator,
) {
    operator fun invoke(query: DailyTransactionsQuery): ApplicationResult<DailyTransactionsSnapshot> {
        if (query.month.dayOfMonth != 1) {
            return ApplicationResult.Failure(
                ApplicationError.Validation("month", "must be the first day of its calendar month"),
            )
        }
        if (query.timeZoneId.isBlank()) {
            return ApplicationResult.Failure(
                ApplicationError.Validation("timeZoneId", "must not be blank"),
            )
        }

        val timeZone =
            try {
                TimeZone.of(query.timeZoneId)
            } catch (error: IllegalArgumentException) {
                return ApplicationResult.Failure(
                    ApplicationError.Validation(
                        field = "timeZoneId",
                        reason = error.message ?: "invalid time zone",
                    ),
                )
            }
        val endMonth = nextMonth(query.month)
        val startInclusive = query.month.atStartOfDayIn(timeZone).toEpochMilliseconds()
        val endExclusive = endMonth.atStartOfDayIn(timeZone).toEpochMilliseconds()
        if (startInclusive < 0L) {
            return ApplicationResult.Failure(
                ApplicationError.Validation(
                    field = "month",
                    reason = "pre-epoch months are not supported by the existing analytics calculator",
                ),
            )
        }

        val transactions =
            when (val result = transactionHistoryRepository.findAll()) {
                is LedgerResult.Success -> result.value
                is LedgerResult.Failure -> return ApplicationResult.Failure(ApplicationError.Repository(result.error))
            }
        val monthTransactions =
            transactions.filter { record ->
                record.timestamp >= startInclusive && record.timestamp < endExclusive
            }

        return try {
            val analyticsSnapshot =
                calculator.calculate(
                    period = SpendingAnalyticsPeriod.MONTHLY,
                    anchorTimestamp = startInclusive,
                    timeZoneId = query.timeZoneId,
                    transactions = monthTransactions,
                )
            val summaries = monthTransactions.mapNotNull(TransactionHistoryRecord::toSummary).sortedForTransactionHistory()
            ApplicationResult.Success(
                UseCaseOutcome(
                    value =
                        DailyTransactionsSnapshot(
                            month = query.month,
                            timeZoneId = query.timeZoneId,
                            startInclusive = startInclusive,
                            endExclusive = endExclusive,
                            currencySections =
                                buildCurrencySections(summaries, analyticsSnapshot.currencySummaries),
                            dayGroups =
                                summaries
                                    .groupBy { summary ->
                                        Instant
                                            .fromEpochMilliseconds(summary.timestamp)
                                            .toLocalDateTime(timeZone)
                                            .date
                                    }
                                    .toList()
                                    .sortedByDescending { (date, _) -> date }
                                    .map { (date, dayTransactions) ->
                                        DailyDayGroup(date = date, transactions = dayTransactions)
                                    },
                        ),
                ),
            )
        } catch (error: ArithmeticException) {
            ApplicationResult.Failure(
                ApplicationError.Ledger(
                    LedgerError.Unknown(error.message ?: "daily transactions calculation overflow"),
                ),
            )
        }
    }

    private fun buildCurrencySections(
        summaries: List<TransactionSummary>,
        analyticsSummaries: List<com.tioledger.analytics.SpendingCurrencySummary>,
    ): List<DailyCurrencySection> {
        val currencies = summaries.map { it.amount.currency }.toSet().sortedBy { it.toString() }
        return currencies.map { currency ->
            val analyticsSummary = analyticsSummaries.firstOrNull { it.currency == currency }
            DailyCurrencySection(
                currencyCode = currency.toString(),
                income = analyticsSummary?.incomeTotal ?: Money.zero(currency),
                expense = analyticsSummary?.expenseTotal ?: Money.zero(currency),
                net = analyticsSummary?.netTotal ?: Money.zero(currency),
            )
        }
    }
}

private fun nextMonth(month: LocalDate): LocalDate =
    if (month.monthNumber == 12) {
        LocalDate(month.year + 1, 1, 1)
    } else {
        LocalDate(month.year, month.monthNumber + 1, 1)
    }
