package com.tioledger.ui.templates

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
}
