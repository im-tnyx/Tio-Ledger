@file:Suppress("FunctionName", "UnusedPrivateMember")

package com.tioledger.ui.budgets

import androidx.compose.runtime.Composable
import com.tioledger.budget.engine.BudgetProgressStatus
import com.tioledger.domain.model.BudgetPeriodType
import com.tioledger.ui.design.TioLedgerTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview
@Composable
private fun BudgetsLightPreview() {
    TioLedgerTheme(darkTheme = false) {
        BudgetsScreen(
            state = BudgetsPreviewData.populated,
            onAction = {},
            onNavigate = {},
        )
    }
}

@Preview
@Composable
private fun BudgetsDarkPreview() {
    TioLedgerTheme(darkTheme = true) {
        BudgetsScreen(
            state = BudgetsPreviewData.populated,
            onAction = {},
            onNavigate = {},
        )
    }
}

@Preview
@Composable
private fun BudgetEditorPreview() {
    TioLedgerTheme(darkTheme = false) {
        BudgetsScreen(
            state =
                BudgetsPreviewData.populated.copy(
                    editor =
                        BudgetEditorUiState(
                            name = "Travel",
                            amount = "5000",
                            currencyCode = "INR",
                            categoryId = "travel",
                            periodType = BudgetPeriodType.MONTHLY,
                        ),
                ),
            onAction = {},
            onNavigate = {},
        )
    }
}

private object BudgetsPreviewData {
    val populated =
        BudgetsUiState(
            isLoading = false,
            budgets =
                listOf(
                    BudgetRowUiModel(
                        id = "food",
                        name = "Food",
                        categoryId = "food-category",
                        categoryLabel = "Food",
                        periodType = BudgetPeriodType.MONTHLY,
                        periodLabel = "Monthly",
                        periodDateRange = "2026-07-01 – 2026-07-31",
                        targetMinorUnits = 20_000L,
                        currencyCode = "INR",
                        targetLabel = "INR 200.00",
                        spentLabel = "INR 145.00",
                        remainingLabel = "INR 55.00",
                        utilizationPermille = 725,
                        status = BudgetProgressStatus.ON_TRACK,
                        statusLabel = "On track",
                    ),
                    BudgetRowUiModel(
                        id = "transport",
                        name = "Transport",
                        categoryId = "transport-category",
                        categoryLabel = "Transport",
                        periodType = BudgetPeriodType.WEEKLY,
                        periodLabel = "Weekly",
                        periodDateRange = "2026-07-13 – 2026-07-19",
                        targetMinorUnits = 8_000L,
                        currencyCode = "INR",
                        targetLabel = "INR 80.00",
                        spentLabel = "INR 86.50",
                        remainingLabel = "-INR 6.50",
                        utilizationPermille = 1_081,
                        status = BudgetProgressStatus.EXCEEDED,
                        statusLabel = "Over budget",
                    ),
                ),
            categoryOptions =
                listOf(
                    BudgetCategoryOption(null, "All expenses"),
                    BudgetCategoryOption("food-category", "Food"),
                    BudgetCategoryOption("transport-category", "Transport"),
                    BudgetCategoryOption("travel", "Travel"),
                ),
        )
}
