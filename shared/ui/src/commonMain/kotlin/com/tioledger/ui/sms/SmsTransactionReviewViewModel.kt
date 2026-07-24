package com.tioledger.ui.sms

import com.tioledger.application.model.ApplicationError
import com.tioledger.application.model.ApplicationResult
import com.tioledger.application.usecase.sms.ConfirmSmsTransactionCommand
import com.tioledger.application.usecase.sms.ConfirmSmsTransactionUseCase
import com.tioledger.application.usecase.sms.PrepareSmsTransactionReviewCommand
import com.tioledger.application.usecase.sms.PrepareSmsTransactionReviewUseCase
import com.tioledger.application.usecase.sms.PreparedSmsTransactionReview
import com.tioledger.application.usecase.sms.SmsReviewAccountOption
import com.tioledger.application.usecase.sms.SmsReviewCategoryOption
import com.tioledger.application.usecase.sms.SmsReviewPreparation
import com.tioledger.domain.model.AccountType
import com.tioledger.domain.model.CategoryType
import com.tioledger.domain.model.SmsDetectedField
import com.tioledger.domain.model.SmsIgnoredReason
import com.tioledger.domain.model.SmsMissingField
import com.tioledger.domain.model.SmsParseConfidence
import com.tioledger.domain.model.SmsParseEvidence
import com.tioledger.domain.model.SmsPaymentRail
import com.tioledger.domain.model.SmsTransactionDirection
import com.tioledger.domain.model.SmsUnsupportedReason
import com.tioledger.domain.model.TransactionRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

enum class SmsReviewStage {
    INPUT,
    PARSING,
    DISABLED,
    IGNORED,
    UNSUPPORTED,
    REVIEW,
    SAVED,
    REJECTED,
}

enum class SmsReviewAccountPickerTarget {
    SOURCE,
    DESTINATION,
}

data class SmsReviewAccountUiModel(
    val id: String,
    val name: String,
    val type: AccountType,
    val currencyCode: String,
) {
    val subtitle: String
        get() = "${type.displayName()} • $currencyCode"
}

data class SmsReviewCategoryUiModel(
    val id: String,
    val name: String,
    val type: CategoryType,
) {
    val subtitle: String
        get() = type.name.lowercase()
}

data class SmsReviewEvidenceUiModel(
    val label: String,
    val value: String,
)

data class SmsTransactionReviewUiState(
    val stage: SmsReviewStage = SmsReviewStage.INPUT,
    val messageInput: String = "",
    val defaultCurrencyCode: String = "INR",
    val confidence: SmsParseConfidence? = null,
    val direction: SmsTransactionDirection? = null,
    val amount: String = "",
    val currencyCode: String = "",
    val occurredAt: Long = 0L,
    val selectedDate: String = "",
    val accountOptions: List<SmsReviewAccountUiModel> = emptyList(),
    val categoryOptions: List<SmsReviewCategoryUiModel> = emptyList(),
    val selectedAccountId: String? = null,
    val selectedDestinationAccountId: String? = null,
    val selectedCategoryId: String? = null,
    val activeAccountPicker: SmsReviewAccountPickerTarget? = null,
    val isCategoryPickerVisible: Boolean = false,
    val note: String = "",
    val paymentRail: SmsPaymentRail? = null,
    val evidence: List<SmsReviewEvidenceUiModel> = emptyList(),
    val unresolvedFields: List<SmsMissingField> = emptyList(),
    val statusMessage: String? = null,
    val validationErrorMessage: String? = null,
    val persistenceErrorMessage: String? = null,
    val isSaving: Boolean = false,
) {
    val visibleAccountOptions: List<SmsReviewAccountUiModel>
        get() =
            accountOptions.filter {
                currencyCode.isBlank() || it.currencyCode.equals(currencyCode, ignoreCase = true)
            }

    val visibleCategoryOptions: List<SmsReviewCategoryUiModel>
        get() =
            categoryOptions.filter { option ->
                when (direction) {
                    SmsTransactionDirection.INCOME -> option.type == CategoryType.INCOME
                    SmsTransactionDirection.EXPENSE -> option.type == CategoryType.EXPENSE
                    SmsTransactionDirection.TRANSFER_CANDIDATE, null -> false
                }
            }

    val selectedAccount: SmsReviewAccountUiModel?
        get() = visibleAccountOptions.firstOrNull { it.id == selectedAccountId }

    val selectedDestinationAccount: SmsReviewAccountUiModel?
        get() = visibleAccountOptions.firstOrNull { it.id == selectedDestinationAccountId }

    val selectedCategory: SmsReviewCategoryUiModel?
        get() = visibleCategoryOptions.firstOrNull { it.id == selectedCategoryId }

    val canParse: Boolean
        get() = stage == SmsReviewStage.INPUT && messageInput.isNotBlank()

    val canSave: Boolean
        get() =
            stage == SmsReviewStage.REVIEW &&
                !isSaving &&
                amount.toMinorUnits() != null &&
                currencyCode.isCurrencyCode() &&
                direction != null &&
                selectedAccountId != null &&
                when (direction) {
                    SmsTransactionDirection.INCOME, SmsTransactionDirection.EXPENSE -> selectedCategoryId != null
                    SmsTransactionDirection.TRANSFER_CANDIDATE ->
                        selectedDestinationAccountId != null &&
                            selectedDestinationAccountId != selectedAccountId
                    null -> false
                }
}

sealed interface SmsTransactionReviewAction {
    data class MessageChanged(val message: String) : SmsTransactionReviewAction

    data class DefaultCurrencyChanged(val currencyCode: String) : SmsTransactionReviewAction

    data object ParseClicked : SmsTransactionReviewAction

    data object TryAnotherMessage : SmsTransactionReviewAction

    data class DirectionChanged(val direction: SmsTransactionDirection) : SmsTransactionReviewAction

    data class AmountChanged(val amount: String) : SmsTransactionReviewAction

    data class CurrencyChanged(val currencyCode: String) : SmsTransactionReviewAction

    data object SourceAccountClicked : SmsTransactionReviewAction

    data object DestinationAccountClicked : SmsTransactionReviewAction

    data class AccountSelected(val accountId: String) : SmsTransactionReviewAction

    data object AccountPickerDismissed : SmsTransactionReviewAction

    data object CategoryClicked : SmsTransactionReviewAction

    data class CategorySelected(val categoryId: String) : SmsTransactionReviewAction

    data object CategoryPickerDismissed : SmsTransactionReviewAction

    data object DateClicked : SmsTransactionReviewAction

    data class DateSelected(
        val timestamp: Long,
        val label: String,
    ) : SmsTransactionReviewAction

    data class NoteChanged(val note: String) : SmsTransactionReviewAction

    data object SaveClicked : SmsTransactionReviewAction

    data object RejectClicked : SmsTransactionReviewAction

    data object MessageDismissed : SmsTransactionReviewAction

    data object EventConsumed : SmsTransactionReviewAction
}

sealed interface SmsTransactionReviewEvent {
    data class TransactionSaved(val transactionId: String) : SmsTransactionReviewEvent

    data object Rejected : SmsTransactionReviewEvent

    data class DateSelectionRequested(val currentTimestamp: Long) : SmsTransactionReviewEvent
}

internal typealias SmsReviewPreparer =
    (PrepareSmsTransactionReviewCommand) -> ApplicationResult<SmsReviewPreparation>
internal typealias SmsReviewConfirmer =
    (ConfirmSmsTransactionCommand) -> ApplicationResult<TransactionRecord>

class SmsTransactionReviewViewModel internal constructor(
    private val prepareReview: SmsReviewPreparer,
    private val confirmReview: SmsReviewConfirmer,
    private val nowProvider: () -> Long,
    private val timeZoneIdProvider: () -> String,
) {
    constructor(
        prepareUseCase: PrepareSmsTransactionReviewUseCase,
        confirmUseCase: ConfirmSmsTransactionUseCase,
        nowProvider: () -> Long = { Clock.System.now().toEpochMilliseconds() },
        timeZoneIdProvider: () -> String = { TimeZone.currentSystemDefault().id },
    ) : this(
        prepareReview = prepareUseCase::invoke,
        confirmReview = confirmUseCase::invoke,
        nowProvider = nowProvider,
        timeZoneIdProvider = timeZoneIdProvider,
    )

    private val _uiState = MutableStateFlow(SmsTransactionReviewUiState())
    val uiState: StateFlow<SmsTransactionReviewUiState> = _uiState.asStateFlow()

    private val _event = MutableStateFlow<SmsTransactionReviewEvent?>(null)
    val event: StateFlow<SmsTransactionReviewEvent?> = _event.asStateFlow()

    fun onAction(action: SmsTransactionReviewAction) {
        when (action) {
            is SmsTransactionReviewAction.MessageChanged -> updateMessage(action.message)
            is SmsTransactionReviewAction.DefaultCurrencyChanged -> updateDefaultCurrency(action.currencyCode)
            SmsTransactionReviewAction.ParseClicked -> parseMessage()
            SmsTransactionReviewAction.TryAnotherMessage -> resetForAnotherMessage()
            is SmsTransactionReviewAction.DirectionChanged -> updateDirection(action.direction)
            is SmsTransactionReviewAction.AmountChanged -> updateAmount(action.amount)
            is SmsTransactionReviewAction.CurrencyChanged -> updateCurrency(action.currencyCode)
            SmsTransactionReviewAction.SourceAccountClicked -> openAccountPicker(SmsReviewAccountPickerTarget.SOURCE)
            SmsTransactionReviewAction.DestinationAccountClicked ->
                openAccountPicker(SmsReviewAccountPickerTarget.DESTINATION)
            is SmsTransactionReviewAction.AccountSelected -> selectAccount(action.accountId)
            SmsTransactionReviewAction.AccountPickerDismissed ->
                _uiState.update { it.copy(activeAccountPicker = null) }
            SmsTransactionReviewAction.CategoryClicked ->
                _uiState.update {
                    it.copy(
                        isCategoryPickerVisible = true,
                        validationErrorMessage = null,
                        persistenceErrorMessage = null,
                    )
                }
            is SmsTransactionReviewAction.CategorySelected -> selectCategory(action.categoryId)
            SmsTransactionReviewAction.CategoryPickerDismissed ->
                _uiState.update { it.copy(isCategoryPickerVisible = false) }
            SmsTransactionReviewAction.DateClicked ->
                _event.value = SmsTransactionReviewEvent.DateSelectionRequested(_uiState.value.occurredAt)
            is SmsTransactionReviewAction.DateSelected -> selectDate(action.timestamp, action.label)
            is SmsTransactionReviewAction.NoteChanged -> updateNote(action.note)
            SmsTransactionReviewAction.SaveClicked -> saveTransaction()
            SmsTransactionReviewAction.RejectClicked -> rejectSuggestion()
            SmsTransactionReviewAction.MessageDismissed ->
                _uiState.update {
                    it.copy(
                        validationErrorMessage = null,
                        persistenceErrorMessage = null,
                    )
                }
            SmsTransactionReviewAction.EventConsumed -> _event.value = null
        }
    }

    private fun updateMessage(message: String) {
        _uiState.update {
            it.copy(
                messageInput = message,
                validationErrorMessage = null,
            )
        }
    }

    private fun updateDefaultCurrency(currencyCode: String) {
        _uiState.update {
            it.copy(
                defaultCurrencyCode = currencyCode.uppercase(),
                validationErrorMessage = null,
            )
        }
    }

    private fun parseMessage() {
        val state = _uiState.value
        val message = state.messageInput.trim()
        if (message.isBlank()) {
            _uiState.update { it.copy(validationErrorMessage = "Paste or enter a transaction message.") }
            return
        }
        val defaultCurrency = state.defaultCurrencyCode.trim().uppercase()
        if (defaultCurrency.isNotBlank() && !defaultCurrency.isCurrencyCode()) {
            _uiState.update { it.copy(validationErrorMessage = "Default currency must use three letters.") }
            return
        }

        val receivedAt = nowProvider()
        _uiState.update {
            it.copy(
                stage = SmsReviewStage.PARSING,
                validationErrorMessage = null,
                persistenceErrorMessage = null,
                statusMessage = null,
            )
        }

        when (
            val result =
                prepareReview(
                    PrepareSmsTransactionReviewCommand(
                        message = message,
                        receivedAt = receivedAt,
                        timeZoneId = timeZoneIdProvider(),
                        defaultCurrencyCode = defaultCurrency.ifBlank { null },
                    ),
                )
        ) {
            is ApplicationResult.Success -> handlePreparation(result.outcome.value, receivedAt)
            is ApplicationResult.Failure -> handlePreparationFailure(result.error)
        }
    }

    private fun handlePreparation(
        preparation: SmsReviewPreparation,
        receivedAt: Long,
    ) {
        when (preparation) {
            is SmsReviewPreparation.Review -> showReview(preparation.value, receivedAt)
            is SmsReviewPreparation.Ignored ->
                _uiState.update {
                    SmsTransactionReviewUiState(
                        stage = SmsReviewStage.IGNORED,
                        defaultCurrencyCode = it.defaultCurrencyCode,
                        statusMessage = preparation.reason.toMessage(),
                        evidence = preparation.evidence.toUiModels(),
                    )
                }
            is SmsReviewPreparation.Unsupported ->
                _uiState.update {
                    SmsTransactionReviewUiState(
                        stage = SmsReviewStage.UNSUPPORTED,
                        defaultCurrencyCode = it.defaultCurrencyCode,
                        statusMessage = preparation.reason.toMessage(),
                        unresolvedFields = preparation.missingFields,
                        evidence = preparation.evidence.toUiModels(),
                    )
                }
        }
    }

    private fun showReview(
        prepared: PreparedSmsTransactionReview,
        receivedAt: Long,
    ) {
        val suggestion = prepared.suggestion
        val occurredAt = suggestion.occurredAt ?: receivedAt
        val currencyCode = suggestion.currencyCode ?: _uiState.value.defaultCurrencyCode.trim().uppercase()
        _uiState.update { current ->
            SmsTransactionReviewUiState(
                stage = SmsReviewStage.REVIEW,
                defaultCurrencyCode = current.defaultCurrencyCode,
                confidence = suggestion.confidence,
                direction = suggestion.direction,
                amount = suggestion.amountMinorUnits?.toAmountText().orEmpty(),
                currencyCode = currencyCode,
                occurredAt = occurredAt,
                selectedDate = occurredAt.toDateLabel(timeZoneIdProvider()),
                accountOptions = prepared.accountOptions.map(SmsReviewAccountOption::toUiModel),
                categoryOptions = prepared.categoryOptions.map(SmsReviewCategoryOption::toUiModel),
                selectedAccountId = prepared.suggestedAccountId,
                note = suggestion.merchantOrCounterparty.orEmpty(),
                paymentRail = suggestion.paymentRail,
                evidence = suggestion.evidence.toUiModels(),
                unresolvedFields = prepared.unresolvedFields,
            ).normalizedSelections()
        }
    }

    private fun handlePreparationFailure(error: ApplicationError) {
        val disabledReason =
            (error as? ApplicationError.Validation)
                ?.takeIf { it.field == "featureFlag" }
                ?.reason
        _uiState.update { current ->
            current.copy(
                stage = if (disabledReason != null) SmsReviewStage.DISABLED else SmsReviewStage.INPUT,
                messageInput = if (disabledReason != null) "" else current.messageInput,
                statusMessage = disabledReason,
                validationErrorMessage =
                    if (disabledReason == null) {
                        error.toPreparationMessage()
                    } else {
                        null
                    },
            )
        }
    }

    private fun resetForAnotherMessage() {
        val defaultCurrency = _uiState.value.defaultCurrencyCode
        _uiState.value = SmsTransactionReviewUiState(defaultCurrencyCode = defaultCurrency)
        _event.value = null
    }

    private fun updateDirection(direction: SmsTransactionDirection) {
        _uiState.update { current ->
            current.copy(
                direction = direction,
                selectedCategoryId =
                    current.selectedCategoryId.takeIf { categoryId ->
                        current.categoryOptions.any {
                            it.id == categoryId && it.type.matches(direction)
                        }
                    },
                selectedDestinationAccountId =
                    current.selectedDestinationAccountId.takeIf {
                        direction == SmsTransactionDirection.TRANSFER_CANDIDATE
                    },
                validationErrorMessage = null,
                persistenceErrorMessage = null,
            ).normalizedSelections()
        }
    }

    private fun updateAmount(amount: String) {
        _uiState.update {
            it.copy(
                amount = amount,
                validationErrorMessage = null,
                persistenceErrorMessage = null,
            )
        }
    }

    private fun updateCurrency(currencyCode: String) {
        _uiState.update { current ->
            current.copy(
                currencyCode = currencyCode.uppercase(),
                selectedAccountId = null,
                selectedDestinationAccountId = null,
                validationErrorMessage = null,
                persistenceErrorMessage = null,
            )
        }
    }

    private fun openAccountPicker(target: SmsReviewAccountPickerTarget) {
        _uiState.update {
            it.copy(
                activeAccountPicker = target,
                validationErrorMessage = null,
                persistenceErrorMessage = null,
            )
        }
    }

    private fun selectAccount(accountId: String) {
        _uiState.update { current ->
            when (current.activeAccountPicker) {
                SmsReviewAccountPickerTarget.SOURCE ->
                    current.copy(
                        selectedAccountId = accountId,
                        activeAccountPicker = null,
                        validationErrorMessage = null,
                        persistenceErrorMessage = null,
                    ).normalizedSelections()
                SmsReviewAccountPickerTarget.DESTINATION ->
                    current.copy(
                        selectedDestinationAccountId = accountId,
                        activeAccountPicker = null,
                        validationErrorMessage = null,
                        persistenceErrorMessage = null,
                    ).normalizedSelections()
                null -> current
            }
        }
    }

    private fun selectCategory(categoryId: String) {
        _uiState.update {
            it.copy(
                selectedCategoryId = categoryId,
                isCategoryPickerVisible = false,
                validationErrorMessage = null,
                persistenceErrorMessage = null,
            )
        }
    }

    private fun selectDate(
        timestamp: Long,
        label: String,
    ) {
        if (timestamp < 0L || label.isBlank()) return
        _uiState.update {
            it.copy(
                occurredAt = timestamp,
                selectedDate = label,
                validationErrorMessage = null,
                persistenceErrorMessage = null,
            )
        }
        _event.value = null
    }

    private fun updateNote(note: String) {
        _uiState.update {
            it.copy(
                note = note,
                validationErrorMessage = null,
                persistenceErrorMessage = null,
            )
        }
    }

    private fun saveTransaction() {
        val state = _uiState.value
        val amountMinorUnits = state.amount.toMinorUnits()
        validate(state, amountMinorUnits)?.let { message ->
            _uiState.update {
                it.copy(
                    validationErrorMessage = message,
                    persistenceErrorMessage = null,
                )
            }
            return
        }

        val direction = requireNotNull(state.direction)
        val accountId = requireNotNull(state.selectedAccountId)
        val amount = requireNotNull(amountMinorUnits)
        _uiState.update {
            it.copy(
                isSaving = true,
                validationErrorMessage = null,
                persistenceErrorMessage = null,
            )
        }

        val result =
            confirmReview(
                ConfirmSmsTransactionCommand(
                    direction = direction,
                    amountMinorUnits = amount,
                    currencyCode = state.currencyCode.trim().uppercase(),
                    accountId = accountId,
                    categoryId = state.selectedCategoryId,
                    destinationAccountId = state.selectedDestinationAccountId,
                    occurredAt = state.occurredAt,
                    description = state.note.trim().ifBlank { null },
                    confirmedAt = nowProvider(),
                    userConfirmed = true,
                ),
            )

        when (result) {
            is ApplicationResult.Success -> {
                _uiState.update { it.copy(stage = SmsReviewStage.SAVED, isSaving = false) }
                _event.value = SmsTransactionReviewEvent.TransactionSaved(result.outcome.value.transaction.id)
            }
            is ApplicationResult.Failure ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        persistenceErrorMessage = result.error.toConfirmationMessage(),
                    )
                }
        }
    }

    private fun rejectSuggestion() {
        _uiState.update { it.copy(stage = SmsReviewStage.REJECTED, isSaving = false) }
        _event.value = SmsTransactionReviewEvent.Rejected
    }

    private fun validate(
        state: SmsTransactionReviewUiState,
        amountMinorUnits: Long?,
    ): String? {
        if (state.stage != SmsReviewStage.REVIEW) return "Prepare a transaction suggestion first."
        if (state.direction == null) return "Select a transaction type."
        if (amountMinorUnits == null) return "Enter a positive amount with at most two decimal places."
        if (!state.currencyCode.isCurrencyCode()) return "Currency must use three letters."
        if (state.selectedAccountId == null) return "Select an account."
        if (
            state.direction != SmsTransactionDirection.TRANSFER_CANDIDATE &&
            state.selectedCategoryId == null
        ) {
            return "Select a category."
        }
        if (
            state.direction == SmsTransactionDirection.TRANSFER_CANDIDATE &&
            state.selectedDestinationAccountId == null
        ) {
            return "Select a destination account."
        }
        if (state.selectedDestinationAccountId == state.selectedAccountId) {
            return "Source and destination accounts must be different."
        }
        return null
    }
}

private fun SmsTransactionReviewUiState.normalizedSelections(): SmsTransactionReviewUiState {
    val validAccountIds = visibleAccountOptions.map { it.id }.toSet()
    val validCategoryIds = visibleCategoryOptions.map { it.id }.toSet()
    return copy(
        selectedAccountId = selectedAccountId.takeIf { it != null && it in validAccountIds },
        selectedDestinationAccountId =
            selectedDestinationAccountId
                .takeIf { it != null && it in validAccountIds }
                .takeIf { direction == SmsTransactionDirection.TRANSFER_CANDIDATE },
        selectedCategoryId =
            selectedCategoryId
                .takeIf { it != null && it in validCategoryIds }
                .takeIf { direction != SmsTransactionDirection.TRANSFER_CANDIDATE },
    )
}

private fun CategoryType.matches(direction: SmsTransactionDirection): Boolean =
    when (direction) {
        SmsTransactionDirection.INCOME -> this == CategoryType.INCOME
        SmsTransactionDirection.EXPENSE -> this == CategoryType.EXPENSE
        SmsTransactionDirection.TRANSFER_CANDIDATE -> false
    }

private fun SmsReviewAccountOption.toUiModel(): SmsReviewAccountUiModel =
    SmsReviewAccountUiModel(
        id = id,
        name = name,
        type = type,
        currencyCode = currencyCode,
    )

private fun SmsReviewCategoryOption.toUiModel(): SmsReviewCategoryUiModel =
    SmsReviewCategoryUiModel(
        id = id,
        name = name,
        type = type,
    )

private fun List<SmsParseEvidence>.toUiModels(): List<SmsReviewEvidenceUiModel> =
    map {
        SmsReviewEvidenceUiModel(
            label = it.field.displayName(),
            value = it.value,
        )
    }

private fun SmsDetectedField.displayName(): String =
    when (this) {
        SmsDetectedField.AMOUNT -> "Amount"
        SmsDetectedField.CURRENCY -> "Currency"
        SmsDetectedField.DIRECTION -> "Direction"
        SmsDetectedField.TRANSACTION_TIME -> "Transaction time"
        SmsDetectedField.ACCOUNT_HINT -> "Account hint"
        SmsDetectedField.MERCHANT_OR_COUNTERPARTY -> "Merchant or counterparty"
        SmsDetectedField.PAYMENT_RAIL -> "Payment rail"
    }

private fun AccountType.displayName(): String =
    when (this) {
        AccountType.CASH -> "Cash"
        AccountType.BANK -> "Bank"
        AccountType.CREDIT_CARD -> "Credit card"
        AccountType.WALLET -> "Wallet"
        AccountType.LOAN_LINKED -> "Loan"
        AccountType.INVESTMENT -> "Investment"
    }

private fun SmsIgnoredReason.toMessage(): String =
    when (this) {
        SmsIgnoredReason.OTP_OR_SECURITY_CODE -> "This appears to be an OTP or security message, not a transaction."
        SmsIgnoredReason.PROMOTIONAL -> "This appears to be a promotional message, not a transaction."
        SmsIgnoredReason.FAILED_OR_DECLINED_TRANSACTION -> "The message describes a failed or declined transaction."
        SmsIgnoredReason.BALANCE_ONLY -> "The message contains balance information without a transaction."
    }

private fun SmsUnsupportedReason.toMessage(): String =
    when (this) {
        SmsUnsupportedReason.EMPTY_MESSAGE -> "Enter a message before preparing a review."
        SmsUnsupportedReason.INSUFFICIENT_EVIDENCE -> "The message does not contain enough transaction evidence."
        SmsUnsupportedReason.UNSUPPORTED_FORMAT -> "This transaction message format is not supported yet."
    }

private fun ApplicationError.toPreparationMessage(): String =
    when (this) {
        is ApplicationError.Validation -> "$field: $reason"
        is ApplicationError.Repository -> "Unable to load accounts or categories for review."
        is ApplicationError.Ledger -> "Unable to prepare this transaction review."
    }

private fun ApplicationError.toConfirmationMessage(): String =
    when (this) {
        is ApplicationError.Validation -> "$field: $reason"
        is ApplicationError.Repository -> "Unable to save the confirmed transaction."
        is ApplicationError.Ledger -> "The confirmed transaction could not be posted."
    }

private fun Long.toAmountText(): String {
    val major = this / MINOR_UNITS
    val minor = this % MINOR_UNITS
    return "$major.${minor.toString().padStart(2, '0')}"
}

private fun Long.toDateLabel(timeZoneId: String): String {
    val timeZone = runCatching { TimeZone.of(timeZoneId) }.getOrElse { TimeZone.UTC }
    return Instant.fromEpochMilliseconds(this).toLocalDateTime(timeZone).date.toString()
}

private fun String.toMinorUnits(): Long? {
    val normalized = trim().replace(",", "")
    if (normalized.isBlank()) return null
    if (normalized.count { it == '.' } > 1) return null
    if (normalized.any { it != '.' && it !in '0'..'9' }) return null
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
    if (majorUnits > Long.MAX_VALUE / MINOR_UNITS) return null
    if (
        majorUnits == Long.MAX_VALUE / MINOR_UNITS &&
        fractionUnits > Long.MAX_VALUE % MINOR_UNITS
    ) {
        return null
    }
    return (majorUnits * MINOR_UNITS + fractionUnits).takeIf { it > 0L }
}

private fun String.isCurrencyCode(): Boolean =
    trim().let { value ->
        value.length == 3 && value.all { it.uppercaseChar() in 'A'..'Z' }
    }

private const val MINOR_UNITS = 100L
