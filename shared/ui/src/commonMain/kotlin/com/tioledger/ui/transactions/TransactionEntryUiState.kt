package com.tioledger.ui.transactions

data class TransactionEntryUiState(
    val transactionType: TransactionType = TransactionType.Expense,
    val amount: String = "",
    val note: String = "",
    val selectedDate: String = "Today",
    val selectedTimestamp: Long = 0L,
    val accountOptions: List<TransactionAccountOption> = emptyList(),
    val categoryOptions: List<TransactionCategoryOption> = emptyList(),
    val selectedAccountId: String? = null,
    val selectedTargetAccountId: String? = null,
    val selectedCategoryId: String? = null,
    val activeAccountPicker: TransactionAccountPickerTarget? = null,
    val isCategoryPickerVisible: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val loadErrorMessage: String? = null,
    val validationErrorMessage: String? = null,
    val persistenceErrorMessage: String? = null,
    val saveSuccessMessage: String? = null,
) {
    val selectedAccount: TransactionAccountOption?
        get() = accountOptions.firstOrNull { it.id == selectedAccountId }

    val selectedTargetAccount: TransactionAccountOption?
        get() = accountOptions.firstOrNull { it.id == selectedTargetAccountId }

    val selectedCategory: TransactionCategoryOption?
        get() = categoryOptions.firstOrNull { it.id == selectedCategoryId }

    val selectedCurrencyCode: String
        get() = selectedAccount?.currencyCode ?: selectedTargetAccount?.currencyCode ?: accountOptions.firstOrNull()?.currencyCode ?: "USD"

    val isTransfer: Boolean
        get() = transactionType == TransactionType.Transfer

    val canSave: Boolean
        get() =
            !isLoading &&
                !isSaving &&
                amount.isNotBlank() &&
                selectedAccountId != null &&
                if (isTransfer) {
                    selectedTargetAccountId != null && selectedTargetAccountId != selectedAccountId
                } else {
                    selectedCategoryId != null
                }
}

data class TransactionAccountOption(
    val id: String,
    val name: String,
    val currencyCode: String,
    val subtitle: String,
)

data class TransactionCategoryOption(
    val id: String,
    val name: String,
    val subtitle: String,
)

enum class TransactionType(val displayName: String) {
    Expense("Expense"),
    Income("Income"),
    Transfer("Transfer"),
}

enum class TransactionAccountPickerTarget {
    Source,
    Destination,
}

sealed interface TransactionEntryAction {
    data object Load : TransactionEntryAction

    data object Retry : TransactionEntryAction

    data class TypeChanged(val type: TransactionType) : TransactionEntryAction

    data class AmountChanged(val amount: String) : TransactionEntryAction

    data object SourceAccountClicked : TransactionEntryAction

    data object TargetAccountClicked : TransactionEntryAction

    data class AccountSelected(val accountId: String) : TransactionEntryAction

    data object AccountPickerDismissed : TransactionEntryAction

    data object CategoryClicked : TransactionEntryAction

    data class CategorySelected(val categoryId: String) : TransactionEntryAction

    data object CategoryPickerDismissed : TransactionEntryAction

    data object DateClicked : TransactionEntryAction

    data class NoteChanged(val note: String) : TransactionEntryAction

    data object SaveClicked : TransactionEntryAction

    data object MessageDismissed : TransactionEntryAction

    data object EventConsumed : TransactionEntryAction
}

sealed interface TransactionEntryEvent {
    data class TransactionSaved(val transactionId: String) : TransactionEntryEvent
}
