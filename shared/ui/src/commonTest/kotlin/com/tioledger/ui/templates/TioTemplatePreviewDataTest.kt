package com.tioledger.ui.templates

import com.tioledger.domain.model.AccountType
import com.tioledger.ui.accounts.AccountsPreviewData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TioTemplatePreviewDataTest {
    @Test
    fun navigationPreviewDataHasSingleSelectedItem() {
        val selectedCount = TioTemplatePreviewData.navigationItems.count { it.selected }

        assertEquals(1, selectedCount)
    }

    @Test
    fun templatePreviewDataUsesDisplayOnlyAmounts() {
        assertTrue(TioTemplatePreviewData.summaryItems.all { it.value.isNotBlank() })
        assertTrue(TioTemplatePreviewData.listItems.all { it.title.isNotBlank() })
    }

    @Test
    fun accountsPreviewStatesCoverReferenceScenarios() {
        assertTrue(AccountsPreviewData.empty.isEmpty)
        assertEquals(1, AccountsPreviewData.oneAccount.groups.size)
        assertTrue(AccountsPreviewData.multipleAccounts.groups.size >= 3)
        assertTrue(AccountsPreviewData.largeBalance.summary.total.isNotBlank())
    }

    @Test
    fun mixedCurrencyPreviewHasOneHeaderPerTypeAndSeparateCurrencyAmounts() {
        val state = AccountsPreviewData.mixedCurrency

        assertEquals(listOf(AccountType.CASH, AccountType.CREDIT_CARD), state.groups.map { it.type })
        assertEquals(listOf("INR 1,250.00", "USD 250.00"), state.groups.first().totals)
        assertEquals(listOf("INR 180.00", "USD 80.00"), state.groups.last().totals)
        state.groups.forEach { group ->
            assertEquals(listOf("INR", "USD"), group.accounts.map { it.currencyCode })
            assertEquals(group.totals, group.accounts.map { it.balance })
        }
        assertTrue(state.groups.last().accounts.all { it.isLiability })
        assertEquals("Multiple currencies", state.summary.currencyLabel)
        assertEquals("INR 1,070.00", state.summary.total)
    }

    @Test
    fun mixedCurrencyZeroPreviewKeepsBothCurrenciesWithoutChangingNonzeroFixture() {
        val state = AccountsPreviewData.mixedCurrencyZero

        assertEquals(2, state.groups.size)
        state.groups.forEach { group ->
            assertEquals(listOf("INR 0.00", "USD 0.00"), group.totals)
            assertEquals(group.totals, group.accounts.map { it.balance })
        }
        assertEquals("INR 0.00", state.summary.total)
        assertEquals(listOf("INR 1,250.00", "USD 250.00"), AccountsPreviewData.mixedCurrency.groups.first().totals)
    }

    @Test
    fun mixedCurrencyLongSignedPreviewKeepsSignCurrencyAndRowConsistency() {
        val group = AccountsPreviewData.mixedCurrencyLargeSigned.groups.single()

        assertEquals(AccountType.CASH, group.type)
        assertEquals(listOf("-INR 1234567890.12", "USD 9876543210.98"), group.totals)
        assertEquals(group.totals, group.accounts.map { it.balance })
        assertEquals("-INR 1234567890.12", AccountsPreviewData.mixedCurrencyLargeSigned.summary.total)
        assertEquals(2, AccountsPreviewData.mixedCurrency.groups.size)
    }
}
