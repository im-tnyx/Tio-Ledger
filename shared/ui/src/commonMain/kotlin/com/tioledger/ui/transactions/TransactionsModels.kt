package com.tioledger.ui.transactions

import com.tioledger.domain.model.TransactionType

data class TransactionsUiState(
    val isLoading: Boolean = true,
    val transactions: List<TransactionRowUiModel> = emptyList(),
    val errorMessage: String? = null,
) {
    val isEmpty: Boolean
        get() = !isLoading && errorMessage == null && transactions.isEmpty()
}

data class TransactionRowUiModel(
    val id: String,
    val title: String,
    val subtitle: String,
    val amount: String,
    val type: TransactionType,
)

sealed interface TransactionsAction {
    data object Load : TransactionsAction

    data object Retry : TransactionsAction
}
