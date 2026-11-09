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
import com.tioledger.application.usecase.transaction.RecordOpeningBalanceCommand
import com.tioledger.application.usecase.transaction.RecordOpeningBalanceUseCase
import com.tioledger.core.model.CurrencyCode
import com.tioledger.core.model.Money
import com.tioledger.core.util.IdGenerator
import com.tioledger.domain.model.AccountType
import com.tioledger.ui.more.AccountBillingDetails
import com.tioledger.ui.more.AccountGroupAssignmentResult
import com.tioledger.ui.more.AccountGroupEntry
import com.tioledger.ui.more.AccountGroupPolicy
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
    private val recordOpeningBalanceUseCase: RecordOpeningBalanceUseCase? = null,
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
                val accountType = accountSummary?.account?.type
                val assignedGroupId =
                    accountGroupStore.groupIdForAccount(action.accountId)?.takeIf { groupId ->
                        accountGroupStore.groupForId(groupId)?.policy?.accountType == accountType
                    } ?: _uiState.value.availableGroups.firstOrNull { it.policy.accountType == accountType }?.id.orEmpty()
                val currency = accountSummary?.account?.currencyCode ?: currentCurrencyCode()
                val billingDetails = accountGroupStore.billingDetailsForAccount(action.accountId)
                _uiState.value =
                    _uiState.value.copy(
                        editingAccountId = action.accountId,
                        draftName = accountSummary?.account?.name.orEmpty(),
                        draftGroupId = assignedGroupId,
                        draftCurrencyCode = currency,
                        currencySymbol = currencySymbolFor(currency),
                        draftSettlementDate = (billingDetails?.billingDay ?: 1).toString(),
                        draftPaymentDate = (billingDetails?.paymentDueDay ?: 1).toString(),
                        draftPaymentAccountId = billingDetails?.paymentAccountId,
                        draftDescription = billingDetails?.description.orEmpty(),
                        createErrorMessage = null,
                    )
            }
            AccountsAction.CreateDismissed -> dismissCreateDialog()
            is AccountsAction.NameChanged ->
                _uiState.value = _uiState.value.copy(draftName = action.name, createErrorMessage = null)
            is AccountsAction.OpeningBalanceChanged ->
                _uiState.value = _uiState.value.copy(draftOpeningBalance = action.amount, createErrorMessage = null)
            is AccountsAction.PaymentAccountChanged ->
                _uiState.value = _uiState.value.copy(draftPaymentAccountId = action.accountId, createErrorMessage = null)
            is AccountsAction.DescriptionChanged ->
                _uiState.value = _uiState.value.copy(draftDescription = action.description, createErrorMessage = null)
            is AccountsAction.SettlementDateChanged ->
                _uiState.value = _uiState.value.copy(draftSettlementDate = action.day, createErrorMessage = null)
            is AccountsAction.PaymentDateChanged ->
                _uiState.value = _uiState.value.copy(draftPaymentDate = action.day, createErrorMessage = null)
            is AccountsAction.GroupChanged -> {
                accountGroupStore.groupForId(action.groupId)?.let { group ->
                    val editingAccount =
                        _uiState.value.editingAccountId?.let { id ->
                            loadedOverview?.accounts?.firstOrNull { it.account.id == id }?.account
                        }
                    if (editingAccount != null && editingAccount.type != group.policy.accountType) {
                        _uiState.value =
                            _uiState.value.copy(
                                createErrorMessage = "Changing an account's financial type is not supported.",
                            )
                        return
                    }
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
        if (current.pendingOpeningBalanceAccountId != null) {
            retryOpeningBalance()
        } else if (current.editingAccountId != null) {
            updateAccount()
        } else {
            createAccount()
        }
    }

    private fun retryOpeningBalance() {
        val current = _uiState.value
        val accountId = current.pendingOpeningBalanceAccountId ?: return
        if (current.isSaving) return
        val amountMinor = parseAmountToMinor(current.draftOpeningBalance)
        if (amountMinor == null) {
            _uiState.value = current.copy(createErrorMessage = "Enter a valid amount with up to 2 decimal places.")
            return
        }
        if (amountMinor == 0L) {
            _uiState.value = resetCreateDraft(current.copy(pendingOpeningBalanceAccountId = null), currencyPreferenceStore)
            loadAccounts()
            return
        }
        val openingBalanceUseCase = recordOpeningBalanceUseCase
        if (openingBalanceUseCase == null) {
            _uiState.value = current.copy(createErrorMessage = "Opening balance posting is unavailable.")
            return
        }
        _uiState.value = current.copy(isSaving = true, createErrorMessage = null)
        val now = nowProvider()
        when (
            val result =
                openingBalanceUseCase(
                    RecordOpeningBalanceCommand(
                        timestamp = now,
                        description = "Opening balance",
                        amount = Money(amountMinor, CurrencyCode(current.draftCurrencyCode)),
                        accountId = accountId,
                        createdAt = now,
                    ),
                )
        ) {
            is ApplicationResult.Success -> {
                _uiState.value = resetCreateDraft(_uiState.value.copy(pendingOpeningBalanceAccountId = null), currencyPreferenceStore)
                loadAccounts()
            }
            is ApplicationResult.Failure -> {
                _uiState.value =
                    current.copy(
                        isSaving = false,
                        createErrorMessage = "Opening balance could not be saved. Select Save to retry: ${result.error.toCreateMessage()}",
                    )
            }
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
        val existingAccount = loadedOverview?.accounts?.firstOrNull { it.account.id == accountId }?.account
        if (existingAccount == null || existingAccount.type != group.policy.accountType) {
            _uiState.value = current.copy(isSaving = false, createErrorMessage = "Account group type cannot be changed.")
            return
        }
        val previousGroupId = accountGroupStore.groupIdForAccount(accountId)
        if (!accountGroupStore.assignAccount(accountId, group.id)) {
            _uiState.value = current.copy(isSaving = false, createErrorMessage = "Unable to save the account group assignment. Try again.")
            return
        }
        val oldBillingDetails = accountGroupStore.billingDetailsForAccount(accountId)
        val newBillingDetails = current.toBillingDetailsOrNull(group)
        if (newBillingDetails != null && !accountGroupStore.saveBillingDetails(accountId, newBillingDetails)) {
            restoreAccountAssignment(accountId, previousGroupId)
            _uiState.value = current.copy(isSaving = false, createErrorMessage = "Unable to save billing dates. Try again.")
            return
        }
        val command =
            UpdateAccountCommand(
                accountId = accountId,
                name = current.draftName,
                displayOrder = 0,
                updatedAt = nowProvider(),
            )
        if (updateAccountUseCase != null) {
            when (val result = updateAccountUseCase.invoke(command)) {
                is ApplicationResult.Success -> {
                    _uiState.value = resetCreateDraft(_uiState.value, currencyPreferenceStore)
                    loadAccounts()
                }
                is ApplicationResult.Failure -> {
                    restoreAccountAssignment(accountId, previousGroupId)
                    restoreBillingDetails(accountId, oldBillingDetails)
                    _uiState.value = current.copy(isSaving = false, createErrorMessage = result.error.toCreateMessage())
                }
            }
        } else {
            restoreAccountAssignment(accountId, previousGroupId)
            restoreBillingDetails(accountId, oldBillingDetails)
            _uiState.value = current.copy(isSaving = false, createErrorMessage = "Account editing is unavailable.")
        }
    }

    private fun createAccount() {
        val current = _uiState.value
        if (current.isSaving) return
        val openingBalanceMinor = parseAmountToMinor(current.draftOpeningBalance)
        if (openingBalanceMinor == null) {
            _uiState.value = current.copy(createErrorMessage = "Enter a valid amount with up to 2 decimal places.")
            return
        }
        if (openingBalanceMinor > 0L && recordOpeningBalanceUseCase == null) {
            _uiState.value = current.copy(createErrorMessage = "Opening balance posting is unavailable. Account was not created.")
            return
        }
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
        val billingDetails = current.toBillingDetailsOrNull(group)
        if (billingDetails != null && !accountGroupStore.saveBillingDetails(command.id, billingDetails)) {
            accountGroupStore.removePreparedAccountAssignment(command.id, group.id)
            _uiState.value = current.copy(isSaving = false, createErrorMessage = "Unable to save billing dates. Try again.")
            return
        }
        when (val result = createAccountUseCase(command)) {
            is ApplicationResult.Success -> {
                val created = result.outcome.value
                if (openingBalanceMinor > 0L && recordOpeningBalanceUseCase != null) {
                    val now = nowProvider()
                    val openingBalanceResult =
                        recordOpeningBalanceUseCase(
                            RecordOpeningBalanceCommand(
                                timestamp = now,
                                description = "Opening balance",
                                amount = Money(openingBalanceMinor, CurrencyCode(created.currencyCode)),
                                accountId = created.id,
                                createdAt = now,
                            ),
                        )
                    when (openingBalanceResult) {
                        is ApplicationResult.Success -> {
                            _uiState.value = resetCreateDraft(_uiState.value, currencyPreferenceStore)
                            loadAccounts()
                        }
                        is ApplicationResult.Failure -> {
                            _uiState.value =
                                _uiState.value.copy(
                                    isSaving = false,
                                    pendingOpeningBalanceAccountId = created.id,
                                    createErrorMessage =
                                        "Account created, but opening balance failed: " +
                                            openingBalanceResult.error.toCreateMessage(),
                                )
                        }
                    }
                } else {
                    _uiState.value = resetCreateDraft(_uiState.value, currencyPreferenceStore)
                    loadAccounts()
                }
            }
            is ApplicationResult.Failure -> {
                accountGroupStore.removeBillingDetails(command.id)
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

    private fun restoreAccountAssignment(
        accountId: String,
        previousGroupId: String?,
    ) {
        if (previousGroupId == null) {
            accountGroupStore.groupIdForAccount(accountId)?.let { currentGroupId ->
                accountGroupStore.removePreparedAccountAssignment(accountId, currentGroupId)
            }
        } else {
            accountGroupStore.assignAccount(accountId, previousGroupId)
        }
    }

    private fun restoreBillingDetails(
        accountId: String,
        previous: AccountBillingDetails?,
    ) {
        if (previous == null) {
            accountGroupStore.removeBillingDetails(accountId)
        } else {
            accountGroupStore.saveBillingDetails(accountId, previous)
        }
    }

    private fun loadAccounts() {
        currencyPreferenceStore?.reload()
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
    val uiGroups =
        toAccountGroupUiModels(
            searchQuery = searchQuery,
            groupEntries = accountGroupStore.groups.value,
            groupStore = accountGroupStore,
            loanIdByAccountId = loanIdByAccountId,
            currencyPreferenceStore = currencyPreferenceStore,
        )
    val availableGroups = accountGroupStore.groups.value
    return AccountsUiState(
        isLoading = false,
        searchQuery = searchQuery,
        summary =
            totals.firstOrNull()?.let {
                AccountsSummaryUiModel(
                    assets =
                        totals.map {
                                row ->
                            row.assets.toDisplayAmount(currencyPreferenceStore.symbolFor(row.currencyCode))
                        }.joinToString("\n"),
                    liabilities =
                        totals.map {
                                row ->
                            row.liabilities.toDisplayAmount(currencyPreferenceStore.symbolFor(row.currencyCode))
                        }.joinToString("\n"),
                    total =
                        totals.map {
                                row ->
                            row.total.toDisplayAmount(currencyPreferenceStore.symbolFor(row.currencyCode))
                        }.joinToString("\n"),
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
    currencyPreferenceStore: CurrencyPreferenceStore? = null,
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
                categoryLabel = entry.policy.sectionLabel(),
                accountCount = summaries.size,
                totals = summaries.toGroupTotals(currencyPreferenceStore),
                accounts =
                    summaries.map { summary ->
                        val billingDetails = groupStore.billingDetailsForAccount(summary.account.id)
                        summary.toRow(loanIdByAccountId, currencyPreferenceStore).copy(
                            billingDay = billingDetails?.billingDay,
                            paymentDueDay = billingDetails?.paymentDueDay,
                        )
                    },
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
                    categoryLabel = type.toSectionLabel(),
                    accountCount = summaries.size,
                    totals = summaries.toGroupTotals(currencyPreferenceStore),
                    accounts = summaries.map { it.toRow(loanIdByAccountId, currencyPreferenceStore) },
                )
            }
    return configuredGroups + fallbackGroups
}

private fun AccountGroupPolicy.sectionLabel(): String =
    when (this) {
        AccountGroupPolicy.CASH -> "Cash accounts"
        AccountGroupPolicy.BANK -> "Bank accounts"
        AccountGroupPolicy.CREDIT_CARD -> "Credit card accounts"
        AccountGroupPolicy.DEBIT_CARD -> "Debit card accounts"
        AccountGroupPolicy.LOAN -> "Loan accounts"
        AccountGroupPolicy.INVESTMENT -> "Investment accounts"
        AccountGroupPolicy.OTHER -> "Other accounts"
    }

private fun AccountType.toSectionLabel(): String =
    when (this) {
        AccountType.CASH -> "Cash accounts"
        AccountType.BANK -> "Bank accounts"
        AccountType.CREDIT_CARD -> "Credit card accounts"
        AccountType.WALLET -> "Other accounts"
        AccountType.LOAN_LINKED -> "Loan accounts"
        AccountType.INVESTMENT -> "Investment accounts"
    }

private fun AccountsUiState.toBillingDetailsOrNull(group: AccountGroupEntry): AccountBillingDetails? {
    if (group.policy != AccountGroupPolicy.CREDIT_CARD) return null
    val billingDay = draftSettlementDate.toIntOrNull() ?: return null
    val paymentDay = draftPaymentDate.toIntOrNull() ?: return null
    return AccountBillingDetails(
        billingDay = billingDay,
        paymentDueDay = paymentDay,
        paymentAccountId = draftPaymentAccountId,
        description = draftDescription,
    )
}

private fun List<AccountBalanceSummary>.toGroupTotals(currencyPreferenceStore: CurrencyPreferenceStore? = null): List<String> =
    groupBy { it.account.currencyCode }
        .toSortedMap()
        .map { (currencyCode, summaries) ->
            val currency = CurrencyCode(currencyCode)
            val total = summaries.fold(Money.zero(currency)) { sum, summary -> sum + summary.balance }
            total.toDisplayAmount(currencyPreferenceStore.symbolFor(currencyCode))
        }

private fun CurrencyPreferenceStore?.symbolFor(currencyCode: String): String? =
    this?.getConfig()?.currency?.takeIf { it.code.normalized == currencyCode }?.symbol

private fun AccountBalanceSummary.toRow(
    loanIdByAccountId: Map<String, String> = emptyMap(),
    currencyPreferenceStore: CurrencyPreferenceStore? = null,
): AccountRowUiModel {
    val isLoanAccount = account.type == AccountType.LOAN_LINKED
    return AccountRowUiModel(
        id = account.id,
        name = account.name,
        typeLabel = account.type.toGroupTitle(),
        balance = balance.toDisplayAmount(currencyPreferenceStore.symbolFor(account.currencyCode)),
        currencyCode = account.currencyCode,
        isLiability = account.type.ledgerClass == com.tioledger.domain.model.LedgerClass.LIABILITY,
        isLoan = isLoanAccount,
        loanId = if (isLoanAccount) loanIdByAccountId[account.id] else null,
        accountType = account.type,
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
        draftOpeningBalance = "",
        draftPaymentAccountId = null,
        draftDescription = "",
        draftSettlementDate = "1",
        draftPaymentDate = "1",
        pendingOpeningBalanceAccountId = null,
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

internal fun parseAmountToMinor(input: String): Long? {
    val clean = input.trim().replace(",", "").replace(" ", "").replace("₹", "").replace("$", "")
    if (clean.isBlank()) return 0L
    if (!clean.matches(Regex("[0-9]+(\\.[0-9]{1,2})?"))) return null
    val parts = clean.split(".")
    val major = parts[0].toLongOrNull() ?: return null
    val minor =
        if (parts.size > 1) {
            parts[1].padEnd(2, '0').toLongOrNull() ?: return null
        } else {
            0L
        }
    if (major > (Long.MAX_VALUE - minor) / 100L) return null
    return major * 100L + minor
}
