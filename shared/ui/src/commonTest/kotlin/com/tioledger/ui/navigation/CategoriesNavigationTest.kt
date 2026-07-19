package com.tioledger.ui.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CategoriesNavigationTest {
    @Test
    fun categoriesRouteIsRegisteredInMainGraph() {
        val destination = RootRoute.Main(MainRoute.Categories)

        assertEquals("main/categories", destination.path)
        assertEquals(MainRoute.Categories, destination.destination)
        assertTrue(MainRoute.Categories in TioNavigationGraphs.main.routes)
        assertTrue(MainRoute.Categories in TioNavigationGraphs.main.bottomNavigationRoutes)
    }
}
