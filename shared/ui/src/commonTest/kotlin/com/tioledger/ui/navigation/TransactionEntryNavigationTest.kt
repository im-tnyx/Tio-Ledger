package com.tioledger.ui.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class TransactionEntryNavigationTest {
    @Test
    fun transactionEntryReturnsToTransactionsDestination() {
        val destination = RootRoute.Main(MainRoute.Transactions)

        assertEquals("main/transactions", destination.path)
        assertEquals(MainRoute.Transactions, destination.destination)
    }
}
