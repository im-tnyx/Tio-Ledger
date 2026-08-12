package com.tioledger.apps.android.reminders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderPlatformRulesTest {
    @Test
    fun permissionStatusDistinguishesFirstRequestDenialAndRevocation() {
        assertEquals(
            AndroidNotificationPermissionStatus.NOT_REQUIRED,
            resolveNotificationPermissionStatus(
                requiresRuntimePermission = false,
                runtimePermissionGranted = true,
                notificationsEnabled = true,
                history = NotificationPermissionHistory(false, false),
            ),
        )
        assertEquals(
            AndroidNotificationPermissionStatus.NOT_REQUESTED,
            resolveNotificationPermissionStatus(
                requiresRuntimePermission = true,
                runtimePermissionGranted = false,
                notificationsEnabled = false,
                history = NotificationPermissionHistory(false, false),
            ),
        )
        assertEquals(
            AndroidNotificationPermissionStatus.DENIED,
            resolveNotificationPermissionStatus(
                requiresRuntimePermission = true,
                runtimePermissionGranted = false,
                notificationsEnabled = false,
                history = NotificationPermissionHistory(true, false),
            ),
        )
        assertEquals(
            AndroidNotificationPermissionStatus.REVOKED,
            resolveNotificationPermissionStatus(
                requiresRuntimePermission = true,
                runtimePermissionGranted = false,
                notificationsEnabled = false,
                history = NotificationPermissionHistory(true, true),
            ),
        )
        assertEquals(
            AndroidNotificationPermissionStatus.GRANTED,
            resolveNotificationPermissionStatus(
                requiresRuntimePermission = true,
                runtimePermissionGranted = true,
                notificationsEnabled = true,
                history = NotificationPermissionHistory(false, false),
            ),
        )
    }

    @Test
    fun disabledAppNotificationsAreTreatedAsRevoked() {
        assertEquals(
            AndroidNotificationPermissionStatus.REVOKED,
            resolveNotificationPermissionStatus(
                requiresRuntimePermission = false,
                runtimePermissionGranted = true,
                notificationsEnabled = false,
                history = NotificationPermissionHistory(false, false),
            ),
        )
        assertEquals(
            AndroidNotificationPermissionStatus.REVOKED,
            resolveNotificationPermissionStatus(
                requiresRuntimePermission = true,
                runtimePermissionGranted = true,
                notificationsEnabled = false,
                history = NotificationPermissionHistory(true, true),
            ),
        )
    }

    @Test
    fun permissionReconciliationRunsOnlyAfterAnObservedStatusChange() {
        assertFalse(
            shouldReconcileNotificationPermissionChange(
                previous = null,
                current = AndroidNotificationPermissionStatus.NOT_REQUESTED,
            ),
        )
        assertFalse(
            shouldReconcileNotificationPermissionChange(
                previous = AndroidNotificationPermissionStatus.GRANTED,
                current = AndroidNotificationPermissionStatus.GRANTED,
            ),
        )
        assertTrue(
            shouldReconcileNotificationPermissionChange(
                previous = AndroidNotificationPermissionStatus.GRANTED,
                current = AndroidNotificationPermissionStatus.REVOKED,
            ),
        )
        assertTrue(
            shouldReconcileNotificationPermissionChange(
                previous = AndroidNotificationPermissionStatus.DENIED,
                current = AndroidNotificationPermissionStatus.GRANTED,
            ),
        )
    }

    @Test
    fun settingsActionsRequestOnlyBeforeTheFirstPermissionAttempt() {
        assertEquals(
            ReminderSettingsPermissionAction.NONE,
            AndroidNotificationPermissionStatus.NOT_REQUIRED.settingsAction(),
        )
        assertEquals(
            ReminderSettingsPermissionAction.REQUEST_PERMISSION,
            AndroidNotificationPermissionStatus.NOT_REQUESTED.settingsAction(),
        )
        assertEquals(
            ReminderSettingsPermissionAction.NONE,
            AndroidNotificationPermissionStatus.GRANTED.settingsAction(),
        )
        assertEquals(
            ReminderSettingsPermissionAction.OPEN_NOTIFICATION_SETTINGS,
            AndroidNotificationPermissionStatus.DENIED.settingsAction(),
        )
        assertEquals(
            ReminderSettingsPermissionAction.OPEN_NOTIFICATION_SETTINGS,
            AndroidNotificationPermissionStatus.REVOKED.settingsAction(),
        )
    }

    @Test
    fun failedPreferenceWriteRestoresPersistedSnapshotAndShowsFeedback() {
        val persistedSnapshot =
            AndroidReminderSettingsSnapshot(
                preferences =
                    AndroidReminderPreferences(
                        emiRemindersEnabled = false,
                        budgetRemindersEnabled = true,
                    ),
                permissionStatus = AndroidNotificationPermissionStatus.GRANTED,
                runtimePermission = "android.permission.POST_NOTIFICATIONS",
            )

        val outcome =
            resolveReminderPreferenceWrite(
                stored = false,
                persistedSnapshot = persistedSnapshot,
                failureMessage = "Could not save reminder settings.",
            )

        assertEquals(persistedSnapshot, outcome.snapshot)
        assertEquals("Could not save reminder settings.", outcome.errorMessage)
    }

    @Test
    fun successfulPreferenceWriteUsesPersistedSnapshotAndClearsFeedback() {
        val persistedSnapshot =
            AndroidReminderSettingsSnapshot(
                preferences =
                    AndroidReminderPreferences(
                        emiRemindersEnabled = true,
                        budgetRemindersEnabled = false,
                    ),
                permissionStatus = AndroidNotificationPermissionStatus.NOT_REQUIRED,
                runtimePermission = null,
            )

        val outcome =
            resolveReminderPreferenceWrite(
                stored = true,
                persistedSnapshot = persistedSnapshot,
                failureMessage = "Could not save reminder settings.",
            )

        assertEquals(persistedSnapshot, outcome.snapshot)
        assertNull(outcome.errorMessage)
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
