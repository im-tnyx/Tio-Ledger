package com.tioledger.ui.accounts

import com.tioledger.application.model.ApplicationError
import com.tioledger.application.model.ApplicationResult
import com.tioledger.application.usecase.account.AccountBalanceSummary
import com.tioledger.application.usecase.account.AccountsBalanceOverview
import com.tioledger.application.usecase.account.ArchiveAccountCommand
import com.tioledger.application.usecase.account.ArchiveAccountUseCase
import com.tioledger.application.usecase.account.CreateAccountCommand
import com.tioledger.application.usecase.account.CreateAccountUseCase
import com.tioledger.application.usecase.account.ListAccountSummariesUseCase
import com.tioledger.application.usecase.account.UpdateAccountCommand
import com.tioledger.application.usecase.account.UpdateAccountUseCase
import com.tioledger.application.usecase.account.withSearchQuery
import com.tioledger.application.usecase.loan.ListLoansUseCase
import com.tioledger.core.model.CurrencyCode
import com.tioledger.core.model.Money
import com.tioledger.core.util.IdGenerator
import com.tioledger.domain.model.AccountType
import com.tioledger.ui.more.AccountGroupAssignmentResult
import com.tioledger.ui.more.AccountGroupEntry
import com.tioledger.ui.more.AccountGroupStore
import com.tioledger.ui.more.CurrencyPreferenceStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock

class AccountsViewModel(
    private val listAccountSummariesUseCase: ListAccountSummariesUseCase,
    private val createAccountUseCase: CreateAccountUseCase,
    private val archiveAccountUseCase: ArchiveAccountUseCase,
    private val idGenerator: IdGenerator,
    private val nowProvider: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val accountGroupStore: AccountGroupStore,
    private val currencyPreferenceStore: CurrencyPreferenceStore? = null,
    private val listLoansUseCase: ListLoansUseCase? = null,
    private val updateAccountUseCase: UpdateAccountUseCase? = null,
) {
    private val _uiState =
        MutableStateFlow(
            AccountsUiState(
                draftCurrencyCode = currentCurrencyCode(),
                currencySymbol = currentCurrencySymbol(),
            ),
        )
    val uiState: StateFlow<AccountsUiState> = _uiState.asStateFlow()
    private var loadedOverview: AccountsBalanceOverview? = null
    private var loanIdByAccountId: Map<String, String> = emptyMap()

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
                            loanIdByAccountId,
                        )
                    _uiState.value = _uiState.value.copy(groups = projected.groups)
                }
            }
            AccountsAction.AddClicked -> {
                val groups = accountGroupStore.groups.value
                val firstGroup = groups.firstOrNull()
                _uiState.value =
                    _uiState.value.copy(
                        isCreateDialogVisible = true,
                        editingAccountId = null,
                        createErrorMessage = if (firstGroup == null) "A valid Account Group is required." else null,
                        availableGroups = groups,
                        draftGroupId = firstGroup?.id.orEmpty(),
                        draftCurrencyCode = currentCurrencyCode(),
                        currencySymbol = currentCurrencySymbol(),
                    )
            }
            is AccountsAction.EditAccountRequested -> {
                val accountSummary = loadedOverview?.accounts?.firstOrNull { it.account.id == action.accountId }
                val assignedGroupId =
                    accountGroupStore.groupIdForAccount(action.accountId)
                        ?: _uiState.value.availableGroups.firstOrNull()?.id.orEmpty()
                val currency = accountSummary?.account?.currencyCode ?: currentCurrencyCode()
                _uiState.value =
                    _uiState.value.copy(
                        editingAccountId = action.accountId,
                        draftName = accountSummary?.account?.name.orEmpty(),
                        draftGroupId = assignedGroupId,
                        draftCurrencyCode = currency,
                        currencySymbol = currencySymbolFor(currency),
                        createErrorMessage = null,
                    )
            }
            AccountsAction.CreateDismissed -> dismissCreateDialog()
            is AccountsAction.NameChanged ->
                _uiState.value = _uiState.value.copy(draftName = action.name, createErrorMessage = null)
            is AccountsAction.GroupChanged -> {
                accountGroupStore.groupForId(action.groupId)?.let { group ->
                    _uiState.value =
                        _uiState.value.copy(
                            draftGroupId = group.id,
                            createErrorMessage = null,
                        )
                }
            }
            AccountsAction.SaveClicked -> saveAccount()
            is AccountsAction.ArchiveRequested -> {
                _uiState.value =
                    _uiState.value.copy(
                        archiveAccountId = action.accountId,
                        archiveAccountName = action.accountName,
                        archiveErrorMessage = null,
                    )
            }
            AccountsAction.ArchiveDismissed -> dismissArchiveDialog()
            AccountsAction.ArchiveConfirmed -> archiveAccount()
        }
    }

    private fun dismissCreateDialog() {
        if (_uiState.value.isSaving) return
        _uiState.value = resetCreateDraft(_uiState.value, currencyPreferenceStore)
    }

    private fun saveAccount() {
        val current = _uiState.value
        if (current.editingAccountId != null) {
            updateAccount()
        } else {
            createAccount()
        }
    }

    private fun updateAccount() {
        val current = _uiState.value
        val accountId = current.editingAccountId ?: return
        if (current.isSaving) return
        _uiState.value = current.copy(isSaving = true, createErrorMessage = null)
        val group = accountGroupStore.groupForId(current.draftGroupId)
        if (group == null) {
            _uiState.value = current.copy(isSaving = false, createErrorMessage = "A valid Account Group is required.")
            return
        }
        val command =
            UpdateAccountCommand(
                accountId = accountId,
                name = current.draftName,
                displayOrder = 0,
                updatedAt = nowProvider(),
                currencyCode = current.draftCurrencyCode,
                type = group.policy.accountType,
            )
        if (updateAccountUseCase != null) {
            when (val result = updateAccountUseCase.invoke(command)) {
                is ApplicationResult.Success -> {
                    accountGroupStore.assignAccount(accountId, group.id)
                    _uiState.value = resetCreateDraft(_uiState.value, currencyPreferenceStore)
                    loadAccounts()
                }
                is ApplicationResult.Failure -> {
                    _uiState.value = current.copy(isSaving = false, createErrorMessage = result.error.toCreateMessage())
                }
            }
        } else {
            accountGroupStore.assignAccount(accountId, group.id)
            _uiState.value = resetCreateDraft(_uiState.value, currencyPreferenceStore)
            loadAccounts()
        }
    }

    private fun createAccount() {
        val current = _uiState.value
        if (current.isSaving) return
        _uiState.value = current.copy(isSaving = true, createErrorMessage = null)
        val group = accountGroupStore.groupForId(current.draftGroupId)
        if (group == null) {
            _uiState.value = current.copy(isSaving = false, createErrorMessage = "A valid Account Group is required.")
            return
        }
        val command =
            CreateAccountCommand(
                id = idGenerator.nextId(),
                name = current.draftName,
                type = group.policy.accountType,
                currencyCode = current.draftCurrencyCode,
                createdAt = nowProvider(),
            )
        when (accountGroupStore.prepareAccountAssignment(command.id, group.id, command.type)) {
            is AccountGroupAssignmentResult.Prepared -> Unit
            AccountGroupAssignmentResult.GroupNotFound -> {
                _uiState.value = current.copy(isSaving = false, createErrorMessage = "A valid Account Group is required.")
                return
            }
            AccountGroupAssignmentResult.PolicyMismatch -> {
                _uiState.value = current.copy(isSaving = false, createErrorMessage = "The selected group policy is invalid.")
                return
            }
            AccountGroupAssignmentResult.PersistenceFailure -> {
                _uiState.value =
                    current.copy(
                        isSaving = false,
                        createErrorMessage = "Unable to save the account group assignment. Try again.",
                    )
                return
            }
        }
        when (val result = createAccountUseCase(command)) {
            is ApplicationResult.Success -> {
                _uiState.value = resetCreateDraft(_uiState.value, currencyPreferenceStore)
                loadAccounts()
            }
            is ApplicationResult.Failure -> {
                val cleanupSucceeded = accountGroupStore.removePreparedAccountAssignment(command.id, group.id)
                val message =
                    if (cleanupSucceeded) {
                        result.error.toCreateMessage()
                    } else {
                        "Account was not saved. Temporary group assignment cleanup failed."
                    }
                _uiState.value = _uiState.value.copy(isSaving = false, createErrorMessage = message)
            }
        }
    }

    private fun dismissArchiveDialog() {
        if (_uiState.value.isArchiving) return
        _uiState.value =
            _uiState.value.copy(
                archiveAccountId = null,
                archiveAccountName = null,
                archiveErrorMessage = null,
            )
    }

    private fun archiveAccount() {
        val current = _uiState.value
        val accountId = current.archiveAccountId ?: return
        if (current.isArchiving) return
        _uiState.value = current.copy(isArchiving = true, archiveErrorMessage = null)
        when (
            val result =
                archiveAccountUseCase(
                    ArchiveAccountCommand(
                        accountId = accountId,
                        archivedAt = nowProvider(),
                    ),
                )
        ) {
            is ApplicationResult.Success -> {
                _uiState.value =
                    _uiState.value.copy(
                        archiveAccountId = null,
                        archiveAccountName = null,
                        isArchiving = false,
                        archiveErrorMessage = null,
                    )
                loadAccounts()
            }
            is ApplicationResult.Failure -> {
                _uiState.value =
                    current.copy(
                        isArchiving = false,
                        archiveErrorMessage = result.error.toArchiveMessage(),
                    )
            }
        }
    }

    private fun loadAccounts() {
        currencyPreferenceStore?.reload()
        if (currencyPreferenceStore != null && updateAccountUseCase != null) {
            val mainCode = currentCurrencyCode()
            when (val initialResult = listAccountSummariesUseCase()) {
                is ApplicationResult.Success -> {
                    val mismatched = initialResult.outcome.value.accounts.filter { it.account.currencyCode != mainCode }
                    if (mismatched.isNotEmpty()) {
                        mismatched.forEach { summary ->
                            updateAccountUseCase(
                                UpdateAccountCommand(
                                    accountId = summary.account.id,
                                    name = summary.account.name,
                                    displayOrder = summary.account.displayOrder,
                                    updatedAt = nowProvider(),
                                    currencyCode = mainCode,
                                    type = summary.account.type,
                                ),
                            )
                        }
                    }
                }
                is ApplicationResult.Failure -> Unit
            }
        }
        loanIdByAccountId =
            listLoansUseCase?.let { useCase ->
                when (val result = useCase()) {
                    is ApplicationResult.Success -> result.outcome.value.associate { it.loan.accountId to it.loan.id }
                    is ApplicationResult.Failure -> emptyMap()
                }
            } ?: emptyMap()
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        when (val result = listAccountSummariesUseCase()) {
            is ApplicationResult.Success -> {
                loadedOverview = result.outcome.value
                _uiState.value =
                    result.outcome.value
                        .withSearchQuery(_uiState.value.searchQuery)
                        .toUiState(_uiState.value.searchQuery, accountGroupStore, currencyPreferenceStore, loanIdByAccountId)
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

    private fun currentCurrencySymbol(): String = currencyPreferenceStore?.getConfig()?.currency?.symbol ?: "₹"
}

private fun AccountsBalanceOverview.toUiState(
    searchQuery: String,
    accountGroupStore: AccountGroupStore,
    currencyPreferenceStore: CurrencyPreferenceStore? = null,
    loanIdByAccountId: Map<String, String> = emptyMap(),
): AccountsUiState {
    val symbol = currencyPreferenceStore?.getConfig()?.currency?.symbol
    val uiGroups =
        toAccountGroupUiModels(
            searchQuery = searchQuery,
            groupEntries = accountGroupStore.groups.value,
            groupStore = accountGroupStore,
            loanIdByAccountId = loanIdByAccountId,
            currencySymbol = symbol,
        )
    val availableGroups = accountGroupStore.groups.value
    return AccountsUiState(
        isLoading = false,
        searchQuery = searchQuery,
        summary =
            totals.firstOrNull()?.let {
                AccountsSummaryUiModel(
                    assets = it.assets.toDisplayAmount(symbol),
                    liabilities = it.liabilities.toDisplayAmount(symbol),
                    total = it.total.toDisplayAmount(symbol),
                    currencyLabel = if (totals.size == 1) it.currencyCode else "Multiple currencies",
                )
            } ?: AccountsSummaryUiModel.Empty,
        groups = uiGroups,
        availableGroups = availableGroups,
        draftGroupId = availableGroups.firstOrNull()?.id.orEmpty(),
        draftCurrencyCode =
            currencyPreferenceStore?.getConfig()?.currency?.code?.normalized
                ?: SUPPORTED_ACCOUNT_CURRENCY_CODES.first(),
        currencySymbol =
            currencyPreferenceStore?.getConfig()?.currency?.symbol
                ?: "₹",
    )
}

private fun AccountsBalanceOverview.toAccountGroupUiModels(
    searchQuery: String,
    groupEntries: List<AccountGroupEntry>,
    groupStore: AccountGroupStore,
    loanIdByAccountId: Map<String, String> = emptyMap(),
    currencySymbol: String? = null,
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
                totals = summaries.toGroupTotals(currencySymbol),
                accounts = summaries.map { it.toRow(loanIdByAccountId, currencySymbol) },
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
                    totals = summaries.toGroupTotals(currencySymbol),
                    accounts = summaries.map { it.toRow(loanIdByAccountId, currencySymbol) },
                )
            }
    return configuredGroups + fallbackGroups
}

private fun List<AccountBalanceSummary>.toGroupTotals(currencySymbol: String? = null): List<String> =
    groupBy { it.account.currencyCode }
        .toSortedMap()
        .map { (currencyCode, summaries) ->
            val currency = CurrencyCode(currencyCode)
            val total = summaries.fold(Money.zero(currency)) { sum, summary -> sum + summary.balance }
            total.toDisplayAmount(currencySymbol)
        }

private fun AccountBalanceSummary.toRow(
    loanIdByAccountId: Map<String, String> = emptyMap(),
    currencySymbol: String? = null,
): AccountRowUiModel {
    val isLoanAccount = account.type == AccountType.LOAN_LINKED
    return AccountRowUiModel(
        id = account.id,
        name = account.name,
        typeLabel = account.type.toGroupTitle(),
        balance = balance.toDisplayAmount(currencySymbol),
        currencyCode = account.currencyCode,
        isLiability = account.type.ledgerClass == com.tioledger.domain.model.LedgerClass.LIABILITY,
        isLoan = isLoanAccount,
        loanId = if (isLoanAccount) loanIdByAccountId[account.id] else null,
    )
}

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

private fun ApplicationError.toArchiveMessage(): String =
    when (this) {
        is ApplicationError.Validation -> "$field: $reason"
        is ApplicationError.Repository -> "Unable to archive this account. Try again."
        is ApplicationError.Ledger -> "Unable to archive this account. Try again."
    }

private fun resetCreateDraft(
    state: AccountsUiState,
    currencyPreferenceStore: CurrencyPreferenceStore? = null,
): AccountsUiState =
    state.copy(
        isCreateDialogVisible = false,
        editingAccountId = null,
        draftName = "",
        draftGroupId = state.availableGroups.firstOrNull()?.id.orEmpty(),
        draftCurrencyCode =
            currencyPreferenceStore?.getConfig()?.currency?.code?.normalized
                ?: SUPPORTED_ACCOUNT_CURRENCY_CODES.first(),
        currencySymbol =
            currencyPreferenceStore?.getConfig()?.currency?.symbol
                ?: "₹",
        isSaving = false,
        createErrorMessage = null,
    )
