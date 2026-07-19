package com.tioledger.ui.budgets

import com.tioledger.budget.engine.BudgetProgressStatus
import com.tioledger.domain.model.BudgetPeriodType

data class BudgetsUiState(
    val isLoading: Boolean = true,
    val budgets: List<BudgetRowUiModel> = emptyList(),
    val categoryOptions: List<BudgetCategoryOption> = emptyList(),
    val loadErrorMessage: String? = null,
    val editor: BudgetEditorUiState? = null,
    val isCategoryPickerVisible: Boolean = false,
    val isSaving: Boolean = false,
    val validationErrorMessage: String? = null,
    val persistenceErrorMessage: String? = null,
    val successMessage: String? = null,
) {
    val isEmpty: Boolean
        get() = !isLoading && loadErrorMessage == null && budgets.isEmpty()
}

data class BudgetRowUiModel(
    val id: String,
    val name: String,
    val categoryId: String?,
    val categoryLabel: String,
    val periodType: BudgetPeriodType,
    val periodLabel: String,
    val periodDateRange: String,
    val targetMinorUnits: Long,
    val currencyCode: String,
    val targetLabel: String,
    val spentLabel: String,
    val remainingLabel: String,
    val utilizationPermille: Int,
    val status: BudgetProgressStatus,
    val statusLabel: String,
)

data class BudgetCategoryOption(
    val id: String?,
    val name: String,
)

data class BudgetEditorUiState(
    val budgetId: String? = null,
    val name: String = "",
    val amount: String = "",
    val currencyCode: String = "",
    val categoryId: String? = null,
    val periodType: BudgetPeriodType = BudgetPeriodType.MONTHLY,
) {
    val isEditing: Boolean
        get() = budgetId != null
}

sealed interface BudgetsAction {
    data object Load : BudgetsAction

    data object Retry : BudgetsAction

    data object AddClicked : BudgetsAction

    data class EditClicked(val budgetId: String) : BudgetsAction

    data object EditorDismissed : BudgetsAction

    data class NameChanged(val name: String) : BudgetsAction

    data class AmountChanged(val amount: String) : BudgetsAction

    data class CurrencyChanged(val currencyCode: String) : BudgetsAction

    data class PeriodChanged(val periodType: BudgetPeriodType) : BudgetsAction

    data object CategoryClicked : BudgetsAction

    data class CategorySelected(val categoryId: String?) : BudgetsAction

    data object CategoryPickerDismissed : BudgetsAction

    data object SaveClicked : BudgetsAction

    data object MessageDismissed : BudgetsAction
}
