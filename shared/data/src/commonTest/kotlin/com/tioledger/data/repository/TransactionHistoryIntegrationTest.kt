package com.tioledger.data.repository

import com.tioledger.database.TioLedgerDatabase
import com.tioledger.domain.model.AccountType
import com.tioledger.domain.model.CategoryType
import com.tioledger.domain.model.LedgerEntryType
import com.tioledger.domain.model.LedgerSourceType
import com.tioledger.domain.model.TransactionType
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TransactionHistoryIntegrationTest {
    private lateinit var database: TioLedgerDatabase
    private lateinit var repository: SQLDelightTransactionRepository

    @BeforeTest
    fun setUp() {
        val driver = createTestSqlDriver()
        TioLedgerDatabase.Schema.create(driver)
        database = TioLedgerDatabase(driver)
        repository = SQLDelightTransactionRepository(database)

        database.tioLedgerDatabaseQueries.insertCurrency("USD", "US Dollar", "$", 2L, "en-US")
        insertAccount("bank", "Bank")
        insertAccount("cash", "Cash")
        insertCategory("salary", "Salary")
    }

    @Test
    fun returnsNewestFirstHistoryWithAccountAndCategoryContext() {
        insertTransaction(
            id = "income",
            timestamp = 1_000L,
            type = TransactionType.INCOME,
            description = "Salary",
        )
        insertSplit(
            id = "income-split",
            transactionId = "income",
            accountId = "bank",
            categoryId = "salary",
            amount = 50_000L,
        )
        insertLedgerEntry(
            id = "income-entry",
            transactionId = "income",
            splitId = "income-split",
            accountId = "bank",
            entryType = LedgerEntryType.DEBIT,
        )

        insertTransaction(
            id = "transfer",
            timestamp = 2_000L,
            type = TransactionType.TRANSFER,
            description = "ATM withdrawal",
        )
        insertSplit(
            id = "transfer-target",
            transactionId = "transfer",
            accountId = "cash",
            categoryId = null,
            amount = 2_000L,
        )
        insertLedgerEntry(
            id = "transfer-target-entry",
            transactionId = "transfer",
            splitId = "transfer-target",
            accountId = "cash",
            entryType = LedgerEntryType.DEBIT,
        )
        insertSplit(
            id = "transfer-source",
            transactionId = "transfer",
            accountId = "bank",
            categoryId = null,
            amount = 2_000L,
        )
        insertLedgerEntry(
            id = "transfer-source-entry",
            transactionId = "transfer",
            splitId = "transfer-source",
            accountId = "bank",
            entryType = LedgerEntryType.CREDIT,
        )

        val records = repository.findAll().getOrNull()

        assertNotNull(records)
        assertEquals(listOf("transfer", "income"), records.map { it.id })
        assertEquals(setOf("Bank", "Cash"), records.first().splits.map { it.accountName }.toSet())
        assertEquals("Salary", records.last().splits.single().categoryName)
        assertEquals(LedgerEntryType.CREDIT, records.first().splits.first { it.accountId == "bank" }.entryType)
    }

    private fun insertAccount(
        id: String,
        name: String,
    ) {
        database.accountsQueries.insertAccount(
            id = id,
            name = name,
            type = AccountType.BANK.name,
            currency_code = "USD",
            is_archived = 0L,
            display_order = 0L,
            created_at = 1L,
            updated_at = 1L,
            entity_version = 1L,
            sync_version = 0L,
            device_id = null,
            updated_by = null,
            deleted_at = null,
        )
    }

    private fun insertCategory(
        id: String,
        name: String,
    ) {
        database.categoriesQueries.insertCategory(
            id = id,
            name = name,
            type = CategoryType.INCOME.name,
            parent_id = null,
            is_default = 0L,
            created_at = 1L,
            updated_at = 1L,
            entity_version = 1L,
            sync_version = 0L,
            device_id = null,
            deleted_at = null,
        )
    }

    private fun insertTransaction(
        id: String,
        timestamp: Long,
        type: TransactionType,
        description: String,
    ) {
        database.transactionsQueries.insertTransaction(
            id = id,
            timestamp = timestamp,
            description = description,
            type = type.name,
            merchant_id = null,
            created_by = "MANUAL",
            is_recurring = 0L,
            created_at = timestamp,
            updated_at = timestamp,
            entity_version = 1L,
            sync_version = 0L,
            device_id = null,
            updated_by = null,
            deleted_at = null,
        )
    }

    private fun insertSplit(
        id: String,
        transactionId: String,
        accountId: String,
        categoryId: String?,
        amount: Long,
    ) {
        database.transactionsQueries.insertTransactionSplit(
            id = id,
            transaction_id = transactionId,
            account_id = accountId,
            category_id = categoryId,
            amount = amount,
            notes = null,
            created_at = 1L,
        )
    }

    private fun insertLedgerEntry(
        id: String,
        transactionId: String,
        splitId: String,
        accountId: String,
        entryType: LedgerEntryType,
    ) {
        database.ledgerQueries.insertLedgerEntry(
            id = id,
            transaction_id = transactionId,
            split_id = splitId,
            account_id = accountId,
            amount = 1L,
            entry_type = entryType.name,
            source_type = LedgerSourceType.TRANSACTION.name,
            description = null,
            created_at = 1L,
        )
    }
}
