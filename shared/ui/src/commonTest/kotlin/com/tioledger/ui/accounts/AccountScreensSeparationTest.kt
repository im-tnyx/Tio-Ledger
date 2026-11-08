package com.tioledger.ui.accounts

import com.tioledger.ui.navigation.MainRoute
import com.tioledger.ui.navigation.TioNavigationGraphs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AccountScreensSeparationTest {
    @Test
    fun accountEditRouteContractIsDefined() {
        val route = MainRoute.AccountEdit("acc-123")
        assertEquals("settings/accounts/edit/acc-123", route.path)
        assertEquals("Edit Account", route.title)
    }

    @Test
    fun accountAddRouteContractIsDefined() {
        val route = MainRoute.AccountAdd
        assertEquals("settings/accounts/add", route.path)
        assertEquals("Add Account", route.title)
        assertTrue(route in TioNavigationGraphs.main.routes)
        assertFalse(route in TioNavigationGraphs.main.bottomNavigationRoutes)
    }

    @Test
    fun accountManagerRouteRemainsInMainRoutes() {
        assertTrue(MainRoute.AccountManager in TioNavigationGraphs.main.routes)
        assertFalse(MainRoute.AccountManager in TioNavigationGraphs.main.bottomNavigationRoutes)
    }

    @Test
    fun accountsRouteRemainsInBottomNavigation() {
        assertTrue(MainRoute.Accounts in TioNavigationGraphs.main.bottomNavigationRoutes)
    }

    @Test
    fun loansRouteIsNonPrimaryEntry() {
        assertTrue(MainRoute.Loans in TioNavigationGraphs.main.routes)
        assertFalse(MainRoute.Loans in TioNavigationGraphs.main.bottomNavigationRoutes)
    }
}
