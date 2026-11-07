package com.tioledger.ui.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TioNavigationGraphsTest {
    @Test
    fun rootGraphStartsAtSplashAndLinksToTransactionsMainEntry() {
        val root = TioNavigationGraphs.root

        assertEquals(RootRoute.Splash, root.startRoute)
        assertEquals(MainRoute.Transactions, root.mainEntry.destination)
        assertTrue(root.routes.contains(root.mainEntry))
    }

    @Test
    fun mainGraphPreparesAllTopLevelRoutes() {
        val main = TioNavigationGraphs.main

        assertEquals(MainRoute.Transactions, main.startRoute)
        assertEquals(
            listOf(
                MainRoute.Dashboard,
                MainRoute.Accounts,
                MainRoute.AddAccount,
                MainRoute.Transactions,
                MainRoute.SmsTransactionReview,
                MainRoute.Categories,
                MainRoute.IncomeCategories,
                MainRoute.ExpenseCategories,
                MainRoute.Budgets,
                MainRoute.Reports,
                MainRoute.More,
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
                MainRoute.PcManager,
                MainRoute.Backup,
                MainRoute.Style,
                MainRoute.Help,
                MainRoute.Feedback,
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
                MainRoute.Transactions,
                MainRoute.Reports,
                MainRoute.Accounts,
                MainRoute.More,
            ),
            main.bottomNavigationRoutes,
        )
        assertTrue(main.bottomNavigationRoutes.all { it in main.routes })
    }

    @Test
    fun settingsRouteRemainsOutsideCanonicalBottomNavigation() {
        assertFalse(MainRoute.Settings in TioNavigationGraphs.main.bottomNavigationRoutes)
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
