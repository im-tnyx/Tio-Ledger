package com.tioledger.apps.android.reminders

import android.content.Context

data class AndroidReminderSettingsSnapshot(
    val preferences: AndroidReminderPreferences,
    val permissionStatus: AndroidNotificationPermissionStatus,
    val runtimePermission: String?,
)

class AndroidReminderSettingsService(
    private val context: Context,
    private val stateStore: ReminderPlatformStateStore,
    private val permissionController: AndroidNotificationPermissionController,
) {
    fun snapshot(): AndroidReminderSettingsSnapshot =
        AndroidReminderSettingsSnapshot(
            preferences = stateStore.reminderPreferences(),
            permissionStatus = permissionController.status(),
            runtimePermission = permissionController.runtimePermissionOrNull(),
        )

    fun setEmiRemindersEnabled(enabled: Boolean): Boolean =
        stateStore.setEmiRemindersEnabled(enabled).also { stored ->
            if (stored) enqueuePreferencesChanged()
        }

    fun setBudgetRemindersEnabled(enabled: Boolean): Boolean =
        stateStore.setBudgetRemindersEnabled(enabled).also { stored ->
            if (stored) enqueuePreferencesChanged()
        }

    fun markPermissionRequestAttempted(): Boolean = permissionController.markRequestAttempted()

    fun onPermissionStateChanged() {
        ReminderReconciliationEnqueuer.enqueue(
            context = context,
            reason = ReminderReconciliationReason.PERMISSION_CHANGED,
        )
    }

    private fun enqueuePreferencesChanged() {
        ReminderReconciliationEnqueuer.enqueue(
            context = context,
            reason = ReminderReconciliationReason.PREFERENCES_CHANGED,
        )
    }
}
