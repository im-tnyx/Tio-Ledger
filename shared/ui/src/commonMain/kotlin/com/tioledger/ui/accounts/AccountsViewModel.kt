package com.tioledger.ui.accounts

import com.tioledger.application.model.ApplicationError
import com.tioledger.application.model.ApplicationResult
import com.tioledger.application.usecase.account.AccountBalanceSummary
import com.tioledger.application.usecase.account.AccountsBalanceOverview
import com.tioledger.application.usecase.account.CreateAccountCommand
import com.tioledger.application.usecase.account.CreateAccountUseCase
import com.tioledger.application.usecase.account.ListAccountSummariesUseCase
import com.tioledger.application.usecase.account.withSearchQuery
import com.tioledger.core.model.CurrencyCode
import com.tioledger.core.model.Money
import com.tioledger.core.util.IdGenerator
import com.tioledger.domain.model.AccountType
import com.tioledger.ui.more.AccountGroupEntry
import com.tioledger.ui.more.AccountGroupStore
import com.tioledger.ui.more.CurrencyPreferenceStore
import com.tioledger.ui.more.DEFAULT_ACCOUNT_GROUPS
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock

class AccountsViewModel(
    private val listAccountSummariesUseCase: ListAccountSummariesUseCase,
    private val createAccountUseCase: CreateAccountUseCase,
    private val idGenerator: IdGenerator,
    private val nowProvider: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val accountGroupStore: AccountGroupStore? = null,
    private val currencyPreferenceStore: CurrencyPreferenceStore? = null,
) {
    private val _uiState = MutableStateFlow(AccountsUiState())
    val uiState: StateFlow<AccountsUiState> = _uiState.asStateFlow()
    private var loadedOverview: AccountsBalanceOverview? = null

    init {
        onAction(AccountsAction.Load)
    }

    fun onAction(action: AccountsAction) {
        when (action) {
            AccountsAction.Load, AccountsAction.Retry -> loadAccounts()
            is AccountsAction.SearchChanged -> {
                _uiState.value = _uiState.value.copy(searchQuery = action.query)
                loadedOverview?.let { overview ->
                    val projected =
                        overview.withSearchQuery(action.query).toUiState(
                            action.query,
                            accountGroupStore,
                            currencyPreferenceStore,
                        )
                    _uiState.value = _uiState.value.copy(groups = projected.groups)
                }
            }
            AccountsAction.AddClicked -> {
                val groups = accountGroupStore?.groups?.value ?: DEFAULT_ACCOUNT_GROUPS
                val firstGroup = groups.firstOrNull()
                _uiState.value =
                    _uiState.value.copy(
                        isCreateDialogVisible = true,
                        createErrorMessage = null,
                        availableGroups = groups,
                        draftGroupId = firstGroup?.id.orEmpty(),
                        draftType = firstGroup?.policy?.accountType ?: AccountType.CASH,
                        draftCurrencyCode = currentCurrencyCode(),
                    )
            }
            AccountsAction.CreateDismissed -> dismissCreateDialog()
            is AccountsAction.NameChanged ->
                _uiState.value = _uiState.value.copy(draftName = action.name, createErrorMessage = null)
            is AccountsAction.TypeChanged -> _uiState.value = _uiState.value.copy(draftType = action.type)
            is AccountsAction.GroupChanged -> {
                _uiState.value.availableGroups.firstOrNull { it.id == action.groupId }?.let { group ->
                    _uiState.value =
                        _uiState.value.copy(
                            draftGroupId = group.id,
                            draftType = group.policy.accountType,
                        )
                }
            }
            is AccountsAction.CurrencyChanged -> _uiState.value = _uiState.value.copy(draftCurrencyCode = action.currencyCode)
            AccountsAction.SaveClicked -> createAccount()
        }
    }

    private fun dismissCreateDialog() {
        if (_uiState.value.isSaving) return
        _uiState.value = resetCreateDraft(_uiState.value)
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
                currencyCode = current.draftCurrencyCode,
                createdAt = nowProvider(),
            )
        when (val result = createAccountUseCase(command)) {
            is ApplicationResult.Success -> {
                accountGroupStore?.assignAccount(command.id, current.draftGroupId)
                _uiState.value = resetCreateDraft(_uiState.value)
                loadAccounts()
            }
            is ApplicationResult.Failure ->
                _uiState.value = _uiState.value.copy(isSaving = false, createErrorMessage = result.error.toCreateMessage())
        }
    }

    private fun loadAccounts() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        when (val result = listAccountSummariesUseCase()) {
            is ApplicationResult.Success -> {
                loadedOverview = result.outcome.value
                _uiState.value =
                    result.outcome.value
                        .withSearchQuery(_uiState.value.searchQuery)
                        .toUiState(_uiState.value.searchQuery, accountGroupStore, currencyPreferenceStore)
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

    private fun currentCurrencyCode(): String =
        currencyPreferenceStore?.getConfig()?.currency?.code?.normalized ?: SUPPORTED_ACCOUNT_CURRENCY_CODES.first()
}

private fun AccountsBalanceOverview.toUiState(
    searchQuery: String,
    accountGroupStore: AccountGroupStore? = null,
    currencyPreferenceStore: CurrencyPreferenceStore? = null,
): AccountsUiState {
    val uiGroups =
        accountGroupStore?.let { store ->
            toAccountGroupUiModels(searchQuery, store.groups.value, store)
        } ?: groups.map { group ->
            AccountGroupUiModel(
                type = group.type,
                id = group.type.name,
                title = group.type.toGroupTitle(),
                totals = group.totals.map { it.toDisplayAmount() },
                accounts = group.accounts.map { it.toRow() },
            )
        }
    val availableGroups = accountGroupStore?.groups?.value ?: DEFAULT_ACCOUNT_GROUPS
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
        groups = uiGroups,
        availableGroups = availableGroups,
        draftGroupId = availableGroups.firstOrNull()?.id.orEmpty(),
        draftType = availableGroups.firstOrNull()?.policy?.accountType ?: AccountType.CASH,
        draftCurrencyCode =
            currencyPreferenceStore?.getConfig()?.currency?.code?.normalized
                ?: SUPPORTED_ACCOUNT_CURRENCY_CODES.first(),
    )
}

private fun AccountsBalanceOverview.toAccountGroupUiModels(
    searchQuery: String,
    groupEntries: List<AccountGroupEntry>,
    groupStore: AccountGroupStore,
): List<AccountGroupUiModel> {
    val visibleAccounts =
        accounts.filter {
            searchQuery.isBlank() || it.account.name.contains(searchQuery, ignoreCase = true)
        }
    val permanentByType = groupEntries.filter { it.isPermanent }.associateBy { it.policy.accountType }
    val grouped =
        visibleAccounts.groupBy { summary ->
            val assignedId = groupStore.groupIdForAccount(summary.account.id)
            groupEntries.firstOrNull { it.id == assignedId }
                ?: permanentByType[summary.account.type]
                ?: groupEntries.firstOrNull { it.policy.accountType == summary.account.type }
        }
    val configuredGroups =
        groupEntries.mapNotNull { entry ->
            val summaries = grouped[entry].orEmpty()
            if (summaries.isEmpty()) return@mapNotNull null
            AccountGroupUiModel(
                type = entry.policy.accountType,
                id = entry.id,
                title = entry.title,
                totals = summaries.toGroupTotals(),
                accounts = summaries.map { it.toRow() },
            )
        }
    val fallbackGroups =
        grouped[null].orEmpty()
            .groupBy { it.account.type }
            .map { (type, summaries) ->
                AccountGroupUiModel(
                    type = type,
                    id = "fallback-${type.name}",
                    title = type.toGroupTitle(),
                    totals = summaries.toGroupTotals(),
                    accounts = summaries.map { it.toRow() },
                )
            }
    return configuredGroups + fallbackGroups
}

private fun List<AccountBalanceSummary>.toGroupTotals(): List<String> =
    groupBy { it.account.currencyCode }
        .entries
        .sortedBy { it.key }
        .map { entry ->
            val currency = CurrencyCode(entry.key)
            val total = entry.value.fold(Money.zero(currency)) { sum, summary -> sum + summary.balance }
            total.toDisplayAmount()
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
        AccountType.CREDIT_CARD -> "Card"
        AccountType.WALLET -> "Others"
        AccountType.LOAN_LINKED -> "Loan"
        AccountType.INVESTMENT -> "Investments"
    }

private fun ApplicationError.toMessage(): String =
    when (this) {
        is ApplicationError.Validation -> "$field: $reason"
        is ApplicationError.Repository -> "Unable to load accounts."
        is ApplicationError.Ledger -> "Unable to calculate account balances."
    }

private fun ApplicationError.toCreateMessage(): String =
    when (this) {
        is ApplicationError.Validation -> "$field: $reason"
        is ApplicationError.Repository -> "Unable to save this account. Try again."
        is ApplicationError.Ledger -> "Unable to save this account. Try again."
    }

private fun resetCreateDraft(state: AccountsUiState): AccountsUiState =
    state.copy(
        isCreateDialogVisible = false,
        draftName = "",
        draftType = AccountType.CASH,
        draftGroupId = state.availableGroups.firstOrNull()?.id.orEmpty(),
        draftCurrencyCode = SUPPORTED_ACCOUNT_CURRENCY_CODES.first(),
        isSaving = false,
        createErrorMessage = null,
    )
