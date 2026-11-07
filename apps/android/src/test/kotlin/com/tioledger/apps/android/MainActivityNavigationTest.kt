package com.tioledger.apps.android

import com.tioledger.ui.navigation.MainRoute
import com.tioledger.ui.navigation.RootRoute
import com.tioledger.ui.navigation.TioNavigationGraphs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MainActivityNavigationTest {
    @Test
    fun systemBackFromConfigurationReturnsMore() {
        assertEquals(
            RootRoute.Main(MainRoute.More),
            systemBackTargetOrNull(RootRoute.Main(MainRoute.Configuration)),
        )
    }

    @Test
    fun systemBackFromMoreEntriesReturnsMore() {
        assertEquals(
            RootRoute.Main(MainRoute.More),
            systemBackTargetOrNull(RootRoute.Main(MainRoute.Settings)),
        )
        assertEquals(
            RootRoute.Main(MainRoute.More),
            systemBackTargetOrNull(RootRoute.Main(MainRoute.Categories)),
        )
        assertEquals(
            RootRoute.Main(MainRoute.More),
            systemBackTargetOrNull(RootRoute.Main(MainRoute.Budgets)),
        )
        assertEquals(
            RootRoute.Main(MainRoute.More),
            systemBackTargetOrNull(RootRoute.Main(MainRoute.CurrencySettings)),
        )
    }

    @Test
    fun systemBackFromAddAccountReturnsAccounts() {
        assertEquals(
            RootRoute.Main(MainRoute.Accounts),
            systemBackTargetOrNull(RootRoute.Main(MainRoute.AddAccount)),
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
            MainRoute.Transactions,
            MainRoute.Reports,
            MainRoute.Accounts,
            MainRoute.More,
        ).forEach { route ->
            assertNull(systemBackTargetOrNull(RootRoute.Main(route)))
        }
    }

    @Test
    fun systemBackFromLoansReturnsAccountsMainEntry() {
        assertEquals(
            RootRoute.Main(MainRoute.Transactions),
            TioNavigationGraphs.root.mainEntry,
        )
        assertEquals(
            TioNavigationGraphs.root.mainEntry,
            systemBackTargetOrNull(RootRoute.Main(MainRoute.Loans)),
        )
    }

    @Test
    fun systemBackFromReportsKeepsDefaultExit() {
        assertNull(systemBackTargetOrNull(RootRoute.Main(MainRoute.Reports)))
    }
}
