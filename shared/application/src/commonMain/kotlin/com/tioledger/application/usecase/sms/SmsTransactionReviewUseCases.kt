package com.tioledger.application.usecase.sms

import com.tioledger.application.model.ApplicationError
import com.tioledger.application.model.ApplicationResult
import com.tioledger.application.model.UseCaseOutcome
import com.tioledger.application.usecase.transaction.RecordExpenseCommand
import com.tioledger.application.usecase.transaction.RecordExpenseUseCase
import com.tioledger.application.usecase.transaction.RecordIncomeCommand
import com.tioledger.application.usecase.transaction.RecordIncomeUseCase
import com.tioledger.application.usecase.transaction.RecordTransferCommand
import com.tioledger.application.usecase.transaction.RecordTransferUseCase
import com.tioledger.core.feature.FeatureFlag
import com.tioledger.core.feature.FeatureFlagProvider
import com.tioledger.core.model.CurrencyCode
import com.tioledger.core.model.LedgerResult
import com.tioledger.core.model.Money
import com.tioledger.domain.model.Account
import com.tioledger.domain.model.AccountType
import com.tioledger.domain.model.Category
import com.tioledger.domain.model.CategoryType
import com.tioledger.domain.model.SmsIgnoredReason
import com.tioledger.domain.model.SmsMissingField
import com.tioledger.domain.model.SmsParseEvidence
import com.tioledger.domain.model.SmsParseRequest
import com.tioledger.domain.model.SmsParseResult
import com.tioledger.domain.model.SmsTransactionDirection
import com.tioledger.domain.model.SmsTransactionParser
import com.tioledger.domain.model.SmsTransactionSuggestion
import com.tioledger.domain.model.SmsUnsupportedReason
import com.tioledger.domain.model.TransactionRecord
import com.tioledger.domain.repository.AccountRepository
import com.tioledger.domain.repository.CategoryRepository

data class PrepareSmsTransactionReviewCommand(
    val message: String,
    val receivedAt: Long,
    val timeZoneId: String,
    val defaultCurrencyCode: String? = null,
)

data class SmsReviewAccountOption(
    val id: String,
    val name: String,
    val type: AccountType,
    val currencyCode: String,
)

data class SmsReviewCategoryOption(
    val id: String,
    val name: String,
    val type: CategoryType,
)

data class PreparedSmsTransactionReview(
    val suggestion: SmsTransactionSuggestion,
    val accountOptions: List<SmsReviewAccountOption>,
    val categoryOptions: List<SmsReviewCategoryOption>,
    val suggestedAccountId: String?,
    val unresolvedFields: List<SmsMissingField>,
)

sealed interface SmsReviewPreparation {
    data class Review(
        val value: PreparedSmsTransactionReview,
    ) : SmsReviewPreparation

    data class Ignored(
        val reason: SmsIgnoredReason,
        val evidence: List<SmsParseEvidence>,
    ) : SmsReviewPreparation

    data class Unsupported(
        val reason: SmsUnsupportedReason,
        val missingFields: List<SmsMissingField>,
        val evidence: List<SmsParseEvidence>,
    ) : SmsReviewPreparation
}

class PrepareSmsTransactionReviewUseCase(
    private val featureFlagProvider: FeatureFlagProvider,
    private val parser: SmsTransactionParser,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
) {
    operator fun invoke(command: PrepareSmsTransactionReviewCommand): ApplicationResult<SmsReviewPreparation> {
        featureDisabledFailure()?.let { return it }
        validatePrepareCommand(command)?.let { return ApplicationResult.Failure(it) }

        return when (
            val result =
                parser.parse(
                    SmsParseRequest(
                        message = command.message,
                        receivedAt = command.receivedAt,
                        timeZoneId = command.timeZoneId.trim(),
                        defaultCurrencyCode = command.defaultCurrencyCode?.trim(),
                    ),
                )
        ) {
            is SmsParseResult.Ignored ->
                success(
                    SmsReviewPreparation.Ignored(
                        reason = result.reason,
                        evidence = result.evidence,
                    ),
                )
            is SmsParseResult.Unsupported ->
                success(
                    SmsReviewPreparation.Unsupported(
                        reason = result.reason,
                        missingFields = result.missingFields,
                        evidence = result.evidence,
                    ),
                )
            is SmsParseResult.Suggestion -> prepareReview(result.value)
        }
    }

    private fun prepareReview(suggestion: SmsTransactionSuggestion): ApplicationResult<SmsReviewPreparation> {
        val accounts =
            when (val result = accountRepository.findAll(includeArchived = false)) {
                is LedgerResult.Success -> result.value
                is LedgerResult.Failure -> return ApplicationResult.Failure(ApplicationError.Repository(result.error))
            }
        val categories =
            when (val result = categoryRepository.findAll()) {
                is LedgerResult.Success -> result.value
                is LedgerResult.Failure -> return ApplicationResult.Failure(ApplicationError.Repository(result.error))
            }

        val eligibleAccounts = accounts.eligibleFor(suggestion)
        val eligibleCategories = categories.eligibleFor(suggestion.direction)
        val suggestedAccountId = findExactAccountMatch(eligibleAccounts, suggestion.accountHint)
        val unresolvedFields =
            suggestion.missingFields.filterNot {
                it == SmsMissingField.ACCOUNT && suggestedAccountId != null
            }

        return success(
            SmsReviewPreparation.Review(
                PreparedSmsTransactionReview(
                    suggestion = suggestion,
                    accountOptions = eligibleAccounts.map(Account::toReviewOption),
                    categoryOptions = eligibleCategories.map(Category::toReviewOption),
                    suggestedAccountId = suggestedAccountId,
                    unresolvedFields = unresolvedFields,
                ),
            ),
        )
    }

    private fun featureDisabledFailure(): ApplicationResult.Failure? =
        if (featureFlagProvider.isEnabled(FeatureFlag.SMS_ASSISTED_TRANSACTION_REVIEW)) {
            null
        } else {
            ApplicationResult.Failure(
                ApplicationError.Validation(
                    field = "featureFlag",
                    reason = "SMS-assisted transaction review is disabled.",
                ),
            )
        }
}

data class ConfirmSmsTransactionCommand(
    val direction: SmsTransactionDirection,
    val amountMinorUnits: Long,
    val currencyCode: String,
    val accountId: String,
    val categoryId: String?,
    val destinationAccountId: String?,
    val occurredAt: Long,
    val description: String?,
    val confirmedAt: Long,
    val userConfirmed: Boolean,
)

internal typealias IncomeRecorder =
    (RecordIncomeCommand) -> ApplicationResult<TransactionRecord>
internal typealias ExpenseRecorder =
    (RecordExpenseCommand) -> ApplicationResult<TransactionRecord>
internal typealias TransferRecorder =
    (RecordTransferCommand) -> ApplicationResult<TransactionRecord>

class ConfirmSmsTransactionUseCase internal constructor(
    private val featureFlagProvider: FeatureFlagProvider,
    private val recordIncome: IncomeRecorder,
    private val recordExpense: ExpenseRecorder,
    private val recordTransfer: TransferRecorder,
) {
    constructor(
        featureFlagProvider: FeatureFlagProvider,
        recordIncomeUseCase: RecordIncomeUseCase,
        recordExpenseUseCase: RecordExpenseUseCase,
        recordTransferUseCase: RecordTransferUseCase,
    ) : this(
        featureFlagProvider = featureFlagProvider,
        recordIncome = recordIncomeUseCase::invoke,
        recordExpense = recordExpenseUseCase::invoke,
        recordTransfer = recordTransferUseCase::invoke,
    )

    operator fun invoke(command: ConfirmSmsTransactionCommand): ApplicationResult<TransactionRecord> {
        featureDisabledFailure()?.let { return it }
        validateConfirmation(command)?.let { return ApplicationResult.Failure(it) }

        val money =
            Money(
                amount = command.amountMinorUnits,
                currency = CurrencyCode(command.currencyCode.trim()),
            )
        val description = command.description?.trim()?.ifBlank { null }

        return when (command.direction) {
            SmsTransactionDirection.INCOME ->
                recordIncome(
                    RecordIncomeCommand(
                        timestamp = command.occurredAt,
                        description = description,
                        amount = money,
                        accountId = command.accountId.trim(),
                        categoryId = command.categoryId?.trim(),
                        merchantId = null,
                        createdAt = command.confirmedAt,
                    ),
                )
            SmsTransactionDirection.EXPENSE ->
                recordExpense(
                    RecordExpenseCommand(
                        timestamp = command.occurredAt,
                        description = description,
                        amount = money,
                        accountId = command.accountId.trim(),
                        categoryId = command.categoryId?.trim(),
                        merchantId = null,
                        createdAt = command.confirmedAt,
                    ),
                )
            SmsTransactionDirection.TRANSFER_CANDIDATE ->
                recordTransfer(
                    RecordTransferCommand(
                        timestamp = command.occurredAt,
                        description = description,
                        amount = money,
                        sourceAccountId = command.accountId.trim(),
                        targetAccountId = requireNotNull(command.destinationAccountId).trim(),
                        createdAt = command.confirmedAt,
                    ),
                )
        }
    }

    private fun featureDisabledFailure(): ApplicationResult.Failure? =
        if (featureFlagProvider.isEnabled(FeatureFlag.SMS_ASSISTED_TRANSACTION_REVIEW)) {
            null
        } else {
            ApplicationResult.Failure(
                ApplicationError.Validation(
                    field = "featureFlag",
                    reason = "SMS-assisted transaction review is disabled.",
                ),
            )
        }
}

private fun validatePrepareCommand(command: PrepareSmsTransactionReviewCommand): ApplicationError.Validation? =
    when {
        command.receivedAt < 0L -> ApplicationError.Validation("receivedAt", "must be non-negative")
        command.timeZoneId.isBlank() -> ApplicationError.Validation("timeZoneId", "must not be blank")
        command.defaultCurrencyCode != null && !command.defaultCurrencyCode.isCurrencyCode() ->
            ApplicationError.Validation("defaultCurrencyCode", "must be a valid three-letter currency code")
        else -> null
    }

private fun validateConfirmation(command: ConfirmSmsTransactionCommand): ApplicationError.Validation? {
    if (!command.userConfirmed) {
        return ApplicationError.Validation("userConfirmed", "explicit confirmation is required")
    }
    if (command.amountMinorUnits <= 0L) {
        return ApplicationError.Validation("amountMinorUnits", "must be positive")
    }
    if (!command.currencyCode.isCurrencyCode()) {
        return ApplicationError.Validation("currencyCode", "must be a valid three-letter currency code")
    }
    if (command.accountId.isBlank()) {
        return ApplicationError.Validation("accountId", "must not be blank")
    }
    if (command.occurredAt < 0L) {
        return ApplicationError.Validation("occurredAt", "must be non-negative")
    }
    if (command.confirmedAt < 0L) {
        return ApplicationError.Validation("confirmedAt", "must be non-negative")
    }

    return when (command.direction) {
        SmsTransactionDirection.INCOME, SmsTransactionDirection.EXPENSE ->
            if (command.categoryId.isNullOrBlank()) {
                ApplicationError.Validation("categoryId", "must be selected before confirmation")
            } else {
                null
            }
        SmsTransactionDirection.TRANSFER_CANDIDATE ->
            when {
                command.destinationAccountId.isNullOrBlank() ->
                    ApplicationError.Validation("destinationAccountId", "must be selected before confirmation")
                command.accountId.trim() == command.destinationAccountId.trim() ->
                    ApplicationError.Validation("destinationAccountId", "must differ from the source account")
                else -> null
            }
    }
}

private fun String.isCurrencyCode(): Boolean =
    trim().let { value ->
        value.length == 3 && value.all { it.uppercaseChar() in 'A'..'Z' }
    }

private fun List<Account>.eligibleFor(suggestion: SmsTransactionSuggestion): List<Account> =
    asSequence()
        .filter { !it.isArchived && it.deletedAt == null }
        .filter { suggestion.currencyCode == null || it.currencyCode.equals(suggestion.currencyCode, ignoreCase = true) }
        .sortedWith(compareBy<Account> { it.displayOrder }.thenBy { it.name }.thenBy { it.id })
        .toList()

private fun List<Category>.eligibleFor(direction: SmsTransactionDirection?): List<Category> {
    val requiredType =
        when (direction) {
            SmsTransactionDirection.INCOME -> CategoryType.INCOME
            SmsTransactionDirection.EXPENSE -> CategoryType.EXPENSE
            SmsTransactionDirection.TRANSFER_CANDIDATE, null -> null
        }
    return asSequence()
        .filter { it.deletedAt == null }
        .filter { requiredType != null && it.type == requiredType }
        .sortedWith(compareBy<Category> { !it.isDefault }.thenBy { it.name }.thenBy { it.id })
        .toList()
}

private fun findExactAccountMatch(
    accounts: List<Account>,
    accountHint: String?,
): String? {
    val normalizedHint = accountHint?.normalizedMatchToken()?.takeIf { it.length >= 3 } ?: return null
    val matches =
        accounts.filter { account ->
            account.id.normalizedMatchToken().endsWith(normalizedHint) ||
                account.name.normalizedMatchToken().endsWith(normalizedHint)
        }
    return matches.singleOrNull()?.id
}

private fun String.normalizedMatchToken(): String =
    lowercase().filter { it.isLetterOrDigit() }

private fun Account.toReviewOption(): SmsReviewAccountOption =
    SmsReviewAccountOption(
        id = id,
        name = name,
        type = type,
        currencyCode = currencyCode,
    )

private fun Category.toReviewOption(): SmsReviewCategoryOption =
    SmsReviewCategoryOption(
        id = id,
        name = name,
        type = type,
    )

private fun <T> success(value: T): ApplicationResult.Success<T> =
    ApplicationResult.Success(UseCaseOutcome(value))
