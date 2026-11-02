package com.tioledger.ui.navigation

import kotlin.test.Test
import kotlin.test.assertTrue

class TransactionsNavigationTest {
    @Test
    fun transactionsIsBottomDestination() {
        assertTrue(TioNavigationGraphs.main.bottomNavigationRoutes.contains(MainRoute.Transactions))
    }
}
