package com.tioledger.application.usecase.analytics

import com.tioledger.analytics.SpendingAnalyticsCalculator
import com.tioledger.analytics.SpendingAnalyticsPeriod
import com.tioledger.application.internal.validateTimestamp
import com.tioledger.application.model.ApplicationError
import com.tioledger.application.model.ApplicationResult
import com.tioledger.application.model.UseCaseOutcome
import com.tioledger.core.model.LedgerError
import com.tioledger.core.model.LedgerResult
import com.tioledger.core.model.Money
import com.tioledger.domain.repository.TransactionHistoryRepository

enum class SpendingReportPeriod {
    WEEKLY,
    MONTHLY,
    YEARLY,
}

data class SpendingCategoryBreakdown(
    val categoryId: String?,
    val categoryName: String,
    val amount: Money,
)

data class SpendingAccountBreakdown(
    val accountId: String,
    val accountName: String,
    val amount: Money,
)

data class SpendingCurrencyReport(
    val currencyCode: String,
    val income: Money,
    val expense: Money,
    val net: Money,
    val categoryBreakdown: List<SpendingCategoryBreakdown>,
    val accountBreakdown: List<SpendingAccountBreakdown>,
)

data class SpendingAnalyticsReport(
    val period: SpendingReportPeriod,
    val startInclusive: Long,
    val endExclusive: Long,
    val currencyReports: List<SpendingCurrencyReport>,
)

class GetSpendingAnalyticsUseCase(
    private val transactionHistoryRepository: TransactionHistoryRepository,
    private val calculator: SpendingAnalyticsCalculator,
) {
    operator fun invoke(
        period: SpendingReportPeriod,
        anchorTimestamp: Long,
        timeZoneId: String,
    ): ApplicationResult<SpendingAnalyticsReport> {
        validateTimestamp(anchorTimestamp, "anchorTimestamp")?.let {
            return ApplicationResult.Failure(it)
        }
        if (timeZoneId.isBlank()) {
            return ApplicationResult.Failure(
                ApplicationError.Validation("timeZoneId", "must not be blank"),
            )
        }

        val transactions =
            when (val result = transactionHistoryRepository.findAll()) {
                is LedgerResult.Success -> result.value
                is LedgerResult.Failure -> return ApplicationResult.Failure(ApplicationError.Repository(result.error))
            }

        return try {
            val snapshot = calculator.calculate(period.toAnalyticsPeriod(), anchorTimestamp, timeZoneId, transactions)
            ApplicationResult.Success(
                UseCaseOutcome(
                    value =
                        SpendingAnalyticsReport(
                            period = period,
                            startInclusive = snapshot.window.startInclusive,
                            endExclusive = snapshot.window.endExclusive,
                            currencyReports =
                                snapshot.currencySummaries.map { summary ->
                                    SpendingCurrencyReport(
                                        currencyCode = summary.currency.code,
                                        income = summary.incomeTotal,
                                        expense = summary.expenseTotal,
                                        net = summary.netTotal,
                                        categoryBreakdown =
                                            summary.categoryTotals.map { category ->
                                                SpendingCategoryBreakdown(
                                                    categoryId = category.categoryId,
                                                    categoryName = category.categoryName,
                                                    amount = category.amount,
                                                )
                                            },
                                        accountBreakdown =
                                            summary.accountTotals.map { account ->
                                                SpendingAccountBreakdown(
                                                    accountId = account.accountId,
                                                    accountName = account.accountName,
                                                    amount = account.amount,
                                                )
                                            },
                                    )
                                },
                        ),
                ),
            )
        } catch (error: IllegalArgumentException) {
            ApplicationResult.Failure(
                ApplicationError.Validation(
                    field = "timeZoneId",
                    reason = error.message ?: "invalid time zone",
                ),
            )
        } catch (error: ArithmeticException) {
            ApplicationResult.Failure(
                ApplicationError.Ledger(
                    LedgerError.Unknown(error.message ?: "spending analytics overflow"),
                ),
            )
        }
    }
}

private fun SpendingReportPeriod.toAnalyticsPeriod(): SpendingAnalyticsPeriod =
    when (this) {
        SpendingReportPeriod.WEEKLY -> SpendingAnalyticsPeriod.WEEKLY
        SpendingReportPeriod.MONTHLY -> SpendingAnalyticsPeriod.MONTHLY
        SpendingReportPeriod.YEARLY -> SpendingAnalyticsPeriod.YEARLY
    }
