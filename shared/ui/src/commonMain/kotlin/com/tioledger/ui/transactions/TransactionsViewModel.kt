package com.tioledger.ui.transactions

import com.tioledger.application.model.ApplicationError
import com.tioledger.application.model.ApplicationResult
import com.tioledger.application.usecase.transaction.ListTransactionsUseCase
import com.tioledger.application.usecase.transaction.TransactionSummary
import com.tioledger.core.model.Money
import com.tioledger.domain.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs

class TransactionsViewModel(
    private val listTransactionsUseCase: ListTransactionsUseCase,
) {
    private val _uiState = MutableStateFlow(TransactionsUiState())
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()

    init {
        onAction(TransactionsAction.Load)
    }

    fun onAction(action: TransactionsAction) {
        when (action) {
            TransactionsAction.Load, TransactionsAction.Retry -> loadTransactions()
        }
    }

    private fun loadTransactions() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        when (val result = listTransactionsUseCase()) {
            is ApplicationResult.Success -> {
                _uiState.value =
                    TransactionsUiState(
                        isLoading = false,
                        transactions = result.outcome.value.map(TransactionSummary::toRow),
                    )
            }
            is ApplicationResult.Failure -> {
                _uiState.value =
                    TransactionsUiState(
                        isLoading = false,
                        errorMessage = result.error.toMessage(),
                    )
            }
        }
    }
}

private fun TransactionSummary.toRow(): TransactionRowUiModel {
    val title =
        when (type) {
            TransactionType.INCOME -> categoryName ?: description ?: "Income"
            TransactionType.EXPENSE -> categoryName ?: description ?: "Expense"
            TransactionType.TRANSFER -> "$sourceAccountName → ${destinationAccountName ?: "Transfer"}"
            TransactionType.LOAN_DISBURSEMENT -> description ?: "Loan disbursement"
            TransactionType.REPAYMENT -> description ?: "Loan repayment"
            TransactionType.ADJUSTMENT -> description ?: "Balance adjustment"
        }
    val context =
        when (type) {
            TransactionType.TRANSFER -> "Transfer"
            else -> sourceAccountName
        }
    val detailParts =
        buildList {
            add(timestamp.toDateLabel())
            add(context)
            description
                ?.takeUnless { it == title }
                ?.let { add(it) }
        }

    return TransactionRowUiModel(
        id = id,
        title = title,
        subtitle = detailParts.joinToString(" • "),
        amount = amount.toDisplayAmount(type),
        type = type,
    )
}

private fun Long.toDateLabel(): String =
    Instant.fromEpochMilliseconds(this)
        .toLocalDateTime(TimeZone.UTC)
        .date
        .toString()

private fun Money.toDisplayAmount(type: TransactionType): String {
    val absolute = abs(amount)
    val major = absolute / MINOR_UNITS
    val minor = absolute % MINOR_UNITS
    val prefix =
        when (type) {
            TransactionType.INCOME, TransactionType.LOAN_DISBURSEMENT -> "+"
            TransactionType.EXPENSE, TransactionType.REPAYMENT -> "-"
            TransactionType.TRANSFER, TransactionType.ADJUSTMENT -> ""
        }
    return "$prefix$currency $major.${minor.toString().padStart(2, '0')}"
}

private fun ApplicationError.toMessage(): String =
    when (this) {
        is ApplicationError.Validation -> "$field: $reason"
        is ApplicationError.Repository -> "Unable to load transactions."
        is ApplicationError.Ledger -> "Unable to prepare transaction history."
    }

private const val MINOR_UNITS = 100L
