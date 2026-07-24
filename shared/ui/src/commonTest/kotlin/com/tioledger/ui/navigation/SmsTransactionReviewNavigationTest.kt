package com.tioledger.ui.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SmsTransactionReviewNavigationTest {
    @Test
    fun SMSReviewUsesTypedRouteWithoutMessageContent() {
        val route = MainRoute.SmsTransactionReview

        assertEquals("transactions/sms-review", route.path)
        assertFalse(route.path.contains("message", ignoreCase = true))
        assertTrue(route in TioNavigationGraphs.main.routes)
        assertFalse(route in TioNavigationGraphs.main.bottomNavigationRoutes)
        assertEquals("main/transactions/sms-review", RootRoute.Main(route).path)
    }
}
