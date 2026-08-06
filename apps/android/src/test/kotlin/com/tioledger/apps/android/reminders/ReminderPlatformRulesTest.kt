package com.tioledger.apps.android.reminders

import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderPlatformRulesTest {
    @Test
    fun permissionStatusDistinguishesFirstRequestDenialAndRevocation() {
        assertEquals(
            AndroidNotificationPermissionStatus.NOT_REQUIRED,
            resolveNotificationPermissionStatus(
                requiresRuntimePermission = false,
                granted = false,
                history = NotificationPermissionHistory(false, false),
            ),
        )
        assertEquals(
            AndroidNotificationPermissionStatus.NOT_REQUESTED,
            resolveNotificationPermissionStatus(
                requiresRuntimePermission = true,
                granted = false,
                history = NotificationPermissionHistory(false, false),
            ),
        )
        assertEquals(
            AndroidNotificationPermissionStatus.DENIED,
            resolveNotificationPermissionStatus(
                requiresRuntimePermission = true,
                granted = false,
                history = NotificationPermissionHistory(true, false),
            ),
        )
        assertEquals(
            AndroidNotificationPermissionStatus.REVOKED,
            resolveNotificationPermissionStatus(
                requiresRuntimePermission = true,
                granted = false,
                history = NotificationPermissionHistory(true, true),
            ),
        )
        assertEquals(
            AndroidNotificationPermissionStatus.GRANTED,
            resolveNotificationPermissionStatus(
                requiresRuntimePermission = true,
                granted = true,
                history = NotificationPermissionHistory(false, false),
            ),
        )
    }

    @Test
    fun budgetReceiptPruningKeepsNewestEntriesDeterministically() {
        val receiptTimestamps =
            mapOf(
                "old" to 1L,
                "middle" to 2L,
                "new-b" to 3L,
                "new-a" to 3L,
            )

        assertEquals(
            listOf("middle", "old"),
            budgetReceiptKeysToPrune(receiptTimestamps, maxEntries = 2),
        )
    }
}
