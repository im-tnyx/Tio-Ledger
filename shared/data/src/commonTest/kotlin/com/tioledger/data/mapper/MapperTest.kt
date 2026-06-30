package com.tioledger.data.mapper

import com.tioledger.core.model.CurrencyCode
import com.tioledger.core.model.Money
import com.tioledger.data.resolver.SystemAccountResolver
import com.tioledger.database.Accounts
import com.tioledger.database.Categories
import com.tioledger.database.Transactions
import com.tioledger.database.query.SelectLedgerEntriesByAccountId
import com.tioledger.database.query.SelectLedgerEntriesByTransactionId
import com.tioledger.database.query.SelectSplitsByTransactionId
import com.tioledger.domain.model.AccountType
import com.tioledger.domain.model.CategoryType
import com.tioledger.domain.model.LedgerClass
import com.tioledger.domain.model.LedgerEntryType
import com.tioledger.domain.model.LedgerSourceType
import com.tioledger.domain.model.PostingTarget
import com.tioledger.domain.model.SYSTEM_INCOME_ID
import com.tioledger.domain.model.TransactionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapperTest {
    @Test
    fun testAccountsMapping() {
        val row =
            Accounts(
                id = "acc-1",
                name = "Test Wallet",
                type = "WALLET",
                currency_code = "USD",
                is_archived = 0L,
                display_order = 5L,
                created_at = 1000L,
                updated_at = 1000L,
                entity_version = 1L,
                sync_version = 0L,
                device_id = "device-abc",
                updated_by = "user-1",
                deleted_at = null,
            )

        val domain = row.toDomain()

        assertEquals("acc-1", domain.id)
        assertEquals("Test Wallet", domain.name)
        assertEquals(AccountType.WALLET, domain.type)
        assertEquals("USD", domain.currencyCode)
        assertEquals(false, domain.isArchived)
        assertEquals(5, domain.displayOrder)
        assertEquals(1000L, domain.createdAt)
        assertEquals("device-abc", domain.deviceId)
    }

    @Test
    fun testCategoriesMapping() {
        val row =
            Categories(
                id = "cat-1",
                name = "Food",
                type = "EXPENSE",
                parent_id = "parent-id",
                is_default = 1L,
                created_at = 1000L,
                updated_at = 1000L,
                entity_version = 1L,
                sync_version = 0L,
                device_id = "device-1",
                deleted_at = null,
            )

        val domain = row.toDomain()

        assertEquals("cat-1", domain.id)
        assertEquals("Food", domain.name)
        assertEquals(CategoryType.EXPENSE, domain.type)
        assertEquals("parent-id", domain.parentId)
        assertEquals(true, domain.isDefault)
    }

    @Test
    fun testTransactionsMapping() {
        val row =
            Transactions(
                id = "tx-1",
                timestamp = 1500L,
                description = "Buying Coffee",
                type = "EXPENSE",
                merchant_id = "merchant-1",
                created_by = "MANUAL",
                is_recurring = 0L,
                created_at = 1500L,
                updated_at = 1500L,
                entity_version = 1L,
                sync_version = 0L,
                device_id = "device-1",
                updated_by = "user-1",
                deleted_at = null,
            )

        val domain = row.toDomain()

        assertEquals("tx-1", domain.id)
        assertEquals(1500L, domain.timestamp)
        assertEquals("Buying Coffee", domain.description)
        assertEquals(TransactionType.EXPENSE, domain.type)
        assertEquals(false, domain.isRecurring)
    }

    @Test
    fun testSelectSplitsMapping() {
        val row =
            SelectSplitsByTransactionId(
                id = "split-1",
                transaction_id = "tx-1",
                account_id = "acc-1",
                category_id = "cat-1",
                amount = 1000L,
                notes = "Note",
                created_at = 1500L,
                currency_code = "USD",
            )

        val domain = row.toDomain()

        assertEquals("split-1", domain.id)
        assertEquals("acc-1", domain.accountId)
        assertEquals("cat-1", domain.categoryId)
        assertEquals(Money(1000L, CurrencyCode("USD")), domain.amount)
        assertEquals("Note", domain.notes)
    }

    @Test
    fun testSystemAccountResolutionMapping() {
        val resolver = SystemAccountResolver

        assertTrue(resolver.isSystemAccountId(SYSTEM_INCOME_ID))

        val virtualIncome = resolver.toVirtualTarget(SYSTEM_INCOME_ID, "cat-1")
        assertEquals(PostingTarget.Virtual("cat-1", LedgerClass.INCOME), virtualIncome)

        val rowVirtual =
            SelectLedgerEntriesByAccountId(
                id = "le-1",
                transaction_id = "tx-1",
                split_id = "split-1",
                account_id = SYSTEM_INCOME_ID,
                amount = 2500L,
                entry_type = "CREDIT",
                source_type = "TRANSACTION",
                description = "Income entry",
                created_at = 1200L,
                currency_code = "USD",
                account_type = "WALLET",
                split_category_id = "cat-income",
            )

        val entryVirtual = rowVirtual.toDomain()
        assertEquals(PostingTarget.Virtual("cat-income", LedgerClass.INCOME), entryVirtual.target)
        assertEquals(Money(2500L, CurrencyCode("USD")), entryVirtual.amount)
        assertEquals(LedgerEntryType.CREDIT, entryVirtual.entryType)
        assertEquals(LedgerSourceType.TRANSACTION, entryVirtual.sourceType)

        val rowAccount =
            SelectLedgerEntriesByTransactionId(
                id = "le-2",
                transaction_id = "tx-1",
                split_id = "split-2",
                account_id = "acc-bank",
                amount = 2500L,
                entry_type = "DEBIT",
                source_type = "TRANSACTION",
                description = "Asset entry",
                created_at = 1200L,
                currency_code = "USD",
                account_type = "BANK",
                split_category_id = null,
            )

        val entryAccount = rowAccount.toDomain()
        assertEquals(PostingTarget.Account("acc-bank", LedgerClass.ASSET), entryAccount.target)
    }
}
