package com.tioledger.ui.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TioNavigationGraphsTest {
    @Test
    fun rootGraphStartsAtSplashAndContainsMainRoute() {
        val root = TioNavigationGraphs.root

        assertEquals(TioRoute.Splash, root.startRoute)
        assertTrue(root.routes.contains(TioRoute.Main))
    }

    @Test
    fun mainGraphStartsAtAccountsRoute() {
        val main = TioNavigationGraphs.main

        assertEquals(TioRoute.Accounts, main.startRoute)
        assertTrue(main.routes.contains(TioRoute.Accounts))
    }

    @Test
    fun routePathsAreUnique() {
        val paths = (TioNavigationGraphs.root.routes + TioNavigationGraphs.main.routes).map { it.path }

        assertEquals(paths.size, paths.toSet().size)
    }
}
