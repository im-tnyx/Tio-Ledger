package com.tioledger.application.usecase

import com.tioledger.application.model.ApplicationError
import com.tioledger.application.model.ApplicationResult
import com.tioledger.application.model.UseCaseOutcome
import com.tioledger.application.usecase.account.ArchiveAccountCommand
import com.tioledger.application.usecase.account.ArchiveAccountUseCase
import com.tioledger.application.usecase.account.CreateAccountCommand
import com.tioledger.application.usecase.account.CreateAccountUseCase
import com.tioledger.application.usecase.account.UpdateAccountCommand
import com.tioledger.application.usecase.account.UpdateAccountUseCase
import com.tioledger.application.usecase.category.ArchiveCategoryCommand
import com.tioledger.application.usecase.category.ArchiveCategoryUseCase
import com.tioledger.application.usecase.category.CreateCategoryCommand
import com.tioledger.application.usecase.category.CreateCategoryUseCase
import com.tioledger.application.usecase.category.UpdateCategoryCommand
import com.tioledger.application.usecase.category.UpdateCategoryUseCase
import com.tioledger.application.usecase.transaction.RecordAdjustmentCommand
import com.tioledger.application.usecase.transaction.RecordAdjustmentUseCase
import com.tioledger.application.usecase.transaction.RecordExpenseCommand
import com.tioledger.application.usecase.transaction.RecordExpenseUseCase
import com.tioledger.application.usecase.transaction.RecordIncomeCommand
import com.tioledger.application.usecase.transaction.RecordIncomeUseCase
import com.tioledger.application.usecase.transaction.RecordOpeningBalanceCommand
import com.tioledger.application.usecase.transaction.RecordOpeningBalanceUseCase
import com.tioledger.application.usecase.transaction.RecordTransferCommand
import com.tioledger.application.usecase.transaction.RecordTransferUseCase
import com.tioledger.core.model.CurrencyCode
import com.tioledger.core.model.LedgerError
import com.tioledger.core.model.LedgerResult
import com.tioledger.core.model.Money
import com.tioledger.core.util.IdGenerator
import com.tioledger.domain.event.DomainEvent
import com.tioledger.domain.model.Account
import com.tioledger.domain.model.AccountType
import com.tioledger.domain.model.Category
import com.tioledger.domain.model.CategoryType
import com.tioledger.domain.model.TransactionRecord
import com.tioledger.domain.model.TransactionType
import com.tioledger.domain.repository.AccountRepository
import com.tioledger.domain.repository.CategoryRepository
import com.tioledger.domain.repository.TransactionRepository
import com.tioledger.finance.engine.PostingEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class ApplicationUseCaseTest {
    private val usd = CurrencyCode("USD")

    @Test
    fun createAccountValidatesAndPersistsAccount() {
        val accounts = FakeAccountRepository()
        val useCase = CreateAccountUseCase(accounts)

        val result =
            useCase(
                CreateAccountCommand(
                    id = " acc-1 ",
                    name = " Checking ",
                    type = AccountType.BANK,
                    currencyCode = " usd ",
                    displayOrder = 2,
                    createdAt = 100L,
                ),
            )

        val outcome = assertSuccess(result)
        assertEquals("acc-1", outcome.value.id)
        assertEquals("Checking", outcome.value.name)
        assertEquals("USD", outcome.value.currencyCode)
        assertEquals(outcome.value, accounts.accounts["acc-1"])
        assertTrue(outcome.events.single() is DomainEvent.AccountCreated)
    }

    @Test
    fun createAccountRejectsInvalidCurrency() {
        val result =
            CreateAccountUseCase(FakeAccountRepository())(
                CreateAccountCommand(
                    id = "acc-1",
                    name = "Checking",
                    type = AccountType.BANK,
                    currencyCode = "US1",
                    createdAt = 100L,
                ),
            )

        assertTrue(assertFailure(result) is ApplicationError.Validation)
    }

    @Test
    fun updateAccountPersistsMutableFields() {
        val accounts = FakeAccountRepository(listOf(account("acc-1")))
        val result =
            UpdateAccountUseCase(accounts)(
                UpdateAccountCommand(
                    accountId = " acc-1 ",
                    name = "Primary Bank",
                    displayOrder = 5,
                    updatedAt = 200L,
                    updatedBy = "device-a",
                ),
            )

        val outcome = assertSuccess(result)
        assertEquals("Primary Bank", outcome.value.name)
        assertEquals(5, outcome.value.displayOrder)
        assertEquals(200L, outcome.value.updatedAt)
        assertTrue(outcome.events.single() is DomainEvent.AccountUpdated)
    }

    @Test
    fun archiveAccountMarksAccountArchived() {
        val accounts = FakeAccountRepository(listOf(account("acc-1")))
        val result =
            ArchiveAccountUseCase(accounts)(
                ArchiveAccountCommand(
                    accountId = "acc-1",
                    archivedAt = 300L,
                    updatedBy = "device-a",
                ),
            )

        val outcome = assertSuccess(result)
        assertTrue(outcome.value.isArchived)
        assertEquals(300L, outcome.value.updatedAt)
        assertTrue(outcome.events.single() is DomainEvent.AccountArchived)
    }

    @Test
    fun archiveAccountRejectsAlreadyArchivedAccount() {
        val accounts = FakeAccountRepository(listOf(account("acc-1", isArchived = true)))
        val result = ArchiveAccountUseCase(accounts)(ArchiveAccountCommand("acc-1", 300L))

        assertTrue(assertFailure(result) is ApplicationError.Validation)
    }

    @Test
    fun createCategoryValidatesAndPersistsCategory() {
        val categories = FakeCategoryRepository()
        val result =
            CreateCategoryUseCase(categories)(
                CreateCategoryCommand(
                    id = " cat-1 ",
                    name = " Salary ",
                    type = CategoryType.INCOME,
                    createdAt = 100L,
                ),
            )

        val outcome = assertSuccess(result)
        assertEquals("cat-1", outcome.value.id)
        assertEquals("Salary", outcome.value.name)
        assertTrue(outcome.events.single() is DomainEvent.CategoryCreated)
    }

    @Test
    fun updateCategoryPersistsNameAndParent() {
        val categories = FakeCategoryRepository(listOf(category("cat-1", CategoryType.EXPENSE)))
        val result =
            UpdateCategoryUseCase(categories)(
                UpdateCategoryCommand(
                    categoryId = "cat-1",
                    name = "Food",
                    parentId = "parent-1",
                    updatedAt = 200L,
                ),
            )

        val outcome = assertSuccess(result)
        assertEquals("Food", outcome.value.name)
        assertEquals("parent-1", outcome.value.parentId)
        assertTrue(outcome.events.single() is DomainEvent.CategoryUpdated)
    }

    @Test
    fun archiveCategorySetsDeletedAt() {
        val categories = FakeCategoryRepository(listOf(category("cat-1", CategoryType.EXPENSE)))
        val result = ArchiveCategoryUseCase(categories)(ArchiveCategoryCommand("cat-1", 300L))

        val outcome = assertSuccess(result)
        assertEquals(300L, outcome.value.deletedAt)
        assertTrue(outcome.events.single() is DomainEvent.CategoryArchived)
    }

    @Test
    fun archiveCategoryRejectsAlreadyArchivedCategory() {
        val categories = FakeCategoryRepository(listOf(category("cat-1", CategoryType.EXPENSE).copy(deletedAt = 250L)))
        val result = ArchiveCategoryUseCase(categories)(ArchiveCategoryCommand("cat-1", 300L))

        assertTrue(assertFailure(result) is ApplicationError.Validation)
    }

    @Test
    fun recordIncomeUsesLedgerEngineAndRecordsTransaction() {
        val accounts = FakeAccountRepository(listOf(account("acc-1")))
        val categories = FakeCategoryRepository(listOf(category("cat-income", CategoryType.INCOME)))
        val transactions = FakeTransactionRepository()
        val result =
            RecordIncomeUseCase(accounts, categories, transactions, postingEngine())(
                RecordIncomeCommand(
                    timestamp = 1000L,
                    description = "Salary",
                    amount = Money(5000L, usd),
                    accountId = " acc-1 ",
                    categoryId = " cat-income ",
                    merchantId = null,
                    createdAt = 1000L,
                ),
            )

        val outcome = assertSuccess(result)
        assertEquals(TransactionType.INCOME, outcome.value.transaction.type)
        assertEquals(2, outcome.value.ledgerEntries.size)
        assertEquals(outcome.value, transactions.records.single())
        assertTrue(outcome.events.single() is DomainEvent.TransactionRecorded)
    }

    @Test
    fun recordIncomeReturnsRepositoryErrorWhenAccountIsMissing() {
        val result =
            RecordIncomeUseCase(FakeAccountRepository(), FakeCategoryRepository(), FakeTransactionRepository(), postingEngine())(
                RecordIncomeCommand(1000L, "Salary", Money(5000L, usd), "missing", null, null, 1000L),
            )

        assertTrue(assertFailure(result) is ApplicationError.Repository)
    }

    @Test
    fun recordExpenseReturnsLedgerErrorForWrongCategoryType() {
        val accounts = FakeAccountRepository(listOf(account("acc-1")))
        val categories = FakeCategoryRepository(listOf(category("cat-income", CategoryType.INCOME)))
        val transactions = FakeTransactionRepository()
        val result =
            RecordExpenseUseCase(accounts, categories, transactions, postingEngine())(
                RecordExpenseCommand(1000L, "Groceries", Money(100L, usd), "acc-1", "cat-income", null, 1000L),
            )

        assertTrue(assertFailure(result) is ApplicationError.Ledger)
        assertTrue(transactions.records.isEmpty())
    }

    @Test
    fun recordTransferUsesLedgerEngineAndRecordsTransaction() {
        val accounts = FakeAccountRepository(listOf(account("source"), account("target", AccountType.CASH)))
        val transactions = FakeTransactionRepository()
        val result =
            RecordTransferUseCase(accounts, transactions, postingEngine())(
                RecordTransferCommand(1000L, "Move cash", Money(250L, usd), "source", "target", 1000L),
            )

        val outcome = assertSuccess(result)
        assertEquals(TransactionType.TRANSFER, outcome.value.transaction.type)
        assertEquals(2, outcome.value.splits.size)
        assertEquals(2, outcome.value.ledgerEntries.size)
    }

    @Test
    fun recordAdjustmentUsesLedgerEngineAndRecordsTransaction() {
        val accounts = FakeAccountRepository(listOf(account("acc-1")))
        val transactions = FakeTransactionRepository()
        val result =
            RecordAdjustmentUseCase(accounts, transactions, postingEngine())(
                RecordAdjustmentCommand(1000L, "Reconcile", Money(25L, usd), "acc-1", isDebit = true, createdAt = 1000L),
            )

        val outcome = assertSuccess(result)
        assertEquals(TransactionType.ADJUSTMENT, outcome.value.transaction.type)
        assertEquals(2, outcome.value.ledgerEntries.size)
    }

    @Test
    fun recordOpeningBalanceUsesLedgerEngineAndRecordsTransaction() {
        val accounts = FakeAccountRepository(listOf(account("acc-1")))
        val transactions = FakeTransactionRepository()
        val result =
            RecordOpeningBalanceUseCase(accounts, transactions, postingEngine())(
                RecordOpeningBalanceCommand(1000L, "Opening", Money(10000L, usd), "acc-1", 1000L),
            )

        val outcome = assertSuccess(result)
        assertEquals(TransactionType.ADJUSTMENT, outcome.value.transaction.type)
        assertEquals(2, outcome.value.ledgerEntries.size)
        assertEquals(outcome.value, transactions.records.single())
    }

    private fun postingEngine(): PostingEngine = PostingEngine(TestIdGenerator())

    private fun account(
        id: String,
        type: AccountType = AccountType.BANK,
        isArchived: Boolean = false,
    ): Account =
        Account(
            id = id,
            name = "Account $id",
            type = type,
            currencyCode = "USD",
            isArchived = isArchived,
            createdAt = 1L,
            updatedAt = 1L,
        )

    private fun category(
        id: String,
        type: CategoryType,
    ): Category =
        Category(
            id = id,
            name = "Category $id",
            type = type,
            createdAt = 1L,
            updatedAt = 1L,
        )

    private fun <T> assertSuccess(result: ApplicationResult<T>): UseCaseOutcome<T> =
        when (result) {
            is ApplicationResult.Success -> result.outcome
            is ApplicationResult.Failure -> fail("Expected success but got ${result.error}")
        }

    private fun <T> assertFailure(result: ApplicationResult<T>): ApplicationError =
        when (result) {
            is ApplicationResult.Success -> fail("Expected failure but got ${result.outcome}")
            is ApplicationResult.Failure -> result.error
        }
}

private class TestIdGenerator : IdGenerator {
    private var counter = 0

    override fun nextId(): String {
        counter += 1
        return "id-$counter"
    }
}

private class FakeAccountRepository(initialAccounts: List<Account> = emptyList()) : AccountRepository {
    val accounts: MutableMap<String, Account> = initialAccounts.associateBy { it.id }.toMutableMap()
    var createError: LedgerError? = null
    var updateError: LedgerError? = null

    override fun findById(accountId: String): LedgerResult<Account> =
        accounts[accountId]?.let { LedgerResult.Success(it) }
            ?: LedgerResult.Failure(LedgerError.AccountNotFound(accountId))

    override fun create(account: Account): LedgerResult<Account> {
        createError?.let { return LedgerResult.Failure(it) }
        accounts[account.id] = account
        return LedgerResult.Success(account)
    }

    override fun update(account: Account): LedgerResult<Account> {
        updateError?.let { return LedgerResult.Failure(it) }
        if (!accounts.containsKey(account.id)) {
            return LedgerResult.Failure(LedgerError.AccountNotFound(account.id))
        }
        accounts[account.id] = account
        return LedgerResult.Success(account)
    }
}

private class FakeCategoryRepository(initialCategories: List<Category> = emptyList()) : CategoryRepository {
    val categories: MutableMap<String, Category> = initialCategories.associateBy { it.id }.toMutableMap()

    override fun findById(categoryId: String): LedgerResult<Category> =
        categories[categoryId]?.let { LedgerResult.Success(it) }
            ?: LedgerResult.Failure(LedgerError.CategoryNotFound(categoryId))

    override fun create(category: Category): LedgerResult<Category> {
        categories[category.id] = category
        return LedgerResult.Success(category)
    }

    override fun update(category: Category): LedgerResult<Category> {
        if (!categories.containsKey(category.id)) {
            return LedgerResult.Failure(LedgerError.CategoryNotFound(category.id))
        }
        categories[category.id] = category
        return LedgerResult.Success(category)
    }
}

private class FakeTransactionRepository : TransactionRepository {
    val records = mutableListOf<TransactionRecord>()
    var recordError: LedgerError? = null

    override fun record(record: TransactionRecord): LedgerResult<TransactionRecord> {
        recordError?.let { return LedgerResult.Failure(it) }
        records.add(record)
        return LedgerResult.Success(record)
    }
}
