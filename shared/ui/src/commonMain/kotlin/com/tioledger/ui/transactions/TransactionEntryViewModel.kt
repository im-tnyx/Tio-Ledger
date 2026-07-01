package com.tioledger.ui.transactions

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class TransactionEntryViewModel {
    private val _uiState = MutableStateFlow(TransactionEntryUiState())
    val uiState: StateFlow<TransactionEntryUiState> = _uiState.asStateFlow()

    fun onAction(action: TransactionEntryAction) {
        when (action) {
            is TransactionEntryAction.TypeChanged -> {
                _uiState.update {
                    val updated = it.copy(transactionType = action.type)
                    updated.copy(canSave = canSave(updated))
                }
            }

            is TransactionEntryAction.AmountChanged -> {
                _uiState.update {
                    val updated = it.copy(amount = action.amount)
                    updated.copy(canSave = canSave(updated))
                }
            }

            is TransactionEntryAction.AccountClicked -> {
                Unit
            }

            is TransactionEntryAction.CategoryClicked -> {
                Unit
            }

            is TransactionEntryAction.DateClicked -> {
                Unit
            }

            is TransactionEntryAction.NoteChanged -> {
                _uiState.update { it.copy(note = action.note) }
            }

            is TransactionEntryAction.SaveClicked -> {
                Unit
            }
        }
    }

    private fun canSave(state: TransactionEntryUiState): Boolean =
        state.amount.isNotBlank() && state.selectedAccount != null && state.selectedCategory != null
}
