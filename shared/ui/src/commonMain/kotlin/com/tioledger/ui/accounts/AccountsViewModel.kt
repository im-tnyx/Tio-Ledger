package com.tioledger.ui.accounts

import com.tioledger.application.model.ApplicationError
import com.tioledger.application.model.ApplicationResult
import com.tioledger.application.usecase.account.AccountBalanceSummary
import com.tioledger.application.usecase.account.AccountsBalanceOverview
import com.tioledger.application.usecase.account.CreateAccountCommand
import com.tioledger.application.usecase.account.CreateAccountUseCase
import com.tioledger.application.usecase.account.ListAccountSummariesUseCase
import com.tioledger.core.model.Money
import com.tioledger.core.util.IdGenerator
import com.tioledger.domain.model.AccountType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock

class AccountsViewModel(
    private val listAccountSummariesUseCase: ListAccountSummariesUseCase,
    private val createAccountUseCase: CreateAccountUseCase,
    private val idGenerator: IdGenerator,
    private val nowProvider: () -> Long = { Clock.System.now().toEpochMilliseconds() },
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
            AccountsAction.AddClicked ->
                _uiState.value = _uiState.value.copy(isCreateDialogVisible = true, createErrorMessage = null)
            AccountsAction.CreateDismissed -> dismissCreateDialog()
            is AccountsAction.NameChanged ->
                _uiState.value = _uiState.value.copy(draftName = action.name, createErrorMessage = null)
            is AccountsAction.TypeChanged -> _uiState.value = _uiState.value.copy(draftType = action.type)
            AccountsAction.SaveClicked -> createAccount()
        }
    }

    private fun dismissCreateDialog() {
        if (_uiState.value.isSaving) return
        _uiState.value = _uiState.value.copy(isCreateDialogVisible = false, draftName = "", createErrorMessage = null)
    }

    private fun createAccount() {
        val current = _uiState.value
        if (current.isSaving) return
        _uiState.value = current.copy(isSaving = true, createErrorMessage = null)
        val command =
            CreateAccountCommand(
                id = idGenerator.nextId(),
                name = current.draftName,
                type = current.draftType,
                currencyCode = "USD",
                createdAt = nowProvider(),
            )
        when (val result = createAccountUseCase(command)) {
            is ApplicationResult.Success -> {
                _uiState.value = _uiState.value.copy(isCreateDialogVisible = false, draftName = "", isSaving = false)
                loadAccounts()
            }
            is ApplicationResult.Failure ->
                _uiState.value = _uiState.value.copy(isSaving = false, createErrorMessage = result.error.toMessage())
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
