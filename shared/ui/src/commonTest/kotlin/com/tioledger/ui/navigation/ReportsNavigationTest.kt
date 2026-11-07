package com.tioledger.ui.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReportsNavigationTest {
    @Test
    fun reportsRouteIsRegisteredAsTheStatsPrimaryDestination() {
        val destination = RootRoute.Main(MainRoute.Reports)

        assertEquals("main/reports", destination.path)
        assertEquals(MainRoute.Reports, destination.destination)
        assertTrue(MainRoute.Reports in TioNavigationGraphs.main.routes)
        assertEquals("Stats", MainRoute.Reports.title)
        assertTrue(MainRoute.Reports in TioNavigationGraphs.main.bottomNavigationRoutes)
    }
}
