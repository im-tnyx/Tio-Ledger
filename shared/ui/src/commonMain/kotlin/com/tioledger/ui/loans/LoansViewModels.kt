package com.tioledger.ui.loans

import com.tioledger.application.model.ApplicationError
import com.tioledger.application.model.ApplicationResult
import com.tioledger.application.usecase.account.AccountsBalanceOverview
import com.tioledger.application.usecase.account.ListAccountSummariesUseCase
import com.tioledger.application.usecase.loan.CreateLoanCommand
import com.tioledger.application.usecase.loan.CreateLoanUseCase
import com.tioledger.application.usecase.loan.GetLoanDetailsUseCase
import com.tioledger.application.usecase.loan.ListLoansUseCase
import com.tioledger.application.usecase.loan.LoanDetailsView
import com.tioledger.application.usecase.loan.LoanOverview
import com.tioledger.core.model.Money
import com.tioledger.core.util.IdGenerator
import com.tioledger.domain.model.Account
import com.tioledger.domain.model.AccountType
import com.tioledger.domain.model.LedgerClass
import com.tioledger.domain.model.LoanInstallment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class LoansViewModel(
    private val listLoansUseCase: ListLoansUseCase,
    private val listAccountSummariesUseCase: ListAccountSummariesUseCase,
    private val createLoanUseCase: CreateLoanUseCase,
    private val idGenerator: IdGenerator,
    private val nowProvider: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val currentDateProvider: () -> LocalDate = {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    },
) {
    private val _uiState = MutableStateFlow(LoansUiState())
    val uiState: StateFlow<LoansUiState> = _uiState.asStateFlow()

    init {
        onAction(LoansAction.Load)
    }

    fun onAction(action: LoansAction) {
        when (action) {
            LoansAction.Load, LoansAction.Retry -> load()
            LoansAction.AddClicked -> openEditor()
            LoansAction.EditorDismissed -> closeEditor()
            is LoansAction.NameChanged -> updateEditor { copy(name = action.value) }
            is LoansAction.PrincipalChanged -> updateEditor { copy(principal = action.value) }
            is LoansAction.InterestRateChanged -> updateEditor { copy(annualInterestRatePercent = action.value) }
            is LoansAction.TenureChanged -> updateEditor { copy(tenureMonths = action.value) }
            is LoansAction.StartDateChanged -> updateEditor { copy(startDate = action.value) }
            LoansAction.LoanAccountClicked -> openPicker(LoanAccountPicker.LOAN_ACCOUNT)
            LoansAction.DisbursedAccountClicked -> openPicker(LoanAccountPicker.DISBURSED_ACCOUNT)
            is LoansAction.LoanAccountSelected -> selectLoanAccount(action.accountId)
            is LoansAction.DisbursedAccountSelected -> selectDisbursedAccount(action.accountId)
            LoansAction.AccountPickerDismissed -> closePicker()
            LoansAction.SaveClicked -> save()
            LoansAction.MessageDismissed -> _uiState.update { it.copy(successMessage = null) }
        }
    }

    private fun load() {
        _uiState.update {
            it.copy(
                isLoading = true,
                loadErrorMessage = null,
                editor = null,
                accountPicker = null,
                isSaving = false,
                validationErrorMessage = null,
                persistenceErrorMessage = null,
            )
        }

        val accounts = loadAccounts() ?: return
        when (val result = listLoansUseCase()) {
            is ApplicationResult.Success -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loans = result.outcome.value.map(LoanOverview::toRowUiModel),
                        accountOptions = accounts.toAccountOptions(),
                        loadErrorMessage = null,
                    )
                }
            }
            is ApplicationResult.Failure -> showLoadFailure(result.error)
        }
    }

    private fun loadAccounts(): AccountsBalanceOverview? =
        when (val result = listAccountSummariesUseCase(includeArchived = false)) {
            is ApplicationResult.Success -> result.outcome.value
            is ApplicationResult.Failure -> {
                showLoadFailure(result.error)
                null
            }
        }

    private fun openEditor() {
        val options = _uiState.value.accountOptions
        val loanAccount = options.firstOrNull { it.type == AccountType.LOAN_LINKED }
        val disbursedAccount =
            options.firstOrNull {
                it.type != AccountType.LOAN_LINKED &&
                    it.ledgerClass == LedgerClass.ASSET &&
                    it.currencyCode == loanAccount?.currencyCode
            }
        _uiState.update {
            it.copy(
                editor =
                    LoanEditorUiState(
                        startDate = currentDateProvider().toString(),
                        loanAccountId = loanAccount?.id,
                        disbursedAccountId = disbursedAccount?.id,
                    ),
                accountPicker = null,
                validationErrorMessage = null,
                persistenceErrorMessage = null,
                successMessage = null,
            )
        }
    }

    private fun closeEditor() {
        if (_uiState.value.isSaving) return
        _uiState.update {
            it.copy(
                editor = null,
                accountPicker = null,
                validationErrorMessage = null,
                persistenceErrorMessage = null,
            )
        }
    }

    private fun updateEditor(transform: LoanEditorUiState.() -> LoanEditorUiState) {
        _uiState.update { current ->
            val editor = current.editor ?: return@update current
            current.copy(
                editor = editor.transform(),
                validationErrorMessage = null,
                persistenceErrorMessage = null,
                successMessage = null,
            )
        }
    }

    private fun openPicker(picker: LoanAccountPicker) {
        if (_uiState.value.editor == null || _uiState.value.isSaving) return
        _uiState.update {
            it.copy(
                accountPicker = picker,
                validationErrorMessage = null,
                persistenceErrorMessage = null,
            )
        }
    }

    private fun closePicker() {
        _uiState.update { it.copy(accountPicker = null) }
    }

    private fun selectLoanAccount(accountId: String) {
        _uiState.update { current ->
            val editor = current.editor ?: return@update current
            val loanAccount = current.accountOptions.firstOrNull { it.id == accountId } ?: return@update current
            val retainedDisbursedAccount =
                current.accountOptions.firstOrNull {
                    it.id == editor.disbursedAccountId &&
                        it.currencyCode == loanAccount.currencyCode &&
                        it.type != AccountType.LOAN_LINKED &&
                        it.ledgerClass == LedgerClass.ASSET
                }
            current.copy(
                editor =
                    editor.copy(
                        loanAccountId = loanAccount.id,
                        disbursedAccountId = retainedDisbursedAccount?.id,
                    ),
                accountPicker = null,
                validationErrorMessage = null,
                persistenceErrorMessage = null,
            )
        }
    }

    private fun selectDisbursedAccount(accountId: String) {
        updateEditor { copy(disbursedAccountId = accountId) }
        closePicker()
    }

    private fun save() {
        val current = _uiState.value
        val editor = current.editor ?: return
        if (current.isSaving) return

        val parsed = editor.parse() ?: run {
            _uiState.update {
                it.copy(
                    validationErrorMessage = editor.validationMessage(),
                    persistenceErrorMessage = null,
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isSaving = true,
                validationErrorMessage = null,
                persistenceErrorMessage = null,
                successMessage = null,
            )
        }

        val result =
            createLoanUseCase(
                CreateLoanCommand(
                    id = idGenerator.nextId(),
                    name = editor.name,
                    principalAmount = parsed.principalAmount,
                    annualInterestRateBasisPoints = parsed.annualInterestRateBasisPoints,
                    tenureMonths = parsed.tenureMonths,
                    startDate = parsed.startDate,
                    accountId = requireNotNull(editor.loanAccountId),
                    disbursedAccountId = requireNotNull(editor.disbursedAccountId),
                    createdAt = nowProvider(),
                ),
            )

        when (result) {
            is ApplicationResult.Success -> refreshAfterCreate(result.outcome.value.overview.loan.name)
            is ApplicationResult.Failure -> showSaveFailure(result.error)
        }
    }

    private fun refreshAfterCreate(loanName: String) {
        when (val result = listLoansUseCase()) {
            is ApplicationResult.Success -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loans = result.outcome.value.map(LoanOverview::toRowUiModel),
                        loadErrorMessage = null,
                        editor = null,
                        accountPicker = null,
                        isSaving = false,
                        validationErrorMessage = null,
                        persistenceErrorMessage = null,
                        successMessage = "$loanName added",
                    )
                }
            }
            is ApplicationResult.Failure -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loans = emptyList(),
                        loadErrorMessage = result.error.toLoadMessage(),
                        editor = null,
                        accountPicker = null,
                        isSaving = false,
                        validationErrorMessage = null,
                        persistenceErrorMessage = null,
                    )
                }
            }
        }
    }

    private fun showLoadFailure(error: ApplicationError) {
        _uiState.update {
            it.copy(
                isLoading = false,
                loans = emptyList(),
                loadErrorMessage = error.toLoadMessage(),
            )
        }
    }

    private fun showSaveFailure(error: ApplicationError) {
        _uiState.update {
            when (error) {
                is ApplicationError.Validation ->
                    it.copy(
                        isSaving = false,
                        validationErrorMessage = error.reason,
                        persistenceErrorMessage = null,
                    )
                is ApplicationError.Repository, is ApplicationError.Ledger ->
                    it.copy(
                        isSaving = false,
                        validationErrorMessage = null,
                        persistenceErrorMessage = "Unable to save loan.",
                    )
            }
        }
    }
}

class LoanDetailsViewModel(
    private val getLoanDetailsUseCase: GetLoanDetailsUseCase,
    private val listAccountSummariesUseCase: ListAccountSummariesUseCase,
) {
    private val _uiState = MutableStateFlow(LoanDetailsUiState())
    val uiState: StateFlow<LoanDetailsUiState> = _uiState.asStateFlow()

    fun load(loanId: String) {
        if (_uiState.value.loanId == loanId && _uiState.value.details != null) return
        _uiState.update {
            it.copy(
                loanId = loanId,
                isLoading = true,
                errorMessage = null,
                details = null,
            )
        }

        val accounts =
            when (val result = listAccountSummariesUseCase(includeArchived = true)) {
                is ApplicationResult.Success -> result.outcome.value.accounts.map { it.account }
                is ApplicationResult.Failure -> {
                    showFailure(result.error)
                    return
                }
            }
        when (val result = getLoanDetailsUseCase(loanId)) {
            is ApplicationResult.Success -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = null,
                        details = result.outcome.value.toDetailsUiModel(accounts),
                    )
                }
            }
            is ApplicationResult.Failure -> showFailure(result.error)
        }
    }

    fun retry() {
        val loanId = _uiState.value.loanId ?: return
        _uiState.update { it.copy(details = null) }
        load(loanId)
    }

    private fun showFailure(error: ApplicationError) {
        _uiState.update {
            it.copy(
                isLoading = false,
                details = null,
                errorMessage = error.toDetailsMessage(),
            )
        }
    }
}

sealed interface LoansAction {
    data object Load : LoansAction

    data object Retry : LoansAction

    data object AddClicked : LoansAction

    data object EditorDismissed : LoansAction

    data class NameChanged(val value: String) : LoansAction

    data class PrincipalChanged(val value: String) : LoansAction

    data class InterestRateChanged(val value: String) : LoansAction

    data class TenureChanged(val value: String) : LoansAction

    data class StartDateChanged(val value: String) : LoansAction

    data object LoanAccountClicked : LoansAction

    data object DisbursedAccountClicked : LoansAction

    data class LoanAccountSelected(val accountId: String) : LoansAction

    data class DisbursedAccountSelected(val accountId: String) : LoansAction

    data object AccountPickerDismissed : LoansAction

    data object SaveClicked : LoansAction

    data object MessageDismissed : LoansAction
}

enum class LoanAccountPicker {
    LOAN_ACCOUNT,
    DISBURSED_ACCOUNT,
}

data class LoansUiState(
    val isLoading: Boolean = true,
    val loans: List<LoanRowUiModel> = emptyList(),
    val accountOptions: List<LoanAccountOption> = emptyList(),
    val loadErrorMessage: String? = null,
    val editor: LoanEditorUiState? = null,
    val accountPicker: LoanAccountPicker? = null,
    val isSaving: Boolean = false,
    val validationErrorMessage: String? = null,
    val persistenceErrorMessage: String? = null,
    val successMessage: String? = null,
) {
    val isEmpty: Boolean
        get() = !isLoading && loadErrorMessage == null && loans.isEmpty()

    val loanAccountOptions: List<LoanAccountOption>
        get() = accountOptions.filter { it.type == AccountType.LOAN_LINKED }

    val disbursedAccountOptions: List<LoanAccountOption>
        get() {
            val selectedCurrency =
                accountOptions.firstOrNull { it.id == editor?.loanAccountId }?.currencyCode
            return accountOptions.filter {
                it.type != AccountType.LOAN_LINKED &&
                    it.ledgerClass == LedgerClass.ASSET &&
                    (selectedCurrency == null || it.currencyCode == selectedCurrency)
            }
        }
}

data class LoanEditorUiState(
    val name: String = "",
    val principal: String = "",
    val annualInterestRatePercent: String = "",
    val tenureMonths: String = "",
    val startDate: String = "",
    val loanAccountId: String? = null,
    val disbursedAccountId: String? = null,
)

data class LoanAccountOption(
    val id: String,
    val name: String,
    val type: AccountType,
    val ledgerClass: LedgerClass,
    val currencyCode: String,
) {
    val label: String = "$name · $currencyCode"
}

data class LoanRowUiModel(
    val id: String,
    val name: String,
    val statusLabel: String,
    val principalLabel: String,
    val scheduledEmiLabel: String,
    val outstandingLabel: String,
    val remainingInstallmentsLabel: String,
    val nextDueDateLabel: String,
)

data class LoanDetailsUiState(
    val loanId: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val details: LoanDetailsUiModel? = null,
)

data class LoanDetailsUiModel(
    val id: String,
    val name: String,
    val statusLabel: String,
    val outstandingLabel: String,
    val scheduledEmiLabel: String,
    val totalInterestLabel: String,
    val totalPayableLabel: String,
    val principalLabel: String,
    val annualRateLabel: String,
    val tenureLabel: String,
    val startDateLabel: String,
    val loanAccountLabel: String,
    val disbursedAccountLabel: String,
    val remainingInstallmentsLabel: String,
    val nextDueDateLabel: String,
    val schedule: List<LoanInstallmentUiModel>,
)

data class LoanInstallmentUiModel(
    val id: String,
    val installmentNumber: Int,
    val dueDateLabel: String,
    val paymentLabel: String,
    val principalLabel: String,
    val interestLabel: String,
    val openingBalanceLabel: String,
    val closingBalanceLabel: String,
    val statusLabel: String,
)

private data class ParsedLoanEditor(
    val principalAmount: Long,
    val annualInterestRateBasisPoints: Int,
    val tenureMonths: Int,
    val startDate: LocalDate,
)

private fun LoanEditorUiState.parse(): ParsedLoanEditor? {
    if (name.isBlank()) return null
    val principalAmount = principal.toPositiveMinorUnitsOrNull() ?: return null
    val rate = annualInterestRatePercent.toBasisPointsOrNull() ?: return null
    val tenure = tenureMonths.trim().toIntOrNull()?.takeIf { it > 0 } ?: return null
    val date = runCatching { LocalDate.parse(startDate.trim()) }.getOrNull() ?: return null
    if (loanAccountId == null || disbursedAccountId == null) return null
    return ParsedLoanEditor(principalAmount, rate, tenure, date)
}

private fun LoanEditorUiState.validationMessage(): String =
    when {
        name.isBlank() -> "Enter a loan name."
        principal.toPositiveMinorUnitsOrNull() == null -> "Enter a valid positive principal with at most 2 decimal places."
        annualInterestRatePercent.toBasisPointsOrNull() == null ->
            "Enter a non-negative annual rate with at most 2 decimal places."
        tenureMonths.trim().toIntOrNull()?.takeIf { it > 0 } == null -> "Enter a positive tenure in months."
        runCatching { LocalDate.parse(startDate.trim()) }.getOrNull() == null -> "Enter the start date as YYYY-MM-DD."
        loanAccountId == null -> "Select a linked loan account."
        disbursedAccountId == null -> "Select a disbursed asset account."
        else -> "Review the loan details."
    }

private fun String.toPositiveMinorUnitsOrNull(): Long? {
    val normalized = trim().replace(",", "")
    if (normalized.isBlank()) return null
    if (normalized.count { it == '.' } > 1 || normalized.any { it != '.' && it !in '0'..'9' }) return null
    val parts = normalized.split('.')
    if (parts.size > 2) return null
    val majorPart = parts.firstOrNull().orEmpty().ifBlank { "0" }
    val fractionPart = parts.getOrNull(1).orEmpty()
    if (fractionPart.length > 2) return null
    val majorUnits = majorPart.toLongOrNull() ?: return null
    val fractionUnits =
        when (fractionPart.length) {
            0 -> 0L
            1 -> fractionPart.toLong() * 10L
            else -> fractionPart.toLong()
        }
    if (
        majorUnits > Long.MAX_VALUE / 100L ||
        (majorUnits == Long.MAX_VALUE / 100L && fractionUnits > Long.MAX_VALUE % 100L)
    ) {
        return null
    }
    return (majorUnits * 100L + fractionUnits).takeIf { it > 0L }
}

private fun String.toBasisPointsOrNull(): Int? {
    val normalized = trim().removeSuffix("%").trim()
    if (normalized.isBlank()) return null
    if (normalized.count { it == '.' } > 1 || normalized.any { it != '.' && it !in '0'..'9' }) return null
    val parts = normalized.split('.')
    if (parts.size > 2) return null
    val wholePart = parts.firstOrNull().orEmpty().ifBlank { "0" }
    val fractionPart = parts.getOrNull(1).orEmpty()
    if (fractionPart.length > 2) return null
    val whole = wholePart.toLongOrNull() ?: return null
    val fraction = fractionPart.padEnd(2, '0').ifBlank { "00" }.toLongOrNull() ?: return null
    if (whole > Int.MAX_VALUE / 100L) return null
    val basisPoints = whole * 100L + fraction
    return basisPoints.takeIf { it <= Int.MAX_VALUE }?.toInt()
}

private fun AccountsBalanceOverview.toAccountOptions(): List<LoanAccountOption> =
    accounts
        .map { it.account }
        .filter { !it.isArchived && it.deletedAt == null }
        .map(Account::toAccountOption)
        .sortedWith(compareBy<LoanAccountOption> { it.name.lowercase() }.thenBy { it.id })

private fun Account.toAccountOption(): LoanAccountOption =
    LoanAccountOption(
        id = id,
        name = name,
        type = type,
        ledgerClass = type.ledgerClass,
        currencyCode = currencyCode,
    )

private fun LoanOverview.toRowUiModel(): LoanRowUiModel =
    LoanRowUiModel(
        id = loan.id,
        name = loan.name,
        statusLabel = loan.status.name.toDisplayLabel(),
        principalLabel = loan.principal.toDisplayLabel(),
        scheduledEmiLabel = scheduledEmi.toDisplayLabel(),
        outstandingLabel = outstandingPrincipal.toDisplayLabel(),
        remainingInstallmentsLabel = "$remainingInstallments remaining",
        nextDueDateLabel = nextDueDate?.toUtcDateLabel() ?: "No remaining due date",
    )

private fun LoanDetailsView.toDetailsUiModel(accounts: List<Account>): LoanDetailsUiModel {
    val accountNames = accounts.associate { it.id to it.name }
    val loan = overview.loan
    return LoanDetailsUiModel(
        id = loan.id,
        name = loan.name,
        statusLabel = loan.status.name.toDisplayLabel(),
        outstandingLabel = overview.outstandingPrincipal.toDisplayLabel(),
        scheduledEmiLabel = overview.scheduledEmi.toDisplayLabel(),
        totalInterestLabel = overview.totalInterest.toDisplayLabel(),
        totalPayableLabel = overview.totalPayable.toDisplayLabel(),
        principalLabel = loan.principal.toDisplayLabel(),
        annualRateLabel = loan.annualInterestRateBasisPoints.toRateLabel(),
        tenureLabel = "${loan.tenureMonths} months",
        startDateLabel = loan.startDate.toUtcDateLabel(),
        loanAccountLabel = accountNames[loan.accountId] ?: "Unavailable account",
        disbursedAccountLabel = accountNames[loan.disbursedAccountId] ?: "Unavailable account",
        remainingInstallmentsLabel = overview.remainingInstallments.toString(),
        nextDueDateLabel = overview.nextDueDate?.toUtcDateLabel() ?: "None",
        schedule = schedule.map(LoanInstallment::toUiModel),
    )
}

private fun LoanInstallment.toUiModel(): LoanInstallmentUiModel =
    LoanInstallmentUiModel(
        id = id,
        installmentNumber = installmentNumber,
        dueDateLabel = dueDate.toUtcDateLabel(),
        paymentLabel = payment.toDisplayLabel(),
        principalLabel = principalComponent.toDisplayLabel(),
        interestLabel = interestComponent.toDisplayLabel(),
        openingBalanceLabel = openingBalance.toDisplayLabel(),
        closingBalanceLabel = closingBalance.toDisplayLabel(),
        statusLabel = status.name.toDisplayLabel(),
    )

private fun Money.toDisplayLabel(): String {
    val major = amount / 100L
    val fraction = (amount % 100L).let { if (it < 0L) -it else it }
    return "${currency.normalized} $major.${fraction.toString().padStart(2, '0')}"
}

private fun Int.toRateLabel(): String {
    val whole = this / 100
    val fraction = this % 100
    return if (fraction == 0) "$whole%" else "$whole.${fraction.toString().padStart(2, '0')}%"
}

private fun Long.toUtcDateLabel(): String =
    Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.UTC).date.toString()

private fun String.toDisplayLabel(): String =
    lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }

private fun ApplicationError.toLoadMessage(): String =
    when (this) {
        is ApplicationError.Validation -> reason
        is ApplicationError.Repository -> "Unable to load loans."
        is ApplicationError.Ledger -> "Unable to calculate loan summaries."
    }

private fun ApplicationError.toDetailsMessage(): String =
    when (this) {
        is ApplicationError.Validation -> reason
        is ApplicationError.Repository -> "Unable to load loan details."
        is ApplicationError.Ledger -> "Unable to summarize loan details."
    }
