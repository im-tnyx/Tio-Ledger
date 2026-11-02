package com.tioledger.ui.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class MainBottomNavigationModelTest {
    @Test
    fun usesCanonicalRouteOrderAndMetadata() {
        val model = TioNavigationGraphs.main.bottomNavigationModel(MainRoute.Transactions)
        val expectedRoutes =
            listOf(
                MainRoute.Transactions,
                MainRoute.Reports,
                MainRoute.Accounts,
                MainRoute.More,
            )

        assertEquals(expectedRoutes, TioNavigationGraphs.main.bottomNavigationRoutes)
        assertEquals(expectedRoutes, model.routes)
        assertEquals(expectedRoutes.map(MainRoute::title), model.items.map { it.label })
        assertEquals(
            listOf("Transactions", "Stats", "Accounts", "More"),
            model.items.map { it.label },
        )
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
    fun selectsMoreForRoutesOwnedByMore() {
        listOf(
            MainRoute.Categories,
            MainRoute.IncomeCategories,
            MainRoute.ExpenseCategories,
            MainRoute.Budgets,
            MainRoute.Settings,
            MainRoute.Configuration,
            MainRoute.AccountSettings,
            MainRoute.AccountGroup,
            MainRoute.BudgetSettings,
            MainRoute.TransactionSettings,
            MainRoute.RepeatSettings,
            MainRoute.CurrencySettings,
            MainRoute.SubCurrencySettings,
            MainRoute.PasscodeSettings,
            MainRoute.AlarmSettings,
            MainRoute.ApplicationIconSettings,
            MainRoute.LanguageSettings,
            MainRoute.AdTrackingSettings,
            MainRoute.CopyPasteSettings,
        ).forEach { currentRoute ->
            val model = TioNavigationGraphs.main.bottomNavigationModel(currentRoute)

            assertEquals("More", model.items.single { it.selected }.label)
        }
    }

    @Test
    fun doesNotSelectPrimaryItemForUnownedNonPrimaryRoute() {
        val model = TioNavigationGraphs.main.bottomNavigationModel(MainRoute.Loans)

        assertFalse(model.items.any { it.selected })
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
