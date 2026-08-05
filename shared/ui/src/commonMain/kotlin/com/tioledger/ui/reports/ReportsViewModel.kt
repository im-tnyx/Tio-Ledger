package com.tioledger.ui.reports

import com.tioledger.application.model.ApplicationError
import com.tioledger.application.model.ApplicationResult
import com.tioledger.application.usecase.analytics.GetSpendingAnalyticsUseCase
import com.tioledger.application.usecase.analytics.SpendingAnalyticsReport
import com.tioledger.application.usecase.analytics.SpendingCashFlowBucket
import com.tioledger.application.usecase.analytics.SpendingCurrencyReport
import com.tioledger.application.usecase.analytics.SpendingReportPeriod
import com.tioledger.core.model.Money
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs

class ReportsViewModel(
    private val getSpendingAnalyticsUseCase: GetSpendingAnalyticsUseCase,
    private val nowProvider: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val timeZoneIdProvider: () -> String = { TimeZone.currentSystemDefault().id },
) {
    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init {
        onAction(ReportsAction.Load)
    }

    fun onAction(action: ReportsAction) {
        when (action) {
            ReportsAction.Load, ReportsAction.Retry -> loadReports(_uiState.value.selectedPeriod)
            is ReportsAction.PeriodSelected -> {
                _uiState.update {
                    it.copy(
                        selectedPeriod = action.period,
                        loadErrorMessage = null,
                    )
                }
                loadReports(action.period)
            }
        }
    }

    private fun loadReports(period: SpendingReportPeriod) {
        _uiState.update {
            it.copy(
                isLoading = true,
                loadErrorMessage = null,
            )
        }

        val timeZoneId = timeZoneIdProvider()
        when (val result = getSpendingAnalyticsUseCase(period, nowProvider(), timeZoneId)) {
            is ApplicationResult.Success -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        report = result.outcome.value.toUiModel(timeZoneId),
                        loadErrorMessage = null,
                    )
                }
            }
            is ApplicationResult.Failure -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        report = null,
                        loadErrorMessage = result.error.toMessage(),
                    )
                }
            }
        }
    }
}

private fun SpendingAnalyticsReport.toUiModel(timeZoneId: String): ReportsPeriodUiModel =
    ReportsPeriodUiModel(
        periodLabel = period.toLabel(),
        dateRangeLabel = formatDateRange(startInclusive, endExclusive, timeZoneId),
        currencySections = currencyReports.map { report -> report.toUiModel(period, timeZoneId) },
    )

private fun SpendingCurrencyReport.toUiModel(
    period: SpendingReportPeriod,
    timeZoneId: String,
): ReportsCurrencySectionUiModel =
    ReportsCurrencySectionUiModel(
        currencyCode = currencyCode,
        incomeLabel = income.toDisplayAmount(),
        expenseLabel = expense.toDisplayAmount(),
        netLabel = net.toDisplayAmount(showPlusForPositive = true),
        netMinorUnits = net.amount,
        cashFlowRows = cashFlowBuckets.map { bucket -> bucket.toUiModel(period, timeZoneId) },
        categoryBreakdown =
            categoryBreakdown.map { category ->
                ReportsBreakdownRowUiModel(
                    id = category.categoryId ?: "uncategorized-${category.categoryName}",
                    label = category.categoryName,
                    amountLabel = category.amount.toDisplayAmount(),
                )
            },
        accountBreakdown =
            accountBreakdown.map { account ->
                ReportsBreakdownRowUiModel(
                    id = account.accountId,
                    label = account.accountName,
                    amountLabel = account.amount.toDisplayAmount(),
                )
            },
    )

private fun SpendingCashFlowBucket.toUiModel(
    period: SpendingReportPeriod,
    timeZoneId: String,
): ReportsCashFlowRowUiModel =
    ReportsCashFlowRowUiModel(
        id = "$startInclusive-$endExclusive",
        label = formatCashFlowBucketLabel(startInclusive, period, timeZoneId),
        incomeLabel = income.toDisplayAmount(),
        expenseLabel = expense.toDisplayAmount(),
        netLabel = net.toDisplayAmount(showPlusForPositive = true),
        netMinorUnits = net.amount,
    )

private fun SpendingReportPeriod.toLabel(): String =
    when (this) {
        SpendingReportPeriod.WEEKLY -> "Weekly"
        SpendingReportPeriod.MONTHLY -> "Monthly"
        SpendingReportPeriod.YEARLY -> "Yearly"
    }

private fun formatDateRange(
    startInclusive: Long,
    endExclusive: Long,
    timeZoneId: String,
): String {
    val timeZone = TimeZone.of(timeZoneId)
    val startDate = Instant.fromEpochMilliseconds(startInclusive).toLocalDateTime(timeZone).date
    val endDate = Instant.fromEpochMilliseconds(endExclusive - 1L).toLocalDateTime(timeZone).date
    return "$startDate to $endDate"
}

private fun formatCashFlowBucketLabel(
    startInclusive: Long,
    period: SpendingReportPeriod,
    timeZoneId: String,
): String {
    val startDate =
        Instant
            .fromEpochMilliseconds(startInclusive)
            .toLocalDateTime(TimeZone.of(timeZoneId))
            .date
    return when (period) {
        SpendingReportPeriod.WEEKLY,
        SpendingReportPeriod.MONTHLY,
        -> startDate.toString()
        SpendingReportPeriod.YEARLY ->
            "${startDate.year}-${startDate.monthNumber.toString().padStart(2, '0')}"
    }
}

private fun Money.toDisplayAmount(showPlusForPositive: Boolean = false): String {
    val absolute = abs(amount)
    val major = absolute / MINOR_UNITS
    val minor = absolute % MINOR_UNITS
    val prefix =
        when {
            amount < 0L -> "-"
            showPlusForPositive && amount > 0L -> "+"
            else -> ""
        }
    return "$prefix$currency $major.${minor.toString().padStart(2, '0')}"
}

private fun ApplicationError.toMessage(): String =
    when (this) {
        is ApplicationError.Validation -> "$field: $reason"
        is ApplicationError.Repository -> "Unable to load reports."
        is ApplicationError.Ledger -> "Unable to prepare spending analytics."
    }

private const val MINOR_UNITS = 100L
