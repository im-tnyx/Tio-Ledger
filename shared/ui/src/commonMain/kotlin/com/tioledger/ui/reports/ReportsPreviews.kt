@file:Suppress("FunctionName", "UnusedPrivateMember")

package com.tioledger.ui.reports

import androidx.compose.runtime.Composable
import com.tioledger.application.usecase.analytics.SpendingReportPeriod
import com.tioledger.ui.design.TioLedgerTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview
@Composable
private fun ReportsLightPreview() {
    TioLedgerTheme(darkTheme = false) {
        ReportsScreen(
            state = ReportsPreviewData.populated,
            onAction = {},
            onNavigate = {},
        )
    }
}

@Preview
@Composable
private fun ReportsDarkPreview() {
    TioLedgerTheme(darkTheme = true) {
        ReportsScreen(
            state = ReportsPreviewData.populated,
            onAction = {},
            onNavigate = {},
        )
    }
}

private object ReportsPreviewData {
    val populated =
        ReportsUiState(
            isLoading = false,
            selectedPeriod = SpendingReportPeriod.MONTHLY,
            report =
                ReportsPeriodUiModel(
                    periodLabel = "Monthly",
                    dateRangeLabel = "2026-07-01 to 2026-07-31",
                    currencySections =
                        listOf(
                            ReportsCurrencySectionUiModel(
                                currencyCode = "INR",
                                incomeLabel = "INR 95000.00",
                                expenseLabel = "INR 14250.00",
                                netLabel = "+INR 80750.00",
                                netMinorUnits = 8_075_000L,
                                cashFlowRows =
                                    listOf(
                                        ReportsCashFlowRowUiModel(
                                            id = "2026-07-01",
                                            label = "2026-07-01",
                                            incomeLabel = "INR 95000.00",
                                            expenseLabel = "INR 0.00",
                                            netLabel = "+INR 95000.00",
                                            netMinorUnits = 9_500_000L,
                                        ),
                                        ReportsCashFlowRowUiModel(
                                            id = "2026-07-02",
                                            label = "2026-07-02",
                                            incomeLabel = "INR 0.00",
                                            expenseLabel = "INR 5250.00",
                                            netLabel = "-INR 5250.00",
                                            netMinorUnits = -525_000L,
                                        ),
                                        ReportsCashFlowRowUiModel(
                                            id = "2026-07-03",
                                            label = "2026-07-03",
                                            incomeLabel = "INR 0.00",
                                            expenseLabel = "INR 0.00",
                                            netLabel = "INR 0.00",
                                            netMinorUnits = 0L,
                                        ),
                                    ),
                                categoryBreakdown =
                                    listOf(
                                        ReportsBreakdownRowUiModel(
                                            id = "food",
                                            label = "Food",
                                            amountLabel = "INR 5250.00",
                                        ),
                                        ReportsBreakdownRowUiModel(
                                            id = "travel",
                                            label = "Travel",
                                            amountLabel = "INR 4500.00",
                                        ),
                                    ),
                                accountBreakdown =
                                    listOf(
                                        ReportsBreakdownRowUiModel(
                                            id = "wallet",
                                            label = "Wallet",
                                            amountLabel = "INR 6250.00",
                                        ),
                                        ReportsBreakdownRowUiModel(
                                            id = "bank",
                                            label = "Bank",
                                            amountLabel = "INR 8000.00",
                                        ),
                                    ),
                            ),
                        ),
                ),
        )
}
