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

    @Test
    fun accountGroupPolicyOptionsSupportRequiredTypes() {
        assertEquals(4, AccountGroupPolicyOption.entries.size)
        assertEquals(AccountGroupPolicy.BANK, AccountGroupPolicyOption.GENERAL.policy)
        assertEquals("General", AccountGroupPolicyOption.GENERAL.label)

        assertEquals(AccountGroupPolicy.CREDIT_CARD, AccountGroupPolicyOption.CARD.policy)
        assertEquals("Card", AccountGroupPolicyOption.CARD.label)

        assertEquals(AccountGroupPolicy.LOAN, AccountGroupPolicyOption.LOAN.policy)
        assertEquals("Loan", AccountGroupPolicyOption.LOAN.label)

        assertEquals(AccountGroupPolicy.INVESTMENT, AccountGroupPolicyOption.INVESTMENT.policy)
        assertEquals("Investment", AccountGroupPolicyOption.INVESTMENT.label)
    }

    @Test
    fun updateGroupUpdatesTitleAndPolicyForCustomGroup() {
        val store = AccountGroupStore()
        val custom = store.addGroup("My Loans", AccountGroupPolicy.LOAN)!!
        assertEquals("Loan", custom.subtitle)

        val updated = store.updateGroup(custom.id, "My Card", AccountGroupPolicy.CREDIT_CARD)
        assertTrue(updated)

        val retrieved = store.groupForId(custom.id)!!
        assertEquals("My Card", retrieved.title)
        assertEquals(AccountGroupPolicy.CREDIT_CARD, retrieved.policy)
        assertEquals("Card", retrieved.subtitle)
    }

    @Test
    fun updateGroupDoesNotChangePolicyForPermanentGroup() {
        val store = AccountGroupStore()
        val cash = store.groupForId("cash")!!
        assertTrue(cash.isPermanent)

        val updated = store.updateGroup(cash.id, "Petty Cash", AccountGroupPolicy.CREDIT_CARD)
        assertTrue(updated)

        val retrieved = store.groupForId("cash")!!
        assertEquals("Petty Cash", retrieved.title)
        assertEquals(AccountGroupPolicy.CASH, retrieved.policy)
    }
}
