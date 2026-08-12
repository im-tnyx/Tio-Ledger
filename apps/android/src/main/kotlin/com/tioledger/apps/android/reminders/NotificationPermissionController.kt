package com.tioledger.apps.android.reminders

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

enum class AndroidNotificationPermissionStatus {
    NOT_REQUIRED,
    NOT_REQUESTED,
    GRANTED,
    DENIED,
    REVOKED,
}

class AndroidNotificationPermissionController(
    private val context: Context,
    private val stateStore: ReminderPlatformStateStore,
) {
    fun status(): AndroidNotificationPermissionStatus {
        val requiresRuntimePermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        val runtimePermissionGranted =
            !requiresRuntimePermission ||
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        val notificationsEnabled =
            context
                .getSystemService(NotificationManager::class.java)
                .areNotificationsEnabled()
        val status =
            resolveNotificationPermissionStatus(
                requiresRuntimePermission = requiresRuntimePermission,
                runtimePermissionGranted = runtimePermissionGranted,
                notificationsEnabled = notificationsEnabled,
                history = stateStore.permissionHistory(),
            )
        if (status == AndroidNotificationPermissionStatus.GRANTED) {
            stateStore.recordPermissionGranted()
        }
        return status
    }

    fun canPostNotifications(): Boolean {
        val status = status()
        return status == AndroidNotificationPermissionStatus.NOT_REQUIRED ||
            status == AndroidNotificationPermissionStatus.GRANTED
    }

    fun markRequestAttempted(): Boolean = stateStore.markPermissionRequestAttempted()

    fun runtimePermissionOrNull(): String? =
        Manifest.permission.POST_NOTIFICATIONS.takeIf {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        }
}

internal fun resolveNotificationPermissionStatus(
    requiresRuntimePermission: Boolean,
    runtimePermissionGranted: Boolean,
    notificationsEnabled: Boolean,
    history: NotificationPermissionHistory,
): AndroidNotificationPermissionStatus =
    when {
        !notificationsEnabled &&
            (runtimePermissionGranted || history.grantObserved) -> {
            AndroidNotificationPermissionStatus.REVOKED
        }
        !requiresRuntimePermission -> AndroidNotificationPermissionStatus.NOT_REQUIRED
        runtimePermissionGranted -> AndroidNotificationPermissionStatus.GRANTED
        history.grantObserved -> AndroidNotificationPermissionStatus.REVOKED
        history.requestAttempted -> AndroidNotificationPermissionStatus.DENIED
        else -> AndroidNotificationPermissionStatus.NOT_REQUESTED
    }

internal fun shouldReconcileNotificationPermissionChange(
    previous: AndroidNotificationPermissionStatus?,
    current: AndroidNotificationPermissionStatus,
): Boolean = previous != null && previous != current
