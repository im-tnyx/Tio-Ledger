@file:Suppress("FunctionName", "UnusedPrivateMember")

package com.tioledger.ui.categories

import androidx.compose.runtime.Composable
import com.tioledger.domain.model.CategoryType
import com.tioledger.ui.design.TioLedgerTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview
@Composable
private fun CategoriesLightPreview() {
    TioLedgerTheme(darkTheme = false) {
        CategoriesScreen(
            state = CategoriesPreviewData.populated,
            onAction = {},
            onNavigate = {},
        )
    }
}

@Preview
@Composable
private fun CategoriesDarkPreview() {
    TioLedgerTheme(darkTheme = true) {
        CategoriesScreen(
            state = CategoriesPreviewData.populated,
            onAction = {},
            onNavigate = {},
        )
    }
}

@Preview
@Composable
private fun CreateCategoryDialogPreview() {
    TioLedgerTheme(darkTheme = false) {
        CategoriesScreen(
            state =
                CategoriesPreviewData.populated.copy(
                    isCreateDialogVisible = true,
                    draftName = "Travel",
                    draftType = CategoryType.EXPENSE,
                ),
            onAction = {},
            onNavigate = {},
        )
    }
}

private object CategoriesPreviewData {
    val populated =
        CategoriesUiState(
            isLoading = false,
            groups =
                listOf(
                    CategoryGroupUiModel(
                        type = CategoryType.EXPENSE,
                        title = "Expense",
                        categories =
                            listOf(
                                CategoryRowUiModel("food", "Food", true),
                                CategoryRowUiModel("transport", "Transport", false),
                                CategoryRowUiModel("utilities", "Utilities", false),
                            ),
                    ),
                    CategoryGroupUiModel(
                        type = CategoryType.INCOME,
                        title = "Income",
                        categories =
                            listOf(
                                CategoryRowUiModel("salary", "Salary", true),
                                CategoryRowUiModel("interest", "Interest", false),
                            ),
                    ),
                ),
        )
}
