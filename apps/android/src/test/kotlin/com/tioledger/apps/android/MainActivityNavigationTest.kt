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
    fun systemBackFromTransactionEntryReturnsTransactions() {
        assertEquals(
            RootRoute.Main(MainRoute.Transactions),
            systemBackTargetOrNull(RootRoute.Main(MainRoute.TransactionEntry)),
        )
    }

    @Test
    fun systemBackFromSmsReviewReturnsTransactions() {
        assertEquals(
            RootRoute.Main(MainRoute.Transactions),
            systemBackTargetOrNull(RootRoute.Main(MainRoute.SmsTransactionReview)),
        )
    }

    @Test
    fun systemBackFromLoanDetailsReturnsLoans() {
        assertEquals(
            RootRoute.Main(MainRoute.Loans),
            systemBackTargetOrNull(RootRoute.Main(MainRoute.LoanDetails("loan-1"))),
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
    fun systemBackExitsFromTopLevelNonPrimaryRoutes() {
        listOf(
            MainRoute.Reports,
            MainRoute.Loans,
        ).forEach { route ->
            assertNull(systemBackTargetOrNull(RootRoute.Main(route)))
        }
    }
}
