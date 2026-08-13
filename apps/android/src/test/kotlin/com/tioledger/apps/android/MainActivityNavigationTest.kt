package com.tioledger.apps.android

import com.tioledger.ui.navigation.MainRoute
import com.tioledger.ui.navigation.RootRoute
import com.tioledger.ui.navigation.TioNavigationGraphs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MainActivityNavigationTest {
    @Test
    fun systemBackFromSettingsReturnsAccountsMainEntry() {
        assertEquals(
            TioNavigationGraphs.root.mainEntry,
            systemBackTargetOrNull(RootRoute.Main(MainRoute.Settings)),
        )
    }

    @Test
    fun systemBackDoesNotOverridePrimaryNavigationRoutes() {
        listOf(
            MainRoute.Dashboard,
            MainRoute.Accounts,
            MainRoute.Transactions,
            MainRoute.Categories,
            MainRoute.Budgets,
        ).forEach { route ->
            assertNull(systemBackTargetOrNull(RootRoute.Main(route)))
        }
    }

    @Test
    fun systemBackDoesNotOverrideOtherNonPrimaryRoutes() {
        listOf(
            MainRoute.Reports,
            MainRoute.Loans,
            MainRoute.TransactionEntry,
            MainRoute.SmsTransactionReview,
            MainRoute.LoanDetails("loan-1"),
        ).forEach { route ->
            assertNull(systemBackTargetOrNull(RootRoute.Main(route)))
        }
    }
}
