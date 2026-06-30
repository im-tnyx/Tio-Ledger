package com.tioledger.data.repository

import com.tioledger.core.model.CurrencyCode
import com.tioledger.core.model.LedgerError
import com.tioledger.core.model.Money
import com.tioledger.database.TioLedgerDatabase
import com.tioledger.domain.model.Account
import com.tioledger.domain.model.AccountType
import com.tioledger.domain.model.Category
import com.tioledger.domain.model.CategoryType
import com.tioledger.domain.model.LedgerClass
import com.tioledger.domain.model.LedgerEntry
import com.tioledger.domain.model.LedgerEntryType
import com.tioledger.domain.model.LedgerSourceType
import com.tioledger.domain.model.PostingTarget
import com.tioledger.domain.model.SYSTEM_ADJUSTMENT_ID
import com.tioledger.domain.model.SYSTEM_EXPENSE_ID
import com.tioledger.domain.model.SYSTEM_INCOME_ID
import com.tioledger.domain.model.SYSTEM_OPENING_BALANCE_ID
import com.tioledger.domain.model.Transaction
import com.tioledger.domain.model.TransactionRecord
import com.tioledger.domain.model.TransactionSplit
import com.tioledger.domain.model.TransactionType
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DataLayerTest {
    private lateinit var database: TioLedgerDatabase
    private lateinit var accountRepository: SQLDelightAccountRepository
    private lateinit var categoryRepository: SQLDelightCategoryRepository
    private lateinit var ledgerRepository: SQLDelightLedgerRepository
    private lateinit var transactionRepository: SQLDelightTransactionRepository

    @BeforeTest
    fun setUp() {
        val driver = createTestSqlDriver()
        TioLedgerDatabase.Schema.create(driver)
        database = TioLedgerDatabase(driver)

        // Initialize repositories
        accountRepository = SQLDelightAccountRepository(database)
        categoryRepository = SQLDelightCategoryRepository(database)
        ledgerRepository = SQLDelightLedgerRepository(database)
        transactionRepository = SQLDelightTransactionRepository(database)

        // Setup base currency data
        database.tioLedgerDatabaseQueries.insertCurrency("USD", "US Dollar", "$", 2L, "en-US")
        database.tioLedgerDatabaseQueries.insertCurrency("INR", "Indian Rupee", "₹", 2L, "en-IN")

        // Setup system accounts
        database.accountsQueries.insertAccount(
            id = SYSTEM_INCOME_ID,
            name = "System Income Account",
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
        database.accountsQueries.insertAccount(
            id = SYSTEM_EXPENSE_ID,
            name = "System Expense Account",
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
        database.accountsQueries.insertAccount(
            id = SYSTEM_OPENING_BALANCE_ID,
            name = "System Opening Balance Account",
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
        database.accountsQueries.insertAccount(
            id = SYSTEM_ADJUSTMENT_ID,
            name = "System Adjustment Account",
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

    @Test
    fun testAccountRepositoryCRUD() {
        val account =
            Account(
                id = "acc-bank",
                name = "Checking Bank",
                type = AccountType.BANK,
                currencyCode = "USD",
                isArchived = false,
                displayOrder = 1,
                createdAt = 1000L,
                updatedAt = 1000L,
            )

        // 1. Create
        val createResult = accountRepository.create(account)
        assertTrue(createResult.isSuccess())
        assertEquals(account, createResult.getOrNull())

        // 2. Retrieve
        val retrieveResult = accountRepository.findById(account.id)
        assertTrue(retrieveResult.isSuccess())
        assertEquals(account, retrieveResult.getOrNull())

        // 3. Update
        val updatedAccount = account.copy(name = "Updated Checking Bank", isArchived = true)
        val updateResult = accountRepository.update(updatedAccount)
        assertTrue(updateResult.isSuccess())
        assertEquals(updatedAccount, updateResult.getOrNull())

        val retrieveUpdated = accountRepository.findById(account.id)
        assertTrue(retrieveUpdated.isSuccess())
        assertEquals(updatedAccount, retrieveUpdated.getOrNull())

        // 4. Retrieve Non-Existent
        val nonExistentResult = accountRepository.findById("non-existent-acc")
        assertTrue(nonExistentResult.isFailure())
        assertTrue(nonExistentResult.errorOrNull() is LedgerError.AccountNotFound)

        // 5. Create Duplicate Account
        val duplicateResult = accountRepository.create(updatedAccount)
        assertTrue(duplicateResult.isFailure())
        assertTrue(duplicateResult.errorOrNull() is LedgerError.DuplicateAccountId)
    }

    @Test
    fun testCategoryRepositoryCRUD() {
        val category =
            Category(
                id = "cat-groceries",
                name = "Groceries",
                type = CategoryType.EXPENSE,
                parentId = null,
                isDefault = false,
                createdAt = 1000L,
                updatedAt = 1000L,
            )

        // 1. Create
        val createResult = categoryRepository.create(category)
        assertTrue(createResult.isSuccess())
        assertEquals(category, createResult.getOrNull())

        // 2. Retrieve
        val retrieveResult = categoryRepository.findById(category.id)
        assertTrue(retrieveResult.isSuccess())
        assertEquals(category, retrieveResult.getOrNull())

        // 3. Update
        val updatedCategory = category.copy(name = "Groceries & Supermarket", isDefault = true)
        val updateResult = categoryRepository.update(updatedCategory)
        assertTrue(updateResult.isSuccess())
        assertEquals(updatedCategory, updateResult.getOrNull())

        val retrieveUpdated = categoryRepository.findById(category.id)
        assertTrue(retrieveUpdated.isSuccess())
        assertEquals(updatedCategory, retrieveUpdated.getOrNull())

        // 4. Retrieve Non-Existent
        val nonExistentResult = categoryRepository.findById("non-existent-cat")
        assertTrue(nonExistentResult.isFailure())
        assertTrue(nonExistentResult.errorOrNull() is LedgerError.CategoryNotFound)
    }

    @Test
    fun testTransactionRepositoryInsertAndLedgerRepositoryRead() {
        // Setup accounts
        val bank = Account("acc-bank", "Checking Bank", AccountType.BANK, "USD", createdAt = 1000L, updatedAt = 1000L)
        val cc = Account("acc-cc", "Visa CC", AccountType.CREDIT_CARD, "USD", createdAt = 1000L, updatedAt = 1000L)
        accountRepository.create(bank)
        accountRepository.create(cc)

        val tx =
            Transaction(
                id = "tx-1",
                timestamp = 2000L,
                description = "Credit Card Payment",
                type = TransactionType.TRANSFER,
                merchantId = null,
                createdBy = "MANUAL",
                isRecurring = false,
                createdAt = 2000L,
                updatedAt = 2000L,
            )

        val splitSource =
            TransactionSplit("split-source", "tx-1", "acc-bank", null, Money(100L, CurrencyCode("USD")), "Transfer Out", 2000L)
        val splitTarget = TransactionSplit("split-target", "tx-1", "acc-cc", null, Money(100L, CurrencyCode("USD")), "Transfer In", 2000L)

        val entrySource =
            LedgerEntry(
                "le-source", "tx-1", "split-source",
                PostingTarget.Account(
                    "acc-bank",
                    LedgerClass.ASSET,
                ),
                Money(100L, CurrencyCode("USD")), LedgerEntryType.CREDIT, LedgerSourceType.TRANSACTION, "CC payment out", 2000L,
            )
        val entryTarget =
            LedgerEntry(
                "le-target", "tx-1", "split-target",
                PostingTarget.Account(
                    "acc-cc",
                    LedgerClass.LIABILITY,
                ),
                Money(100L, CurrencyCode("USD")), LedgerEntryType.DEBIT, LedgerSourceType.TRANSACTION, "CC payment in", 2000L,
            )

        val record =
            TransactionRecord(
                transaction = tx,
                splits = listOf(splitSource, splitTarget),
                ledgerEntries = listOf(entrySource, entryTarget),
            )

        // 1. Record TransactionRecord
        val recordResult = transactionRepository.record(record)
        assertTrue(recordResult.isSuccess())
        assertEquals(record, recordResult.getOrNull())

        // 2. Fetch Ledger Entries By Account
        val bankEntriesResult = ledgerRepository.findEntriesByAccount("acc-bank")
        assertTrue(bankEntriesResult.isSuccess())
        val bankEntries = bankEntriesResult.getOrNull()!!
        assertEquals(1, bankEntries.size)
        assertEquals(entrySource, bankEntries[0])

        val ccEntriesResult = ledgerRepository.findEntriesByAccount("acc-cc")
        assertTrue(ccEntriesResult.isSuccess())
        val ccEntries = ccEntriesResult.getOrNull()!!
        assertEquals(1, ccEntries.size)
        assertEquals(entryTarget, ccEntries[0])

        // 3. Fetch Ledger Entries By Transaction
        val txEntriesResult = ledgerRepository.findEntriesByTransaction("tx-1")
        assertTrue(txEntriesResult.isSuccess())
        val txEntries = txEntriesResult.getOrNull()!!
        assertEquals(2, txEntries.size)
        assertTrue(txEntries.contains(entrySource))
        assertTrue(txEntries.contains(entryTarget))

        // 4. Duplicate Transaction ID (Double Posting Protection)
        val duplicateResult = transactionRepository.record(record)
        assertTrue(duplicateResult.isFailure())
        assertTrue(duplicateResult.errorOrNull() is LedgerError.DuplicateTransactionId)
    }

    @Test
    fun testTransactionRepositoryRollbackOnConstraintViolation() {
        // Setup only one account
        val bank = Account("acc-bank", "Checking Bank", AccountType.BANK, "USD", createdAt = 1000L, updatedAt = 1000L)
        accountRepository.create(bank)

        val tx =
            Transaction(
                id = "tx-failed",
                timestamp = 2000L,
                description = "Failing transaction",
                type = TransactionType.TRANSFER,
                merchantId = null,
                createdBy = "MANUAL",
                isRecurring = false,
                createdAt = 2000L,
                updatedAt = 2000L,
            )

        // Split refers to "acc-non-existent" which does not exist in accounts table!
        val splitSource =
            TransactionSplit("split-failed-source", "tx-failed", "acc-bank", null, Money(100L, CurrencyCode("USD")), "Source", 2000L)
        val splitTarget =
            TransactionSplit(
                "split-failed-target",
                "tx-failed",
                "acc-non-existent",
                null,
                Money(100L, CurrencyCode("USD")),
                "Target non-existent",
                2000L,
            )

        val entrySource =
            LedgerEntry(
                "le-failed-source", "tx-failed", "split-failed-source",
                PostingTarget.Account(
                    "acc-bank",
                    LedgerClass.ASSET,
                ),
                Money(100L, CurrencyCode("USD")), LedgerEntryType.CREDIT, LedgerSourceType.TRANSACTION, "Source", 2000L,
            )
        val entryTarget =
            LedgerEntry(
                "le-failed-target", "tx-failed", "split-failed-target",
                PostingTarget.Account(
                    "acc-non-existent",
                    LedgerClass.ASSET,
                ),
                Money(100L, CurrencyCode("USD")), LedgerEntryType.DEBIT, LedgerSourceType.TRANSACTION, "Target non-existent", 2000L,
            )

        val record =
            TransactionRecord(
                transaction = tx,
                splits = listOf(splitSource, splitTarget),
                ledgerEntries = listOf(entrySource, entryTarget),
            )

        // 1. Record TransactionRecord (should fail and rollback)
        val recordResult = transactionRepository.record(record)
        assertTrue(recordResult.isFailure())
        assertTrue(recordResult.errorOrNull() is LedgerError.ConstraintViolation)

        // 2. Verify rollback: check that the transaction table does NOT contain "tx-failed"
        val queryResult = database.transactionsQueries.selectTransactionById("tx-failed").executeAsOneOrNull()
        assertNull(queryResult, "Transaction was inserted despite split constraint violation (Rollback failed)")
    }
}
