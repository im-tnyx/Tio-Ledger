package com.tioledger.ui.categories

import com.tioledger.domain.model.CategoryType

data class CategoriesUiState(
    val isLoading: Boolean = true,
    val groups: List<CategoryGroupUiModel> = emptyList(),
    val loadErrorMessage: String? = null,
    val isCreateDialogVisible: Boolean = false,
    val draftName: String = "",
    val draftType: CategoryType = CategoryType.EXPENSE,
    val isSaving: Boolean = false,
    val validationErrorMessage: String? = null,
    val persistenceErrorMessage: String? = null,
    val successMessage: String? = null,
) {
    val isEmpty: Boolean
        get() = !isLoading && loadErrorMessage == null && groups.all { it.categories.isEmpty() }
}

data class CategoryGroupUiModel(
    val type: CategoryType,
    val title: String,
    val categories: List<CategoryRowUiModel>,
)

data class CategoryRowUiModel(
    val id: String,
    val name: String,
    val isDefault: Boolean,
)

sealed interface CategoriesAction {
    data object Load : CategoriesAction

    data object Retry : CategoriesAction

    data object AddClicked : CategoriesAction

    data object CreateDismissed : CategoriesAction

    data class NameChanged(val name: String) : CategoriesAction

    data class TypeChanged(val type: CategoryType) : CategoriesAction

    data object SaveClicked : CategoriesAction

    data object MessageDismissed : CategoriesAction
}
