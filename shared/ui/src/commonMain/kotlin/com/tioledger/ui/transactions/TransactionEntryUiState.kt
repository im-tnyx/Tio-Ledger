package com.tioledger.ui.transactions

data class TransactionEntryUiState(
    val transactionType: TransactionType = TransactionType.Expense,
    val amount: String = "",
    val selectedAccount: String? = null,
    val selectedCategory: String? = null,
    val selectedDate: String = "Today",
    val note: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val canSave: Boolean = false,
)

enum class TransactionType(val displayName: String) {
    Expense("Expense"),
    Income("Income"),
    Transfer("Transfer"),
}

sealed interface TransactionEntryAction {
    data class TypeChanged(val type: TransactionType) : TransactionEntryAction

    data class AmountChanged(val amount: String) : TransactionEntryAction

    data object AccountClicked : TransactionEntryAction

    data object CategoryClicked : TransactionEntryAction

    data object DateClicked : TransactionEntryAction

    data class NoteChanged(val note: String) : TransactionEntryAction

    data object SaveClicked : TransactionEntryAction
}
