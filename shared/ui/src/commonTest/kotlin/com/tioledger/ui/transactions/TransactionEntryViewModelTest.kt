package com.tioledger.ui.transactions

import com.tioledger.application.usecase.account.ListAccountSummariesUseCase
import com.tioledger.application.usecase.category.ListCategoriesUseCase
import com.tioledger.application.usecase.transaction.RecordExpenseUseCase
import com.tioledger.application.usecase.transaction.RecordIncomeUseCase
import com.tioledger.application.usecase.transaction.RecordTransferUseCase
import com.tioledger.core.model.CurrencyCode
import com.tioledger.core.model.LedgerError
import com.tioledger.core.model.LedgerResult
import com.tioledger.core.model.Money
import com.tioledger.core.util.IdGenerator
import com.tioledger.domain.model.Account
import com.tioledger.domain.model.AccountType
import com.tioledger.domain.model.Category
import com.tioledger.domain.model.CategoryType
import com.tioledger.domain.model.LedgerEntry
import com.tioledger.domain.model.LedgerEntryType
import com.tioledger.domain.model.LedgerSourceType
import com.tioledger.domain.model.PostingTarget
import com.tioledger.domain.model.TransactionRecord
import com.tioledger.domain.repository.AccountRepository
import com.tioledger.domain.repository.CategoryRepository
import com.tioledger.domain.repository.LedgerRepository
import com.tioledger.domain.repository.TransactionRepository
import com.tioledger.finance.engine.BalanceCalculator
import com.tioledger.finance.engine.PostingEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TransactionEntryViewModelTest {
    @Test
    fun initialLoadExposesReferenceData() {
        val viewModel = transactionEntryViewModel()

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertEquals(2, state.accountOptions.size)
        assertEquals(1, state.categoryOptions.size)
        assertEquals(TransactionType.Expense, state.transactionType)
    }

    @Test
    fun saveIncomePersistsTransactionAndEmitsSuccessEvent() {
        val transactions = FakeTransactionRepository()
        val viewModel = transactionEntryViewModel(transactionRepository = transactions)

        viewModel.onAction(TransactionEntryAction.TypeChanged(TransactionType.Income))
        viewModel.onAction(TransactionEntryAction.SourceAccountClicked)
        viewModel.onAction(TransactionEntryAction.AccountSelected("cash"))
        viewModel.onAction(TransactionEntryAction.CategoryClicked)
        viewModel.onAction(TransactionEntryAction.CategorySelected("salary"))
        viewModel.onAction(TransactionEntryAction.AmountChanged("1250.50"))
        viewModel.onAction(TransactionEntryAction.NoteChanged("Monthly salary"))
        viewModel.onAction(TransactionEntryAction.SaveClicked)

        val state = viewModel.uiState.value
        assertFalse(state.isSaving)
        assertEquals("Transaction saved successfully.", state.saveSuccessMessage)
        assertEquals("", state.amount)
        assertEquals(1, transactions.records.size)
        assertEquals(com.tioledger.domain.model.TransactionType.INCOME, transactions.records.single().transaction.type)
        assertIs<TransactionEntryEvent.TransactionSaved>(viewModel.event.value)
    }

    @Test
    fun saveTransferRequiresDistinctAccounts() {
        val transactions = FakeTransactionRepository()
        val viewModel = transactionEntryViewModel(transactionRepository = transactions)

        viewModel.onAction(TransactionEntryAction.TypeChanged(TransactionType.Transfer))
        viewModel.onAction(TransactionEntryAction.SourceAccountClicked)
        viewModel.onAction(TransactionEntryAction.AccountSelected("cash"))
        viewModel.onAction(TransactionEntryAction.TargetAccountClicked)
        viewModel.onAction(TransactionEntryAction.AccountSelected("cash"))
        viewModel.onAction(TransactionEntryAction.AmountChanged("10.00"))
        viewModel.onAction(TransactionEntryAction.SaveClicked)

        val state = viewModel.uiState.value
        assertEquals("Source and destination accounts must be different.", state.validationErrorMessage)
        assertTrue(transactions.records.isEmpty())
    }

    @Test
    fun persistenceFailureIsSurfacedInUiState() {
        val transactions =
            FakeTransactionRepository().apply {
                recordError = LedgerError.DuplicateTransactionId("txn-1")
            }
        val viewModel = transactionEntryViewModel(transactionRepository = transactions)

        viewModel.onAction(TransactionEntryAction.SourceAccountClicked)
        viewModel.onAction(TransactionEntryAction.AccountSelected("cash"))
        viewModel.onAction(TransactionEntryAction.CategoryClicked)
        viewModel.onAction(TransactionEntryAction.CategorySelected("food"))
        viewModel.onAction(TransactionEntryAction.AmountChanged("25.00"))
        viewModel.onAction(TransactionEntryAction.SaveClicked)

        val state = viewModel.uiState.value
        assertFalse(state.isSaving)
        assertEquals("Unable to save transaction.", state.persistenceErrorMessage)
        assertNotNull(state.selectedAccount)
    }
}

private fun transactionEntryViewModel(
    accountRepository: AccountRepository = FakeAccountRepository(),
    categoryRepository: CategoryRepository = FakeCategoryRepository(),
    ledgerRepository: LedgerRepository = FakeLedgerRepository(),
    transactionRepository: FakeTransactionRepository = FakeTransactionRepository(),
): TransactionEntryViewModel {
    val postingEngine = PostingEngine(TestIdGenerator())
    return TransactionEntryViewModel(
        listAccountSummariesUseCase =
            ListAccountSummariesUseCase(
                accountRepository,
                ledgerRepository,
                BalanceCalculator(),
            ),
        listCategoriesUseCase = ListCategoriesUseCase(categoryRepository),
        recordIncomeUseCase =
            RecordIncomeUseCase(
                accountRepository,
                categoryRepository,
                transactionRepository,
                postingEngine,
            ),
        recordExpenseUseCase =
            RecordExpenseUseCase(
                accountRepository,
                categoryRepository,
                transactionRepository,
                postingEngine,
            ),
        recordTransferUseCase =
            RecordTransferUseCase(
                accountRepository,
                transactionRepository,
                postingEngine,
            ),
        nowProvider = { 1_720_000_000_000L },
    )
}

private class TestIdGenerator : IdGenerator {
    private var counter = 0

    override fun nextId(): String {
        counter += 1
        return "txn-$counter"
    }
}

private class FakeAccountRepository : AccountRepository {
    private val accounts =
        listOf(
            Account(
                id = "cash",
                name = "Cash",
                type = AccountType.CASH,
                currencyCode = "USD",
                createdAt = 1L,
                updatedAt = 1L,
            ),
            Account(
                id = "bank",
                name = "Bank",
                type = AccountType.BANK,
                currencyCode = "USD",
                createdAt = 1L,
                updatedAt = 1L,
            ),
        )

    override fun findAll(includeArchived: Boolean): LedgerResult<List<Account>> = LedgerResult.Success(accounts)

    override fun findById(accountId: String): LedgerResult<Account> =
        accounts.firstOrNull { it.id == accountId }?.let { LedgerResult.Success(it) }
            ?: LedgerResult.Failure(LedgerError.AccountNotFound(accountId))

    override fun create(account: Account): LedgerResult<Account> = LedgerResult.Success(account)

    override fun update(account: Account): LedgerResult<Account> = LedgerResult.Success(account)
}

private class FakeCategoryRepository : CategoryRepository {
    private val categories =
        listOf(
            Category(
                id = "food",
                name = "Food",
                type = CategoryType.EXPENSE,
                createdAt = 1L,
                updatedAt = 1L,
            ),
            Category(
                id = "salary",
                name = "Salary",
                type = CategoryType.INCOME,
                isDefault = true,
                createdAt = 1L,
                updatedAt = 1L,
            ),
        )

    override fun findAll(): LedgerResult<List<Category>> = LedgerResult.Success(categories)

    override fun findById(categoryId: String): LedgerResult<Category> =
        categories.firstOrNull { it.id == categoryId }?.let { LedgerResult.Success(it) }
            ?: LedgerResult.Failure(LedgerError.CategoryNotFound(categoryId))

    override fun create(category: Category): LedgerResult<Category> = LedgerResult.Success(category)

    override fun update(category: Category): LedgerResult<Category> = LedgerResult.Success(category)
}

private class FakeLedgerRepository : LedgerRepository {
    override fun findEntriesByAccount(accountId: String): LedgerResult<List<LedgerEntry>> =
        LedgerResult.Success(
            listOf(
                LedgerEntry(
                    id = "entry-$accountId",
                    transactionId = "seed-$accountId",
                    target = PostingTarget.Account(accountId, AccountType.CASH.ledgerClass),
                    amount = Money(0L, CurrencyCode("USD")),
                    entryType = LedgerEntryType.DEBIT,
                    sourceType = LedgerSourceType.TRANSACTION,
                    createdAt = 1L,
                ),
            ),
        )

    override fun findEntriesByTransaction(transactionId: String): LedgerResult<List<LedgerEntry>> = LedgerResult.Success(emptyList())
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
