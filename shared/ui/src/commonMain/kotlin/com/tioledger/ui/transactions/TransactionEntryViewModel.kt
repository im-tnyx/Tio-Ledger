package com.tioledger.ui.transactions

import com.tioledger.application.model.ApplicationError
import com.tioledger.application.model.ApplicationResult
import com.tioledger.application.usecase.account.AccountBalanceSummary
import com.tioledger.application.usecase.account.ListAccountSummariesUseCase
import com.tioledger.application.usecase.category.ListCategoriesUseCase
import com.tioledger.application.usecase.transaction.RecordExpenseCommand
import com.tioledger.application.usecase.transaction.RecordExpenseUseCase
import com.tioledger.application.usecase.transaction.RecordIncomeCommand
import com.tioledger.application.usecase.transaction.RecordIncomeUseCase
import com.tioledger.application.usecase.transaction.RecordTransferCommand
import com.tioledger.application.usecase.transaction.RecordTransferUseCase
import com.tioledger.core.model.CurrencyCode
import com.tioledger.core.model.Money
import com.tioledger.domain.model.AccountType
import com.tioledger.domain.model.Category
import com.tioledger.domain.model.CategoryType
import com.tioledger.domain.model.TransactionRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock
import kotlin.math.abs

class TransactionEntryViewModel(
    private val listAccountSummariesUseCase: ListAccountSummariesUseCase,
    private val listCategoriesUseCase: ListCategoriesUseCase,
    private val recordIncomeUseCase: RecordIncomeUseCase,
    private val recordExpenseUseCase: RecordExpenseUseCase,
    private val recordTransferUseCase: RecordTransferUseCase,
    private val nowProvider: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    private val _uiState = MutableStateFlow(TransactionEntryUiState(selectedTimestamp = nowProvider()))
    val uiState: StateFlow<TransactionEntryUiState> = _uiState.asStateFlow()

    private val _event = MutableStateFlow<TransactionEntryEvent?>(null)
    val event: StateFlow<TransactionEntryEvent?> = _event.asStateFlow()

    private var allCategories: List<Category> = emptyList()

    init {
        loadReferenceData()
    }

    fun onAction(action: TransactionEntryAction) {
        when (action) {
            TransactionEntryAction.Load, TransactionEntryAction.Retry -> loadReferenceData()
            is TransactionEntryAction.TypeChanged -> updateTransactionType(action.type)
            is TransactionEntryAction.AmountChanged -> updateAmount(action.amount)
            TransactionEntryAction.SourceAccountClicked -> openAccountPicker(TransactionAccountPickerTarget.Source)
            TransactionEntryAction.TargetAccountClicked -> openAccountPicker(TransactionAccountPickerTarget.Destination)
            is TransactionEntryAction.AccountSelected -> selectAccount(action.accountId)
            TransactionEntryAction.AccountPickerDismissed -> _uiState.update { it.copy(activeAccountPicker = null) }
            TransactionEntryAction.CategoryClicked ->
                _uiState.update {
                    it.copy(
                        isCategoryPickerVisible = true,
                        validationErrorMessage = null,
                        persistenceErrorMessage = null,
                        saveSuccessMessage = null,
                    )
                }
            is TransactionEntryAction.CategorySelected -> selectCategory(action.categoryId)
            TransactionEntryAction.CategoryPickerDismissed -> _uiState.update { it.copy(isCategoryPickerVisible = false) }
            TransactionEntryAction.DateClicked ->
                _event.value = TransactionEntryEvent.DateSelectionRequested(_uiState.value.selectedTimestamp)
            is TransactionEntryAction.DateSelected -> selectDate(action.timestamp, action.label)
            is TransactionEntryAction.NoteChanged ->
                _uiState.update {
                    it.copy(
                        note = action.note,
                        validationErrorMessage = null,
                        persistenceErrorMessage = null,
                        saveSuccessMessage = null,
                    )
                }
            TransactionEntryAction.SaveClicked -> saveTransaction()
            TransactionEntryAction.MessageDismissed ->
                _uiState.update {
                    it.copy(
                        validationErrorMessage = null,
                        persistenceErrorMessage = null,
                        saveSuccessMessage = null,
                    )
                }
            TransactionEntryAction.EventConsumed -> _event.value = null
        }
    }

    private fun loadReferenceData() {
        val currentTimestamp = nowProvider()
        _uiState.update {
            it.copy(
                isLoading = true,
                isSaving = false,
                loadErrorMessage = null,
                validationErrorMessage = null,
                persistenceErrorMessage = null,
                saveSuccessMessage = null,
                activeAccountPicker = null,
                isCategoryPickerVisible = false,
                selectedTimestamp = currentTimestamp,
                selectedDate = "Today",
            )
        }

        val accountsResult = listAccountSummariesUseCase()
        val categoriesResult = listCategoriesUseCase()
        when {
            accountsResult is ApplicationResult.Failure ->
                _uiState.update { it.copy(isLoading = false, loadErrorMessage = accountsResult.error.toLoadMessage()) }
            categoriesResult is ApplicationResult.Failure ->
                _uiState.update { it.copy(isLoading = false, loadErrorMessage = categoriesResult.error.toLoadMessage()) }
            accountsResult is ApplicationResult.Success && categoriesResult is ApplicationResult.Success -> {
                allCategories = categoriesResult.outcome.value
                _uiState.update { current ->
                    val accountOptions = accountsResult.outcome.value.accounts.map { it.toOption() }
                    val availableCategories = categoriesFor(current.transactionType)
                    current.copy(
                        isLoading = false,
                        loadErrorMessage = null,
                        accountOptions = accountOptions,
                        categoryOptions = availableCategories,
                        selectedAccountId = current.selectedAccountId.takeIf { id -> accountOptions.any { it.id == id } },
                        selectedTargetAccountId = current.selectedTargetAccountId.takeIf { id -> accountOptions.any { it.id == id } },
                        selectedCategoryId = current.selectedCategoryId.takeIf { id -> availableCategories.any { it.id == id } },
                    ).normalizedForType()
                }
            }
        }
    }

    private fun updateTransactionType(type: TransactionType) {
        _uiState.update { current ->
            val availableCategories = categoriesFor(type)
            current.copy(
                transactionType = type,
                categoryOptions = availableCategories,
                selectedCategoryId = current.selectedCategoryId.takeIf { id -> availableCategories.any { it.id == id } },
                validationErrorMessage = null,
                persistenceErrorMessage = null,
                saveSuccessMessage = null,
            ).normalizedForType()
        }
    }

    private fun updateAmount(amount: String) {
        _uiState.update {
            it.copy(
                amount = amount,
                validationErrorMessage = null,
                persistenceErrorMessage = null,
                saveSuccessMessage = null,
            )
        }
    }

    private fun openAccountPicker(target: TransactionAccountPickerTarget) {
        _uiState.update {
            it.copy(
                activeAccountPicker = target,
                validationErrorMessage = null,
                persistenceErrorMessage = null,
                saveSuccessMessage = null,
            )
        }
    }

    private fun selectAccount(accountId: String) {
        _uiState.update { current ->
            when (current.activeAccountPicker) {
                TransactionAccountPickerTarget.Source ->
                    current.copy(
                        selectedAccountId = accountId,
                        activeAccountPicker = null,
                        validationErrorMessage = null,
                        persistenceErrorMessage = null,
                        saveSuccessMessage = null,
                    ).normalizedForType()
                TransactionAccountPickerTarget.Destination ->
                    current.copy(
                        selectedTargetAccountId = accountId,
                        activeAccountPicker = null,
                        validationErrorMessage = null,
                        persistenceErrorMessage = null,
                        saveSuccessMessage = null,
                    ).normalizedForType()
                null -> current
            }
        }
    }

    private fun selectCategory(categoryId: String) {
        _uiState.update {
            it.copy(
                selectedCategoryId = categoryId,
                isCategoryPickerVisible = false,
                validationErrorMessage = null,
                persistenceErrorMessage = null,
                saveSuccessMessage = null,
            )
        }
    }

    private fun selectDate(
        timestamp: Long,
        label: String,
    ) {
        if (timestamp < 0L || label.isBlank()) return
        _uiState.update {
            it.copy(
                selectedTimestamp = timestamp,
                selectedDate = label,
                validationErrorMessage = null,
                persistenceErrorMessage = null,
                saveSuccessMessage = null,
            )
        }
        _event.value = null
    }

    private fun saveTransaction() {
        val state = _uiState.value
        val sourceAccountId = state.selectedAccountId
        val amount = parseMoney(state.amount, state.selectedCurrencyCode)
        validate(state, amount)?.let { message ->
            _uiState.update {
                it.copy(
                    validationErrorMessage = message,
                    persistenceErrorMessage = null,
                    saveSuccessMessage = null,
                )
            }
            return
        }

        requireNotNull(sourceAccountId)
        requireNotNull(amount)
        _uiState.update {
            it.copy(
                isSaving = true,
                validationErrorMessage = null,
                persistenceErrorMessage = null,
                saveSuccessMessage = null,
            )
        }

        val result =
            when (state.transactionType) {
                TransactionType.Income ->
                    recordIncomeUseCase(
                        RecordIncomeCommand(
                            timestamp = state.selectedTimestamp,
                            description = state.note.trim().ifBlank { null },
                            amount = amount,
                            accountId = sourceAccountId,
                            categoryId = state.selectedCategoryId,
                            merchantId = null,
                            createdAt = state.selectedTimestamp,
                        ),
                    )
                TransactionType.Expense ->
                    recordExpenseUseCase(
                        RecordExpenseCommand(
                            timestamp = state.selectedTimestamp,
                            description = state.note.trim().ifBlank { null },
                            amount = amount,
                            accountId = sourceAccountId,
                            categoryId = state.selectedCategoryId,
                            merchantId = null,
                            createdAt = state.selectedTimestamp,
                        ),
                    )
                TransactionType.Transfer ->
                    recordTransferUseCase(
                        RecordTransferCommand(
                            timestamp = state.selectedTimestamp,
                            description = state.note.trim().ifBlank { null },
                            amount = amount,
                            sourceAccountId = sourceAccountId,
                            targetAccountId = requireNotNull(state.selectedTargetAccountId),
                            createdAt = state.selectedTimestamp,
                        ),
                    )
            }

        when (result) {
            is ApplicationResult.Success -> onSaveSucceeded(result.outcome.value)
            is ApplicationResult.Failure ->
                _uiState.update {
                    it.copy(isSaving = false, persistenceErrorMessage = result.error.toPersistenceMessage())
                }
        }
    }

    private fun onSaveSucceeded(record: TransactionRecord) {
        _uiState.update {
            it.copy(
                isSaving = false,
                amount = "",
                note = "",
                selectedCategoryId = null,
                validationErrorMessage = null,
                persistenceErrorMessage = null,
                saveSuccessMessage = "Transaction saved successfully.",
            ).normalizedForType()
        }
        _event.value = TransactionEntryEvent.TransactionSaved(record.transaction.id)
    }

    private fun validate(
        state: TransactionEntryUiState,
        amount: Money?,
    ): String? {
        if (state.isLoading) return "Transaction form is still loading."
        if (state.accountOptions.isEmpty()) return "At least one account is required before recording a transaction."
        if (state.selectedAccountId == null) return "Select an account."
        if (amount == null) return amountValidationMessage(state.amount)
        if (state.transactionType != TransactionType.Transfer && state.categoryOptions.isEmpty()) {
            return "No categories are available for this transaction type."
        }
        if (state.transactionType != TransactionType.Transfer && state.selectedCategoryId == null) return "Select a category."
        if (state.transactionType == TransactionType.Transfer && state.selectedTargetAccountId == null) {
            return "Select a destination account."
        }
        if (state.transactionType == TransactionType.Transfer && state.selectedAccountId == state.selectedTargetAccountId) {
            return "Source and destination accounts must be different."
        }
        return null
    }

    private fun parseMoney(
        rawAmount: String,
        currencyCode: String,
    ): Money? {
        val normalized = rawAmount.trim().replace(",", "")
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
            (
                majorUnits == Long.MAX_VALUE / 100L &&
                    fractionUnits > Long.MAX_VALUE % 100L
            )
        ) {
            return null
        }
        val minorUnits = majorUnits * 100L + fractionUnits
        if (minorUnits <= 0L) return null
        return Money(minorUnits, CurrencyCode(currencyCode))
    }

    private fun amountValidationMessage(rawAmount: String): String {
        val normalized = rawAmount.trim().replace(",", "")
        return when {
            normalized.isBlank() -> "Enter an amount."
            normalized.count { it == '.' } > 1 || normalized.any { it != '.' && it !in '0'..'9' } -> "Amount must contain digits only."
            (normalized.split('.').getOrNull(1)?.length ?: 0) > 2 -> "Use at most 2 decimal places."
            else -> "Enter a valid amount."
        }
    }

    private fun categoriesFor(type: TransactionType): List<TransactionCategoryOption> {
        val categoryType =
            when (type) {
                TransactionType.Expense -> CategoryType.EXPENSE
                TransactionType.Income -> CategoryType.INCOME
                TransactionType.Transfer -> null
            }
        return allCategories.asSequence()
            .filter { categoryType == null || it.type == categoryType }
            .map {
                TransactionCategoryOption(
                    id = it.id,
                    name = it.name,
                    subtitle = if (it.isDefault) "Default ${it.type.name.lowercase()}" else it.type.name.lowercase(),
                )
            }
            .toList()
    }
}

private fun AccountBalanceSummary.toOption(): TransactionAccountOption =
    TransactionAccountOption(
        id = account.id,
        name = account.name,
        currencyCode = account.currencyCode,
        subtitle = "${account.type.displayName()} - ${balance.toDisplayAmount()}",
    )

private fun TransactionEntryUiState.normalizedForType(): TransactionEntryUiState =
    if (transactionType == TransactionType.Transfer) copy(selectedCategoryId = null) else copy(selectedTargetAccountId = null)

private fun AccountType.displayName(): String =
    when (this) {
        AccountType.CASH -> "Cash"
        AccountType.BANK -> "Bank"
        AccountType.CREDIT_CARD -> "Credit Card"
        AccountType.WALLET -> "Wallet"
        AccountType.LOAN_LINKED -> "Loan"
        AccountType.INVESTMENT -> "Investment"
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
        is ApplicationError.Repository -> "Unable to load transaction reference data."
        is ApplicationError.Ledger -> "Unable to prepare transaction entry."
    }

private fun ApplicationError.toPersistenceMessage(): String =
    when (this) {
        is ApplicationError.Validation -> "$field: $reason"
        is ApplicationError.Repository -> "Unable to save transaction."
        is ApplicationError.Ledger -> "Transaction could not be posted."
    }
