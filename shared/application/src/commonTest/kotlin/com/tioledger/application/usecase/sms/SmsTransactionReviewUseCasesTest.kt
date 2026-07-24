package com.tioledger.application.usecase.sms

import com.tioledger.application.model.ApplicationError
import com.tioledger.application.model.ApplicationResult
import com.tioledger.application.usecase.transaction.RecordExpenseCommand
import com.tioledger.application.usecase.transaction.RecordIncomeCommand
import com.tioledger.application.usecase.transaction.RecordTransferCommand
import com.tioledger.core.feature.FeatureFlag
import com.tioledger.core.feature.StaticFeatureFlagProvider
import com.tioledger.core.model.LedgerError
import com.tioledger.core.model.LedgerResult
import com.tioledger.domain.model.Account
import com.tioledger.domain.model.AccountType
import com.tioledger.domain.model.Category
import com.tioledger.domain.model.CategoryType
import com.tioledger.domain.model.SmsIgnoredReason
import com.tioledger.domain.model.SmsMissingField
import com.tioledger.domain.model.SmsParseConfidence
import com.tioledger.domain.model.SmsParseResult
import com.tioledger.domain.model.SmsTransactionDirection
import com.tioledger.domain.model.SmsTransactionParser
import com.tioledger.domain.model.SmsTransactionSuggestion
import com.tioledger.domain.model.TransactionRecord
import com.tioledger.domain.repository.AccountRepository
import com.tioledger.domain.repository.CategoryRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SmsTransactionReviewUseCasesTest {
    @Test
    fun disabledFeatureRejectsPreparationBeforeParserInvocation() {
        var parserInvoked = false
        val useCase =
            PrepareSmsTransactionReviewUseCase(
                featureFlagProvider = StaticFeatureFlagProvider(),
                parser =
                    SmsTransactionParser {
                        parserInvoked = true
                        SmsParseResult.Ignored(SmsIgnoredReason.PROMOTIONAL)
                    },
                accountRepository = FakeAccountRepository(),
                categoryRepository = FakeCategoryRepository(),
            )

        val result = useCase(prepareCommand())

        assertFeatureDisabled(result)
        assertFalse(parserInvoked)
    }

    @Test
    fun ignoredMessageDoesNotLoadReferenceData() {
        val accountRepository = FakeAccountRepository()
        val categoryRepository = FakeCategoryRepository()
        val useCase =
            PrepareSmsTransactionReviewUseCase(
                featureFlagProvider = enabledFlags(),
                parser =
                    SmsTransactionParser {
                        SmsParseResult.Ignored(SmsIgnoredReason.OTP_OR_SECURITY_CODE)
                    },
                accountRepository = accountRepository,
                categoryRepository = categoryRepository,
            )

        val preparation = useCase(prepareCommand()).successValue()

        assertEquals(
            SmsIgnoredReason.OTP_OR_SECURITY_CODE,
            assertIs<SmsReviewPreparation.Ignored>(preparation).reason,
        )
        assertEquals(0, accountRepository.findAllCalls)
        assertEquals(0, categoryRepository.findAllCalls)
    }

    @Test
    fun suggestionLoadsEligibleOptionsAndSelectsOnlyExactAccountHintMatch() {
        val accounts =
            listOf(
                account(id = "bank-1234", name = "Everyday 1234", currency = "INR"),
                account(id = "bank-9999", name = "Savings 9999", currency = "INR"),
                account(id = "usd-1234", name = "Dollar 1234", currency = "USD"),
                account(id = "archived-1234", name = "Old 1234", currency = "INR", archived = true),
            )
        val categories =
            listOf(
                category(id = "expense-food", name = "Food", type = CategoryType.EXPENSE, isDefault = true),
                category(id = "expense-travel", name = "Travel", type = CategoryType.EXPENSE),
                category(id = "income-salary", name = "Salary", type = CategoryType.INCOME),
            )
        val useCase = prepareUseCase(accounts = accounts, categories = categories, suggestion = expenseSuggestion())

        val review = assertIs<SmsReviewPreparation.Review>(useCase(prepareCommand()).successValue()).value

        assertEquals(listOf("bank-1234", "bank-9999"), review.accountOptions.map { it.id })
        assertEquals(listOf("expense-food", "expense-travel"), review.categoryOptions.map { it.id })
        assertEquals("bank-1234", review.suggestedAccountId)
        assertFalse(SmsMissingField.ACCOUNT in review.unresolvedFields)
        assertTrue(SmsMissingField.CATEGORY in review.unresolvedFields)
    }

    @Test
    fun ambiguousAccountHintNeverSelectsAnAccount() {
        val accounts =
            listOf(
                account(id = "bank-a", name = "Primary 1234", currency = "INR"),
                account(id = "bank-b", name = "Secondary 1234", currency = "INR"),
            )
        val useCase = prepareUseCase(accounts = accounts, suggestion = expenseSuggestion())

        val review = assertIs<SmsReviewPreparation.Review>(useCase(prepareCommand()).successValue()).value

        assertNull(review.suggestedAccountId)
        assertTrue(SmsMissingField.ACCOUNT in review.unresolvedFields)
    }

    @Test
    fun repositoryFailureIsReturnedWithoutPreparingAReview() {
        val useCase =
            PrepareSmsTransactionReviewUseCase(
                featureFlagProvider = enabledFlags(),
                parser = SmsTransactionParser { SmsParseResult.Suggestion(expenseSuggestion()) },
                accountRepository =
                    FakeAccountRepository(
                        findAllResult = LedgerResult.Failure(LedgerError.StorageUnavailable),
                    ),
                categoryRepository = FakeCategoryRepository(),
            )

        val failure = assertIs<ApplicationResult.Failure>(useCase(prepareCommand()))

        assertEquals(
            ApplicationError.Repository(LedgerError.StorageUnavailable),
            failure.error,
        )
    }

    @Test
    fun confirmationRequiresExplicitUserActionBeforeAnyRecorderRuns() {
        val recorder = CapturingRecorders()
        val useCase = recorder.useCase(featureFlagProvider = enabledFlags())

        val result = useCase(confirmCommand(userConfirmed = false))

        val validation = assertIs<ApplicationError.Validation>(assertIs<ApplicationResult.Failure>(result).error)
        assertEquals("userConfirmed", validation.field)
        assertEquals(0, recorder.totalCalls)
    }

    @Test
    fun disabledFeatureRejectsConfirmationBeforeAnyRecorderRuns() {
        val recorder = CapturingRecorders()
        val useCase = recorder.useCase(featureFlagProvider = StaticFeatureFlagProvider())

        val result = useCase(confirmCommand())

        assertFeatureDisabled(result)
        assertEquals(0, recorder.totalCalls)
    }

    @Test
    fun confirmedExpenseUsesExistingExpenseRecordingPath() {
        val recorder = CapturingRecorders()
        val useCase = recorder.useCase(featureFlagProvider = enabledFlags())

        val result =
            useCase(
                confirmCommand(
                    direction = SmsTransactionDirection.EXPENSE,
                    description = " Grocery Mart ",
                ),
            )

        assertEquals(recorder.result, result)
        val command = requireNotNull(recorder.expenseCommand)
        assertEquals(125_050L, command.amount.amount)
        assertEquals("INR", command.amount.currency.toString())
        assertEquals("account-1", command.accountId)
        assertEquals("category-1", command.categoryId)
        assertEquals("Grocery Mart", command.description)
        assertEquals(1, recorder.totalCalls)
    }

    @Test
    fun confirmedIncomeUsesExistingIncomeRecordingPath() {
        val recorder = CapturingRecorders()
        val useCase = recorder.useCase(featureFlagProvider = enabledFlags())

        useCase(confirmCommand(direction = SmsTransactionDirection.INCOME))

        assertEquals("account-1", requireNotNull(recorder.incomeCommand).accountId)
        assertEquals(1, recorder.totalCalls)
    }

    @Test
    fun confirmedTransferRequiresAndUsesDistinctDestinationAccount() {
        val recorder = CapturingRecorders()
        val useCase = recorder.useCase(featureFlagProvider = enabledFlags())

        useCase(
            confirmCommand(
                direction = SmsTransactionDirection.TRANSFER_CANDIDATE,
                categoryId = null,
                destinationAccountId = "account-2",
            ),
        )

        val command = requireNotNull(recorder.transferCommand)
        assertEquals("account-1", command.sourceAccountId)
        assertEquals("account-2", command.targetAccountId)
        assertEquals(1, recorder.totalCalls)
    }

    @Test
    fun transferWithSameSourceAndDestinationIsRejected() {
        val recorder = CapturingRecorders()
        val useCase = recorder.useCase(featureFlagProvider = enabledFlags())

        val result =
            useCase(
                confirmCommand(
                    direction = SmsTransactionDirection.TRANSFER_CANDIDATE,
                    categoryId = null,
                    destinationAccountId = "account-1",
                ),
            )

        val validation = assertIs<ApplicationError.Validation>(assertIs<ApplicationResult.Failure>(result).error)
        assertEquals("destinationAccountId", validation.field)
        assertEquals(0, recorder.totalCalls)
    }

    private fun prepareUseCase(
        accounts: List<Account>,
        categories: List<Category> = listOf(category()),
        suggestion: SmsTransactionSuggestion,
    ): PrepareSmsTransactionReviewUseCase =
        PrepareSmsTransactionReviewUseCase(
            featureFlagProvider = enabledFlags(),
            parser = SmsTransactionParser { SmsParseResult.Suggestion(suggestion) },
            accountRepository = FakeAccountRepository(LedgerResult.Success(accounts)),
            categoryRepository = FakeCategoryRepository(LedgerResult.Success(categories)),
        )

    private fun prepareCommand(): PrepareSmsTransactionReviewCommand =
        PrepareSmsTransactionReviewCommand(
            message = "INR 1,250.50 debited from account 1234",
            receivedAt = 1_721_800_000_000L,
            timeZoneId = "Asia/Kolkata",
            defaultCurrencyCode = "INR",
        )

    private fun expenseSuggestion(): SmsTransactionSuggestion =
        SmsTransactionSuggestion(
            amountMinorUnits = 125_050L,
            currencyCode = "INR",
            direction = SmsTransactionDirection.EXPENSE,
            occurredAt = 1_721_800_000_000L,
            accountHint = "1234",
            merchantOrCounterparty = "Grocery Mart",
            paymentRail = null,
            confidence = SmsParseConfidence.HIGH,
            missingFields = listOf(SmsMissingField.ACCOUNT, SmsMissingField.CATEGORY),
            evidence = emptyList(),
        )

    private fun confirmCommand(
        direction: SmsTransactionDirection = SmsTransactionDirection.EXPENSE,
        categoryId: String? = "category-1",
        destinationAccountId: String? = null,
        description: String? = "Store",
        userConfirmed: Boolean = true,
    ): ConfirmSmsTransactionCommand =
        ConfirmSmsTransactionCommand(
            direction = direction,
            amountMinorUnits = 125_050L,
            currencyCode = "INR",
            accountId = "account-1",
            categoryId = categoryId,
            destinationAccountId = destinationAccountId,
            occurredAt = 1_721_800_000_000L,
            description = description,
            confirmedAt = 1_721_800_100_000L,
            userConfirmed = userConfirmed,
        )

    private fun assertFeatureDisabled(result: ApplicationResult<*>) {
        val validation = assertIs<ApplicationError.Validation>(assertIs<ApplicationResult.Failure>(result).error)
        assertEquals("featureFlag", validation.field)
    }

    private fun <T> ApplicationResult<T>.successValue(): T =
        when (this) {
            is ApplicationResult.Success -> outcome.value
            is ApplicationResult.Failure -> kotlin.error("Expected success but was ${this.error}")
        }

    private fun enabledFlags(): StaticFeatureFlagProvider = StaticFeatureFlagProvider(setOf(FeatureFlag.SMS_ASSISTED_TRANSACTION_REVIEW))
}

private class CapturingRecorders {
    val result: ApplicationResult<TransactionRecord> =
        ApplicationResult.Failure(ApplicationError.Repository(LedgerError.StorageUnavailable))
    var incomeCommand: RecordIncomeCommand? = null
    var expenseCommand: RecordExpenseCommand? = null
    var transferCommand: RecordTransferCommand? = null

    val totalCalls: Int
        get() = listOf(incomeCommand, expenseCommand, transferCommand).count { it != null }

    fun useCase(featureFlagProvider: StaticFeatureFlagProvider): ConfirmSmsTransactionUseCase =
        ConfirmSmsTransactionUseCase(
            featureFlagProvider = featureFlagProvider,
            recordIncome = { command ->
                incomeCommand = command
                result
            },
            recordExpense = { command ->
                expenseCommand = command
                result
            },
            recordTransfer = { command ->
                transferCommand = command
                result
            },
        )
}

private class FakeAccountRepository(
    private val findAllResult: LedgerResult<List<Account>> = LedgerResult.Success(emptyList()),
) : AccountRepository {
    var findAllCalls: Int = 0

    override fun findAll(includeArchived: Boolean): LedgerResult<List<Account>> {
        findAllCalls += 1
        return findAllResult
    }

    override fun findById(accountId: String): LedgerResult<Account> = LedgerResult.Failure(LedgerError.AccountNotFound(accountId))

    override fun create(account: Account): LedgerResult<Account> = LedgerResult.Success(account)

    override fun update(account: Account): LedgerResult<Account> = LedgerResult.Success(account)
}

private class FakeCategoryRepository(
    private val findAllResult: LedgerResult<List<Category>> = LedgerResult.Success(emptyList()),
) : CategoryRepository {
    var findAllCalls: Int = 0

    override fun findAll(): LedgerResult<List<Category>> {
        findAllCalls += 1
        return findAllResult
    }

    override fun findById(categoryId: String): LedgerResult<Category> = LedgerResult.Failure(LedgerError.CategoryNotFound(categoryId))

    override fun create(category: Category): LedgerResult<Category> = LedgerResult.Success(category)

    override fun update(category: Category): LedgerResult<Category> = LedgerResult.Success(category)
}

private fun account(
    id: String,
    name: String,
    currency: String,
    archived: Boolean = false,
): Account =
    Account(
        id = id,
        name = name,
        type = AccountType.BANK,
        currencyCode = currency,
        isArchived = archived,
        createdAt = 1L,
        updatedAt = 1L,
    )

private fun category(
    id: String = "expense-food",
    name: String = "Food",
    type: CategoryType = CategoryType.EXPENSE,
    isDefault: Boolean = false,
): Category =
    Category(
        id = id,
        name = name,
        type = type,
        isDefault = isDefault,
        createdAt = 1L,
        updatedAt = 1L,
    )