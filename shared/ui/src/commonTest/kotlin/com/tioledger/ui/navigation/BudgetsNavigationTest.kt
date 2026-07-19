package com.tioledger.ui.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BudgetsNavigationTest {
    @Test
    fun budgetsRouteIsRegisteredAsPrimaryDestination() {
        val destination = RootRoute.Main(MainRoute.Budgets)

        assertEquals("main/budgets", destination.path)
        assertEquals(MainRoute.Budgets, destination.destination)
        assertTrue(MainRoute.Budgets in TioNavigationGraphs.main.routes)
        assertTrue(MainRoute.Budgets in TioNavigationGraphs.main.bottomNavigationRoutes)
        assertFalse(MainRoute.Reports in TioNavigationGraphs.main.bottomNavigationRoutes)
        assertTrue(MainRoute.Reports in TioNavigationGraphs.main.routes)
    }
}
