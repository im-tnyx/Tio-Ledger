package com.tioledger.ui.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class MainBottomNavigationModelTest {
    @Test
    fun usesCanonicalRouteOrderAndMetadata() {
        val model = TioNavigationGraphs.main.bottomNavigationModel(MainRoute.Dashboard)
        val expectedRoutes = TioNavigationGraphs.main.bottomNavigationRoutes

        assertEquals(expectedRoutes, model.routes)
        assertEquals(expectedRoutes.map(MainRoute::title), model.items.map { it.label })
        assertEquals(expectedRoutes.map(MainRoute::icon), model.items.map { it.icon })
    }

    @Test
    fun selectsExactlyTheActivePrimaryRoute() {
        TioNavigationGraphs.main.bottomNavigationRoutes.forEach { currentRoute ->
            val model = TioNavigationGraphs.main.bottomNavigationModel(currentRoute)

            assertEquals(1, model.items.count { it.selected })
            assertEquals(currentRoute.title, model.items.single { it.selected }.label)
        }
    }

    @Test
    fun doesNotSelectPrimaryItemForNonPrimaryRoute() {
        listOf(MainRoute.Reports, MainRoute.Loans, MainRoute.Settings).forEach { currentRoute ->
            val model = TioNavigationGraphs.main.bottomNavigationModel(currentRoute)

            assertFalse(model.items.any { it.selected })
        }
    }

    @Test
    fun forwardsEachSelectedItemToItsTypedRoute() {
        val model = TioNavigationGraphs.main.bottomNavigationModel(MainRoute.Accounts)

        model.items.forEachIndexed { index, item ->
            var navigatedRoute: MainRoute? = null

            model.navigate(item) { route ->
                navigatedRoute = route
            }

            assertEquals(model.routes[index], navigatedRoute)
        }
    }

    @Test
    fun ignoresItemOutsideTheCanonicalModel() {
        val model = TioNavigationGraphs.main.bottomNavigationModel(MainRoute.Accounts)
        val unknownItem = model.items.first().copy(label = "Unknown")
        var navigatedRoute: MainRoute? = null

        model.navigate(unknownItem) { route ->
            navigatedRoute = route
        }

        assertNull(navigatedRoute)
    }
}
