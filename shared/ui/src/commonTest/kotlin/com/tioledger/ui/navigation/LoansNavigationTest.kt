package com.tioledger.ui.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LoansNavigationTest {
    @Test
    fun loansListAndTypedDetailsRoutesAreStable() {
        val listDestination = RootRoute.Main(MainRoute.Loans)
        val detailsRoute = MainRoute.LoanDetails("loan-id")
        val detailsDestination = RootRoute.Main(detailsRoute)

        assertEquals("main/loans", listDestination.path)
        assertEquals("loans/loan-id", detailsRoute.path)
        assertEquals("main/loans/loan-id", detailsDestination.path)
        assertEquals("loan-id", detailsRoute.loanId)
        assertTrue(MainRoute.Loans in TioNavigationGraphs.main.routes)
        assertFalse(MainRoute.Loans in TioNavigationGraphs.main.bottomNavigationRoutes)
        assertFalse(detailsRoute in TioNavigationGraphs.main.routes)
    }
}
