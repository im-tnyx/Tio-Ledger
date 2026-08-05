package com.tioledger.ui.reports

import com.tioledger.application.usecase.analytics.SpendingReportPeriod

data class ReportsUiState(
    val isLoading: Boolean = true,
    val selectedPeriod: SpendingReportPeriod = SpendingReportPeriod.MONTHLY,
    val report: ReportsPeriodUiModel? = null,
    val loadErrorMessage: String? = null,
) {
    val isEmpty: Boolean
        get() = !isLoading && loadErrorMessage == null && report?.currencySections.orEmpty().isEmpty()
}

data class ReportsPeriodUiModel(
    val periodLabel: String,
    val dateRangeLabel: String,
    val currencySections: List<ReportsCurrencySectionUiModel>,
)

data class ReportsCurrencySectionUiModel(
    val currencyCode: String,
    val incomeLabel: String,
    val expenseLabel: String,
    val netLabel: String,
    val netMinorUnits: Long,
    val cashFlowRows: List<ReportsCashFlowRowUiModel>,
    val categoryBreakdown: List<ReportsBreakdownRowUiModel>,
    val accountBreakdown: List<ReportsBreakdownRowUiModel>,
)

data class ReportsCashFlowRowUiModel(
    val id: String,
    val label: String,
    val incomeLabel: String,
    val expenseLabel: String,
    val netLabel: String,
    val netMinorUnits: Long,
)

data class ReportsBreakdownRowUiModel(
    val id: String,
    val label: String,
    val amountLabel: String,
)

sealed interface ReportsAction {
    data object Load : ReportsAction

    data object Retry : ReportsAction

    data class PeriodSelected(val period: SpendingReportPeriod) : ReportsAction
}
