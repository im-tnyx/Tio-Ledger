package com.tioledger.ui.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MoreNavigationTest {
    @Test
    fun moreRouteIsRegisteredAsTheLastPrimaryDestination() {
        val destination = RootRoute.Main(MainRoute.More)

        assertEquals("main/more", destination.path)
        assertEquals(MainRoute.More, destination.destination)
        assertTrue(MainRoute.More in TioNavigationGraphs.main.routes)
        assertEquals(MainRoute.More, TioNavigationGraphs.main.bottomNavigationRoutes.last())
    }

    @Test
    fun moreSettingsRoutesStayOutsidePrimaryNavigation() {
        assertTrue(MainRoute.Configuration in TioNavigationGraphs.main.routes)
        assertTrue(MainRoute.IncomeCategories in TioNavigationGraphs.main.routes)
        assertTrue(MainRoute.ExpenseCategories in TioNavigationGraphs.main.routes)
        assertTrue(MainRoute.AccountSettings in TioNavigationGraphs.main.routes)
        assertTrue(MainRoute.AccountManager in TioNavigationGraphs.main.routes)
        assertTrue(MainRoute.AccountGroup in TioNavigationGraphs.main.routes)
        assertTrue(MainRoute.TransactionSettings in TioNavigationGraphs.main.routes)
        assertTrue(MainRoute.CurrencySettings in TioNavigationGraphs.main.routes)
        assertTrue(MainRoute.SubCurrencySettings in TioNavigationGraphs.main.routes)
        assertTrue(MainRoute.PasscodeSettings in TioNavigationGraphs.main.routes)
        assertTrue(MainRoute.AlarmSettings in TioNavigationGraphs.main.routes)
        assertTrue(MainRoute.CopyPasteSettings in TioNavigationGraphs.main.routes)
        assertTrue(MainRoute.Configuration !in TioNavigationGraphs.main.bottomNavigationRoutes)
    }
}
