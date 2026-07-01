package com.tioledger.ui.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TioNavigationGraphsTest {
    @Test
    fun rootGraphStartsAtSplashAndLinksToAccountsMainEntry() {
        val root = TioNavigationGraphs.root

        assertEquals(RootRoute.Splash, root.startRoute)
        assertEquals(MainRoute.Accounts, root.mainEntry.destination)
        assertTrue(root.routes.contains(root.mainEntry))
    }

    @Test
    fun mainGraphPreparesAllTopLevelRoutes() {
        val main = TioNavigationGraphs.main

        assertEquals(MainRoute.Accounts, main.startRoute)
        assertEquals(
            listOf(
                MainRoute.Dashboard,
                MainRoute.Accounts,
                MainRoute.Transactions,
                MainRoute.Categories,
                MainRoute.Reports,
                MainRoute.Loans,
                MainRoute.Settings,
            ),
            main.routes,
        )
    }

    @Test
    fun bottomNavigationRoutesStayWithinMainGraph() {
        val main = TioNavigationGraphs.main

        assertEquals(
            listOf(
                MainRoute.Dashboard,
                MainRoute.Accounts,
                MainRoute.Transactions,
                MainRoute.Categories,
                MainRoute.Reports,
            ),
            main.bottomNavigationRoutes,
        )
        assertTrue(main.bottomNavigationRoutes.all { it in main.routes })
    }

    @Test
    fun preparedRoutePathsAreUnique() {
        val paths =
            buildList {
                addAll(TioNavigationGraphs.root.routes.map { it.path })
                addAll(TioNavigationGraphs.main.routes.map { it.path })
            }

        assertEquals(paths.size, paths.toSet().size)
    }
}
