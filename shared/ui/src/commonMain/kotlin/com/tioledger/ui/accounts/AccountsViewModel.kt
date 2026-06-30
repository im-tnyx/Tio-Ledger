package com.tioledger.ui.accounts

import com.tioledger.application.model.ApplicationError
import com.tioledger.application.model.ApplicationResult
import com.tioledger.application.usecase.account.AccountBalanceSummary
import com.tioledger.application.usecase.account.AccountsBalanceOverview
import com.tioledger.application.usecase.account.ListAccountSummariesUseCase
import com.tioledger.core.model.Money
import com.tioledger.domain.model.AccountType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AccountsViewModel(
    private val listAccountSummariesUseCase: ListAccountSummariesUseCase,
) {
    private val _uiState = MutableStateFlow(AccountsUiState())
    val uiState: StateFlow<AccountsUiState> = _uiState.asStateFlow()

    init {
        onAction(AccountsAction.Load)
    }

    fun onAction(action: AccountsAction) {
        when (action) {
            AccountsAction.Load, AccountsAction.Retry -> loadAccounts()
            is AccountsAction.SearchChanged -> {
                _uiState.value = _uiState.value.copy(searchQuery = action.query)
                loadAccounts()
            }
        }
    }

    private fun loadAccounts() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        when (val result = listAccountSummariesUseCase()) {
            is ApplicationResult.Success -> {
                _uiState.value = result.outcome.value.toUiState(_uiState.value.searchQuery)
            }
            is ApplicationResult.Failure -> {
                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.error.toMessage(),
                    )
            }
        }
    }
}

private fun AccountsBalanceOverview.toUiState(searchQuery: String): AccountsUiState {
    val filteredAccounts =
        accounts.filter { summary ->
            searchQuery.isBlank() || summary.account.name.contains(searchQuery, ignoreCase = true)
        }

    return AccountsUiState(
        isLoading = false,
        searchQuery = searchQuery,
        summary =
            totals.firstOrNull()?.let {
                AccountsSummaryUiModel(
                    assets = it.assets.toDisplayAmount(),
                    liabilities = it.liabilities.toDisplayAmount(),
                    total = it.total.toDisplayAmount(),
                    currencyLabel = if (totals.size == 1) it.currencyCode else "Multiple currencies",
                )
            } ?: AccountsSummaryUiModel.Empty,
        groups =
            AccountType.entries.mapNotNull { type ->
                val items = filteredAccounts.filter { it.account.type == type }
                if (items.isEmpty()) {
                    null
                } else {
                    AccountGroupUiModel(
                        type = type,
                        title = type.toGroupTitle(),
                        total = items.totalBalanceFor(type).toDisplayAmount(),
                        accounts = items.map { it.toRow() },
                    )
                }
            },
    )
}

private fun List<AccountBalanceSummary>.totalBalanceFor(type: AccountType): Money {
    val first = first()
    return filter { it.account.type == type }
        .fold(Money.zero(first.balance.currency)) { total, summary -> total + summary.balance }
}

private fun AccountBalanceSummary.toRow(): AccountRowUiModel =
    AccountRowUiModel(
        id = account.id,
        name = account.name,
        typeLabel = account.type.toGroupTitle(),
        balance = balance.toDisplayAmount(),
        currencyCode = account.currencyCode,
        isLiability = account.type.ledgerClass == com.tioledger.domain.model.LedgerClass.LIABILITY,
    )

private fun AccountType.toGroupTitle(): String =
    when (this) {
        AccountType.CASH -> "Cash"
        AccountType.BANK -> "Accounts"
        AccountType.CREDIT_CARD -> "Credit Card"
        AccountType.WALLET -> "Wallet"
        AccountType.LOAN_LINKED -> "Loan"
        AccountType.INVESTMENT -> "Investments"
    }

private fun ApplicationError.toMessage(): String =
    when (this) {
        is ApplicationError.Validation -> "$field: $reason"
        is ApplicationError.Repository -> "Unable to load accounts."
        is ApplicationError.Ledger -> "Unable to calculate account balances."
    }
