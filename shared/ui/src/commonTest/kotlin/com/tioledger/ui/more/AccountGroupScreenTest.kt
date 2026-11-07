package com.tioledger.ui.more

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AccountGroupScreenTest {
    @Test
    fun defaultAccountGroupsContainOnlyThreePermanentOptions() {
        assertEquals(3, DEFAULT_ACCOUNT_GROUPS.size)

        val titles = DEFAULT_ACCOUNT_GROUPS.map { it.title }
        assertEquals(listOf("Cash", "Accounts", "Credit Card"), titles)

        assertTrue(DEFAULT_ACCOUNT_GROUPS.all { it.isPermanent })
    }

    @Test
    fun customAccountGroupsAreNotPermanentAndCanBeDeleted() {
        val customGroup =
            AccountGroupEntry(
                id = "group_investments_4",
                title = "Investments",
                isPermanent = false,
            )

        assertFalse(customGroup.isPermanent)

        val groups = DEFAULT_ACCOUNT_GROUPS + customGroup
        assertEquals(4, groups.size)

        val filtered = groups.filterNot { it.id == customGroup.id }
        assertEquals(3, filtered.size)
        assertEquals(DEFAULT_ACCOUNT_GROUPS, filtered)
    }

    @Test
    fun creditCardHasCardSubtitle() {
        val creditCard = DEFAULT_ACCOUNT_GROUPS.first { it.id == "credit_card" }
        assertEquals("Credit Card", creditCard.title)
        assertEquals("Card", creditCard.subtitle)
    }
}
