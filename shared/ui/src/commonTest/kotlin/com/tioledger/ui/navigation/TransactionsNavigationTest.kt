package com.tioledger.ui.navigation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TransactionsNavigationTest {
    @Test
    fun transactionsIsBottomDestinationAndEntryIsNestedDestination() {
        assertTrue(TioNavigationGraphs.main.bottomNavigationRoutes.contains(MainRoute.Transactions))
        assertTrue(TioNavigationGraphs.main.routes.contains(MainRoute.TransactionEntry))
        assertFalse(TioNavigationGraphs.main.bottomNavigationRoutes.contains(MainRoute.TransactionEntry))
    }
}
