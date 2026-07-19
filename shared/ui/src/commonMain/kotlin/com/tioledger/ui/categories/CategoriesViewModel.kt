package com.tioledger.ui.categories

import com.tioledger.application.model.ApplicationError
import com.tioledger.application.model.ApplicationResult
import com.tioledger.application.usecase.category.CreateCategoryCommand
import com.tioledger.application.usecase.category.CreateCategoryUseCase
import com.tioledger.application.usecase.category.ListCategoriesUseCase
import com.tioledger.core.util.IdGenerator
import com.tioledger.domain.model.Category
import com.tioledger.domain.model.CategoryType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock

class CategoriesViewModel(
    private val listCategoriesUseCase: ListCategoriesUseCase,
    private val createCategoryUseCase: CreateCategoryUseCase,
    private val idGenerator: IdGenerator,
    private val nowProvider: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    private val _uiState = MutableStateFlow(CategoriesUiState())
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    init {
        onAction(CategoriesAction.Load)
    }

    fun onAction(action: CategoriesAction) {
        when (action) {
            CategoriesAction.Load, CategoriesAction.Retry -> loadCategories()
            CategoriesAction.AddClicked -> openCreateDialog()
            CategoriesAction.CreateDismissed -> closeCreateDialog()
            is CategoriesAction.NameChanged -> updateDraftName(action.name)
            is CategoriesAction.TypeChanged -> updateDraftType(action.type)
            CategoriesAction.SaveClicked -> createCategory()
            CategoriesAction.MessageDismissed -> dismissMessage()
        }
    }

    private fun loadCategories() {
        _uiState.update {
            it.copy(
                isLoading = true,
                loadErrorMessage = null,
            )
        }
        when (val result = listCategoriesUseCase()) {
            is ApplicationResult.Success -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        groups = result.outcome.value.toGroups(),
                        loadErrorMessage = null,
                    )
                }
            }
            is ApplicationResult.Failure -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        groups = emptyList(),
                        loadErrorMessage = result.error.toLoadMessage(),
                    )
                }
            }
        }
    }

    private fun openCreateDialog() {
        _uiState.update {
            it.copy(
                isCreateDialogVisible = true,
                draftName = "",
                draftType = CategoryType.EXPENSE,
                isSaving = false,
                validationErrorMessage = null,
                persistenceErrorMessage = null,
                successMessage = null,
            )
        }
    }

    private fun closeCreateDialog() {
        if (_uiState.value.isSaving) return
        _uiState.update {
            it.copy(
                isCreateDialogVisible = false,
                draftName = "",
                validationErrorMessage = null,
                persistenceErrorMessage = null,
            )
        }
    }

    private fun updateDraftName(name: String) {
        _uiState.update {
            it.copy(
                draftName = name,
                validationErrorMessage = null,
                persistenceErrorMessage = null,
            )
        }
    }

    private fun updateDraftType(type: CategoryType) {
        _uiState.update {
            it.copy(
                draftType = type,
                validationErrorMessage = null,
                persistenceErrorMessage = null,
            )
        }
    }

    private fun createCategory() {
        val current = _uiState.value
        if (current.isSaving) return

        _uiState.update {
            it.copy(
                isSaving = true,
                validationErrorMessage = null,
                persistenceErrorMessage = null,
                successMessage = null,
            )
        }

        val result =
            createCategoryUseCase(
                CreateCategoryCommand(
                    id = idGenerator.nextId(),
                    name = current.draftName,
                    type = current.draftType,
                    createdAt = nowProvider(),
                ),
            )
        when (result) {
            is ApplicationResult.Success -> refreshAfterCreate(result.outcome.value.name)
            is ApplicationResult.Failure -> handleCreateFailure(result.error)
        }
    }

    private fun refreshAfterCreate(createdName: String) {
        when (val result = listCategoriesUseCase()) {
            is ApplicationResult.Success -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        groups = result.outcome.value.toGroups(),
                        loadErrorMessage = null,
                        isCreateDialogVisible = false,
                        draftName = "",
                        isSaving = false,
                        validationErrorMessage = null,
                        persistenceErrorMessage = null,
                        successMessage = "$createdName added",
                    )
                }
            }
            is ApplicationResult.Failure -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isCreateDialogVisible = false,
                        draftName = "",
                        isSaving = false,
                        loadErrorMessage = result.error.toLoadMessage(),
                        validationErrorMessage = null,
                        persistenceErrorMessage = null,
                    )
                }
            }
        }
    }

    private fun handleCreateFailure(error: ApplicationError) {
        _uiState.update {
            when (error) {
                is ApplicationError.Validation ->
                    it.copy(
                        isSaving = false,
                        validationErrorMessage = error.reason,
                        persistenceErrorMessage = null,
                    )
                is ApplicationError.Repository, is ApplicationError.Ledger ->
                    it.copy(
                        isSaving = false,
                        validationErrorMessage = null,
                        persistenceErrorMessage = "Unable to save category.",
                    )
            }
        }
    }

    private fun dismissMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
}

private fun List<Category>.toGroups(): List<CategoryGroupUiModel> =
    CategoryType.entries.mapNotNull { type ->
        val rows =
            filter { it.type == type }
                .map { category ->
                    CategoryRowUiModel(
                        id = category.id,
                        name = category.name,
                        isDefault = category.isDefault,
                    )
                }
        if (rows.isEmpty()) {
            null
        } else {
            CategoryGroupUiModel(
                type = type,
                title = type.toTitle(),
                categories = rows,
            )
        }
    }

private fun CategoryType.toTitle(): String =
    when (this) {
        CategoryType.INCOME -> "Income"
        CategoryType.EXPENSE -> "Expense"
    }

private fun ApplicationError.toLoadMessage(): String =
    when (this) {
        is ApplicationError.Validation -> "$field: $reason"
        is ApplicationError.Repository -> "Unable to load categories."
        is ApplicationError.Ledger -> "Unable to prepare categories."
    }
