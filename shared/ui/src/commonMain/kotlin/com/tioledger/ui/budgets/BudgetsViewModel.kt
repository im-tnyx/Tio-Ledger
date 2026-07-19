package com.tioledger.ui.budgets

import com.tioledger.application.model.ApplicationError
import com.tioledger.application.model.ApplicationResult
import com.tioledger.application.usecase.budget.BudgetSummary
import com.tioledger.application.usecase.budget.CreateBudgetCommand
import com.tioledger.application.usecase.budget.CreateBudgetUseCase
import com.tioledger.application.usecase.budget.ListBudgetSummariesUseCase
import com.tioledger.application.usecase.budget.UpdateBudgetCommand
import com.tioledger.application.usecase.budget.UpdateBudgetUseCase
import com.tioledger.application.usecase.category.ListCategoriesUseCase
import com.tioledger.budget.engine.BudgetProgressStatus
import com.tioledger.core.model.Money
import com.tioledger.core.util.IdGenerator
import com.tioledger.domain.model.Budget
import com.tioledger.domain.model.BudgetPeriodType
import com.tioledger.domain.model.Category
import com.tioledger.domain.model.CategoryType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs

class BudgetsViewModel(
    private val listBudgetSummariesUseCase: ListBudgetSummariesUseCase,
    private val listCategoriesUseCase: ListCategoriesUseCase,
    private val createBudgetUseCase: CreateBudgetUseCase,
    private val updateBudgetUseCase: UpdateBudgetUseCase,
    private val idGenerator: IdGenerator,
    private val nowProvider: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val timeZoneIdProvider: () -> String = { TimeZone.currentSystemDefault().id },
) {
    private val _uiState = MutableStateFlow(BudgetsUiState())
    val uiState: StateFlow<BudgetsUiState> = _uiState.asStateFlow()

    init {
        onAction(BudgetsAction.Load)
    }

    fun onAction(action: BudgetsAction) {
        when (action) {
            BudgetsAction.Load, BudgetsAction.Retry -> loadBudgets()
            BudgetsAction.AddClicked -> openAddEditor()
            is BudgetsAction.EditClicked -> openEditEditor(action.budgetId)
            BudgetsAction.EditorDismissed -> closeEditor()
            is BudgetsAction.NameChanged -> updateEditor { copy(name = action.name) }
            is BudgetsAction.AmountChanged -> updateEditor { copy(amount = action.amount) }
            is BudgetsAction.CurrencyChanged -> updateEditor { copy(currencyCode = action.currencyCode.uppercase()) }
            is BudgetsAction.PeriodChanged -> updateEditor { copy(periodType = action.periodType) }
            BudgetsAction.CategoryClicked -> openCategoryPicker()
            is BudgetsAction.CategorySelected -> selectCategory(action.categoryId)
            BudgetsAction.CategoryPickerDismissed -> closeCategoryPicker()
            BudgetsAction.SaveClicked -> saveBudget()
            BudgetsAction.MessageDismissed -> dismissMessage()
        }
    }

    private fun loadBudgets() {
        _uiState.update {
            it.copy(
                isLoading = true,
                loadErrorMessage = null,
                editor = null,
                isCategoryPickerVisible = false,
                isSaving = false,
                validationErrorMessage = null,
                persistenceErrorMessage = null,
            )
        }

        val categoriesResult = listCategoriesUseCase(CategoryType.EXPENSE)
        if (categoriesResult is ApplicationResult.Failure) {
            showLoadFailure(categoriesResult.error)
            return
        }
        require(categoriesResult is ApplicationResult.Success)

        val summariesResult = currentSummaries()
        when (summariesResult) {
            is ApplicationResult.Success -> {
                val timeZoneId = timeZoneIdProvider()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        budgets = summariesResult.outcome.value.map { summary -> summary.toUiModel(timeZoneId) },
                        categoryOptions = categoriesResult.outcome.value.toCategoryOptions(),
                        loadErrorMessage = null,
                    )
                }
            }
            is ApplicationResult.Failure -> showLoadFailure(summariesResult.error)
        }
    }

    private fun openAddEditor() {
        val defaultCurrency = _uiState.value.budgets.firstOrNull()?.currencyCode.orEmpty()
        _uiState.update {
            it.copy(
                editor = BudgetEditorUiState(currencyCode = defaultCurrency),
                isCategoryPickerVisible = false,
                isSaving = false,
                validationErrorMessage = null,
                persistenceErrorMessage = null,
                successMessage = null,
            )
        }
    }

    private fun openEditEditor(budgetId: String) {
        val budget = _uiState.value.budgets.firstOrNull { it.id == budgetId } ?: return
        _uiState.update {
            it.copy(
                editor =
                    BudgetEditorUiState(
                        budgetId = budget.id,
                        name = budget.name,
                        amount = budget.targetMinorUnits.toEditableAmount(),
                        currencyCode = budget.currencyCode,
                        categoryId = budget.categoryId,
                        periodType = budget.periodType,
                    ),
                isCategoryPickerVisible = false,
                isSaving = false,
                validationErrorMessage = null,
                persistenceErrorMessage = null,
                successMessage = null,
            )
        }
    }

    private fun closeEditor() {
        if (_uiState.value.isSaving) return
        _uiState.update {
            it.copy(
                editor = null,
                isCategoryPickerVisible = false,
                validationErrorMessage = null,
                persistenceErrorMessage = null,
            )
        }
    }

    private fun updateEditor(transform: BudgetEditorUiState.() -> BudgetEditorUiState) {
        _uiState.update { current ->
            val editor = current.editor ?: return@update current
            current.copy(
                editor = editor.transform(),
                validationErrorMessage = null,
                persistenceErrorMessage = null,
                successMessage = null,
            )
        }
    }

    private fun openCategoryPicker() {
        if (_uiState.value.editor == null || _uiState.value.isSaving) return
        _uiState.update {
            it.copy(
                isCategoryPickerVisible = true,
                validationErrorMessage = null,
                persistenceErrorMessage = null,
            )
        }
    }

    private fun selectCategory(categoryId: String?) {
        _uiState.update { current ->
            val editor = current.editor ?: return@update current
            current.copy(
                editor = editor.copy(categoryId = categoryId),
                isCategoryPickerVisible = false,
                validationErrorMessage = null,
                persistenceErrorMessage = null,
            )
        }
    }

    private fun closeCategoryPicker() {
        _uiState.update { it.copy(isCategoryPickerVisible = false) }
    }

    private fun saveBudget() {
        val current = _uiState.value
        val editor = current.editor ?: return
        if (current.isSaving) return

        validateEditor(editor)?.let { message ->
            _uiState.update {
                it.copy(
                    validationErrorMessage = message,
                    persistenceErrorMessage = null,
                )
            }
            return
        }
        val amount = requireNotNull(editor.amount.toMinorUnitsOrNull())
        val timestamp = nowProvider()

        _uiState.update {
            it.copy(
                isSaving = true,
                validationErrorMessage = null,
                persistenceErrorMessage = null,
                successMessage = null,
            )
        }

        val result =
            editor.budgetId?.let { budgetId ->
                updateBudgetUseCase(
                    UpdateBudgetCommand(
                        budgetId = budgetId,
                        name = editor.name,
                        amount = amount,
                        currencyCode = editor.currencyCode,
                        categoryId = editor.categoryId,
                        periodType = editor.periodType,
                        updatedAt = timestamp,
                    ),
                )
            } ?: createBudgetUseCase(
                CreateBudgetCommand(
                    id = idGenerator.nextId(),
                    name = editor.name,
                    amount = amount,
                    currencyCode = editor.currencyCode,
                    categoryId = editor.categoryId,
                    periodType = editor.periodType,
                    createdAt = timestamp,
                ),
            )

        when (result) {
            is ApplicationResult.Success -> refreshAfterSave(result.outcome.value, editor.isEditing)
            is ApplicationResult.Failure -> showSaveFailure(result.error)
        }
    }

    private fun refreshAfterSave(
        budget: Budget,
        wasEditing: Boolean,
    ) {
        when (val result = currentSummaries()) {
            is ApplicationResult.Success -> {
                val timeZoneId = timeZoneIdProvider()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        budgets = result.outcome.value.map { summary -> summary.toUiModel(timeZoneId) },
                        loadErrorMessage = null,
                        editor = null,
                        isCategoryPickerVisible = false,
                        isSaving = false,
                        validationErrorMessage = null,
                        persistenceErrorMessage = null,
                        successMessage = if (wasEditing) "${budget.name} updated" else "${budget.name} added",
                    )
                }
            }
            is ApplicationResult.Failure -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        budgets = emptyList(),
                        loadErrorMessage = result.error.toLoadMessage(),
                        editor = null,
                        isCategoryPickerVisible = false,
                        isSaving = false,
                        validationErrorMessage = null,
                        persistenceErrorMessage = null,
                    )
                }
            }
        }
    }

    private fun currentSummaries(): ApplicationResult<List<BudgetSummary>> =
        listBudgetSummariesUseCase(
            anchorTimestamp = nowProvider(),
            timeZoneId = timeZoneIdProvider(),
        )

    private fun showLoadFailure(error: ApplicationError) {
        _uiState.update {
            it.copy(
                isLoading = false,
                budgets = emptyList(),
                loadErrorMessage = error.toLoadMessage(),
            )
        }
    }

    private fun showSaveFailure(error: ApplicationError) {
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
                        persistenceErrorMessage = "Unable to save budget.",
                    )
            }
        }
    }

    private fun dismissMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
}

private fun validateEditor(editor: BudgetEditorUiState): String? {
    if (editor.name.isBlank()) return "Enter a budget name."
    if (editor.amount.toMinorUnitsOrNull() == null) return amountValidationMessage(editor.amount)
    val currency = editor.currencyCode.trim().uppercase()
    if (currency.length != 3 || !currency.all { it in 'A'..'Z' }) return "Enter a 3-letter currency code."
    if (editor.periodType == BudgetPeriodType.CUSTOM) return "Custom budget periods are not supported in v1."
    return null
}

private fun String.toMinorUnitsOrNull(): Long? {
    val normalized = trim().replace(",", "")
    if (normalized.isBlank()) return null
    if (normalized.count { it == '.' } > 1 || normalized.any { it != '.' && it !in '0'..'9' }) return null
    val parts = normalized.split('.')
    if (parts.size > 2) return null
    val majorPart = parts.firstOrNull().orEmpty().ifBlank { "0" }
    val fractionPart = parts.getOrNull(1).orEmpty()
    if (fractionPart.length > 2) return null
    val majorUnits = majorPart.toLongOrNull() ?: return null
    val fractionUnits =
        when (fractionPart.length) {
            0 -> 0L
            1 -> fractionPart.toLong() * 10L
            else -> fractionPart.toLong()
        }
    if (
        majorUnits > Long.MAX_VALUE / 100L ||
        (majorUnits == Long.MAX_VALUE / 100L && fractionUnits > Long.MAX_VALUE % 100L)
    ) {
        return null
    }
    return (majorUnits * 100L + fractionUnits).takeIf { it > 0L }
}

private fun amountValidationMessage(rawAmount: String): String {
    val normalized = rawAmount.trim().replace(",", "")
    return when {
        normalized.isBlank() -> "Enter a budget amount."
        normalized.count { it == '.' } > 1 || normalized.any { it != '.' && it !in '0'..'9' } ->
            "Amount must contain digits only."
        (normalized.split('.').getOrNull(1)?.length ?: 0) > 2 -> "Use at most 2 decimal places."
        else -> "Enter a valid positive amount."
    }
}

private fun List<Category>.toCategoryOptions(): List<BudgetCategoryOption> =
    listOf(BudgetCategoryOption(id = null, name = "All expenses")) +
        map { category -> BudgetCategoryOption(id = category.id, name = category.name) }

private fun BudgetSummary.toUiModel(timeZoneId: String): BudgetRowUiModel =
    BudgetRowUiModel(
        id = id,
        name = name,
        categoryId = categoryId,
        categoryLabel =
            when {
                categoryId == null -> "All expenses"
                categoryName != null -> categoryName
                else -> "Unavailable category"
            },
        periodType = periodType,
        periodLabel = periodType.toDisplayName(),
        periodDateRange = formatDateRange(periodStartInclusive, periodEndExclusive, timeZoneId),
        targetMinorUnits = target.amount,
        currencyCode = target.currency.toString(),
        targetLabel = target.toDisplayAmount(),
        spentLabel = spent.toDisplayAmount(),
        remainingLabel = remaining.toDisplayAmount(),
        utilizationPermille = utilizationPermille,
        status = status,
        statusLabel = status.toDisplayName(),
    )

private fun BudgetPeriodType.toDisplayName(): String =
    when (this) {
        BudgetPeriodType.WEEKLY -> "Weekly"
        BudgetPeriodType.MONTHLY -> "Monthly"
        BudgetPeriodType.YEARLY -> "Yearly"
        BudgetPeriodType.CUSTOM -> "Custom"
    }

private fun BudgetProgressStatus.toDisplayName(): String =
    when (this) {
        BudgetProgressStatus.ON_TRACK -> "On track"
        BudgetProgressStatus.WARNING -> "Near limit"
        BudgetProgressStatus.REACHED -> "Limit reached"
        BudgetProgressStatus.EXCEEDED -> "Over budget"
    }

private fun formatDateRange(
    startInclusive: Long,
    endExclusive: Long,
    timeZoneId: String,
): String {
    val timeZone = TimeZone.of(timeZoneId)
    val startDate = Instant.fromEpochMilliseconds(startInclusive).toLocalDateTime(timeZone).date
    val endDate = Instant.fromEpochMilliseconds(endExclusive - 1L).toLocalDateTime(timeZone).date
    return "$startDate – $endDate"
}

private fun Long.toEditableAmount(): String {
    val major = this / 100L
    val minor = this % 100L
    return if (minor == 0L) major.toString() else "$major.${minor.toString().padStart(2, '0')}"
}

private fun Money.toDisplayAmount(): String {
    val absolute = abs(amount)
    val major = absolute / 100L
    val minor = absolute % 100L
    val sign = if (amount < 0L) "-" else ""
    return "$sign$currency $major.${minor.toString().padStart(2, '0')}"
}

private fun ApplicationError.toLoadMessage(): String =
    when (this) {
        is ApplicationError.Validation -> "$field: $reason"
        is ApplicationError.Repository -> "Unable to load budgets."
        is ApplicationError.Ledger -> "Unable to prepare budget progress."
    }
