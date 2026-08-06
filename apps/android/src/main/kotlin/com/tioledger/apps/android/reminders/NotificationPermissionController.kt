package com.tioledger.apps.android.reminders

import android.Manifest
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
        val granted =
            !requiresRuntimePermission ||
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        val status =
            resolveNotificationPermissionStatus(
                requiresRuntimePermission = requiresRuntimePermission,
                granted = granted,
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
    granted: Boolean,
    history: NotificationPermissionHistory,
): AndroidNotificationPermissionStatus =
    when {
        !requiresRuntimePermission -> AndroidNotificationPermissionStatus.NOT_REQUIRED
        granted -> AndroidNotificationPermissionStatus.GRANTED
        history.grantObserved -> AndroidNotificationPermissionStatus.REVOKED
        history.requestAttempted -> AndroidNotificationPermissionStatus.DENIED
        else -> AndroidNotificationPermissionStatus.NOT_REQUESTED
    }
