package com.tioledger.apps.android

import com.tioledger.ui.navigation.MainRoute
import com.tioledger.ui.navigation.RootRoute
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
    fun systemBackPreservesAccountSettingsHierarchy() {
        assertEquals(
            RootRoute.Main(MainRoute.More),
            systemBackTargetOrNull(RootRoute.Main(MainRoute.AccountSettings)),
        )
        assertEquals(
            RootRoute.Main(MainRoute.AccountSettings),
            systemBackTargetOrNull(RootRoute.Main(MainRoute.AccountManager)),
        )
        assertEquals(
            RootRoute.Main(MainRoute.AccountManager),
            systemBackTargetOrNull(RootRoute.Main(MainRoute.AccountEdit("acc-1"))),
        )
        assertEquals(
            RootRoute.Main(MainRoute.AccountManager),
            systemBackTargetOrNull(RootRoute.Main(MainRoute.AccountAdd)),
        )
        assertEquals(
            RootRoute.Main(MainRoute.AccountSettings),
            systemBackTargetOrNull(RootRoute.Main(MainRoute.AccountGroup)),
        )
        assertEquals(
            RootRoute.Main(MainRoute.AccountGroup),
            systemBackTargetOrNull(RootRoute.Main(MainRoute.AccountGroupAdd)),
        )
        assertEquals(
            RootRoute.Main(MainRoute.AccountGroup),
            systemBackTargetOrNull(RootRoute.Main(MainRoute.AccountGroupEdit("grp-1"))),
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
    fun systemBackFromLoanDetailsReturnsAccounts() {
        assertEquals(
            RootRoute.Main(MainRoute.Accounts),
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
    fun systemBackFromLoansReturnsAccounts() {
        assertEquals(
            RootRoute.Main(MainRoute.Accounts),
            systemBackTargetOrNull(RootRoute.Main(MainRoute.Loans)),
        )
    }

    @Test
    fun systemBackFromReportsKeepsDefaultExit() {
        assertNull(systemBackTargetOrNull(RootRoute.Main(MainRoute.Reports)))
    }
}
