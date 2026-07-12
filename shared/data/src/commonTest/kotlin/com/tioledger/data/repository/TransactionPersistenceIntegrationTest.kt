package com.tioledger.data.repository

import com.tioledger.application.model.ApplicationResult
import com.tioledger.application.usecase.transaction.RecordExpenseCommand
import com.tioledger.application.usecase.transaction.RecordExpenseUseCase
import com.tioledger.application.usecase.transaction.RecordIncomeCommand
import com.tioledger.application.usecase.transaction.RecordIncomeUseCase
import com.tioledger.application.usecase.transaction.RecordTransferCommand
import com.tioledger.application.usecase.transaction.RecordTransferUseCase
import com.tioledger.core.model.CurrencyCode
import com.tioledger.core.model.Money
import com.tioledger.core.util.IdGenerator
import com.tioledger.database.TioLedgerDatabase
import com.tioledger.domain.model.Account
import com.tioledger.domain.model.AccountType
import com.tioledger.domain.model.Category
import com.tioledger.domain.model.CategoryType
import com.tioledger.domain.model.LedgerEntry
import com.tioledger.domain.model.LedgerEntryType
import com.tioledger.domain.model.SYSTEM_ADJUSTMENT_ID
import com.tioledger.domain.model.SYSTEM_EXPENSE_ID
import com.tioledger.domain.model.SYSTEM_INCOME_ID
import com.tioledger.domain.model.SYSTEM_OPENING_BALANCE_ID
import com.tioledger.domain.model.TransactionRecord
import com.tioledger.domain.model.TransactionType
import com.tioledger.finance.engine.PostingEngine
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class TransactionPersistenceIntegrationTest {
    private lateinit var database: TioLedgerDatabase
    private lateinit var accountRepository: SQLDelightAccountRepository
    private lateinit var categoryRepository: SQLDelightCategoryRepository
    private lateinit var ledgerRepository: SQLDelightLedgerRepository
    private lateinit var transactionRepository: SQLDelightTransactionRepository
    private lateinit var recordIncomeUseCase: RecordIncomeUseCase
    private lateinit var recordExpenseUseCase: RecordExpenseUseCase
    private lateinit var recordTransferUseCase: RecordTransferUseCase

    @BeforeTest
    fun setUp() {
        val driver = createTestSqlDriver()
        TioLedgerDatabase.Schema.create(driver)
        database = TioLedgerDatabase(driver)
        accountRepository = SQLDelightAccountRepository(database)
        categoryRepository = SQLDelightCategoryRepository(database)
        ledgerRepository = SQLDelightLedgerRepository(database)
        transactionRepository = SQLDelightTransactionRepository(database)

        database.tioLedgerDatabaseQueries.insertCurrency("USD", "US Dollar", "$", 2L, "en-US")
        database.tioLedgerDatabaseQueries.insertCurrency("INR", "Indian Rupee", "INR", 2L, "en-IN")

        insertSystemAccount(SYSTEM_INCOME_ID)
        insertSystemAccount(SYSTEM_EXPENSE_ID)
        insertSystemAccount(SYSTEM_OPENING_BALANCE_ID)
        insertSystemAccount(SYSTEM_ADJUSTMENT_ID)

        val postingEngine = PostingEngine(IncrementingIdGenerator())
        recordIncomeUseCase = RecordIncomeUseCase(accountRepository, categoryRepository, transactionRepository, postingEngine)
        recordExpenseUseCase = RecordExpenseUseCase(accountRepository, categoryRepository, transactionRepository, postingEngine)
        recordTransferUseCase = RecordTransferUseCase(accountRepository, transactionRepository, postingEngine)
    }

    @Test
    fun recordIncomePersistsBalancedLedgerTransaction() {
        val cash = createAccount("acc-cash", "Cash", AccountType.CASH)
        val salary = createCategory("cat-salary", "Salary", CategoryType.INCOME)

        val result =
            recordIncomeUseCase(
                RecordIncomeCommand(
                    timestamp = 1_000L,
                    description = "Salary",
                    amount = Money(50_000L, CurrencyCode("USD")),
                    accountId = cash.id,
                    categoryId = salary.id,
                    merchantId = null,
                    createdAt = 1_000L,
                ),
            )

        val record = assertSuccess(result)
        assertEquals(TransactionType.INCOME, record.transaction.type)
        assertTransactionPersisted(record)
    }

    @Test
    fun recordExpensePersistsBalancedLedgerTransaction() {
        val cash = createAccount("acc-wallet", "Wallet", AccountType.WALLET)
        val food = createCategory("cat-food", "Food", CategoryType.EXPENSE)

        val result =
            recordExpenseUseCase(
                RecordExpenseCommand(
                    timestamp = 2_000L,
                    description = "Lunch",
                    amount = Money(1_250L, CurrencyCode("USD")),
                    accountId = cash.id,
                    categoryId = food.id,
                    merchantId = null,
                    createdAt = 2_000L,
                ),
            )

        val record = assertSuccess(result)
        assertEquals(TransactionType.EXPENSE, record.transaction.type)
        assertTransactionPersisted(record)
    }

    @Test
    fun recordTransferPersistsBalancedLedgerTransaction() {
        val bank = createAccount("acc-bank", "Bank", AccountType.BANK)
        val cash = createAccount("acc-cash", "Cash", AccountType.CASH)

        val result =
            recordTransferUseCase(
                RecordTransferCommand(
                    timestamp = 3_000L,
                    description = "ATM withdrawal",
                    amount = Money(2_000L, CurrencyCode("USD")),
                    sourceAccountId = bank.id,
                    targetAccountId = cash.id,
                    createdAt = 3_000L,
                ),
            )

        val record = assertSuccess(result)
        assertEquals(TransactionType.TRANSFER, record.transaction.type)
        assertEquals(2, record.splits.size)
        assertTransactionPersisted(record)
    }

    private fun insertSystemAccount(id: String) {
        database.accountsQueries.insertAccount(
            id = id,
            name = id,
            type = AccountType.WALLET.name,
            currency_code = "USD",
            is_archived = 0L,
            display_order = 0L,
            created_at = 0L,
            updated_at = 0L,
            entity_version = 1L,
            sync_version = 0L,
            device_id = null,
            updated_by = null,
            deleted_at = null,
        )
    }

    private fun createAccount(
        id: String,
        name: String,
        type: AccountType,
    ): Account {
        val account =
            Account(
                id = id,
                name = name,
                type = type,
                currencyCode = "USD",
                createdAt = 1L,
                updatedAt = 1L,
            )
        val result = accountRepository.create(account)
        assertTrue(result.isSuccess())
        return account
    }

    private fun createCategory(
        id: String,
        name: String,
        type: CategoryType,
    ): Category {
        val category =
            Category(
                id = id,
                name = name,
                type = type,
                createdAt = 1L,
                updatedAt = 1L,
            )
        val result = categoryRepository.create(category)
        assertTrue(result.isSuccess())
        return category
    }

    private fun assertTransactionPersisted(record: TransactionRecord) {
        val persistedTransaction = database.transactionsQueries.selectTransactionById(record.transaction.id).executeAsOneOrNull()
        assertTrue(persistedTransaction != null)

        val entries = ledgerRepository.findEntriesByTransaction(record.transaction.id).getOrNull().orEmpty()
        assertEquals(2, entries.size)
        assertBalanced(entries)
    }

    private fun assertBalanced(entries: List<LedgerEntry>) {
        val debitTotal = entries.filter { it.entryType == LedgerEntryType.DEBIT }.sumOf { it.amount.amount }
        val creditTotal = entries.filter { it.entryType == LedgerEntryType.CREDIT }.sumOf { it.amount.amount }
        assertEquals(debitTotal, creditTotal)
    }

    private fun assertSuccess(result: ApplicationResult<TransactionRecord>): TransactionRecord =
        when (result) {
            is ApplicationResult.Success -> result.outcome.value
            is ApplicationResult.Failure -> fail("Expected success but got ${result.error}")
        }
}

private class IncrementingIdGenerator : IdGenerator {
    private var counter = 0

    override fun nextId(): String {
        counter += 1
        return "generated-$counter"
    }
}
