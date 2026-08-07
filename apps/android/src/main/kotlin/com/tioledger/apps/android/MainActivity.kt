package com.tioledger.apps.android

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import com.tioledger.apps.android.reminders.AndroidReminderSettingsRoute
import com.tioledger.apps.android.reminders.AndroidReminderSettingsService
import com.tioledger.apps.android.reminders.ReminderNavigationIntent
import com.tioledger.bootstrap.diagnostics.StartupDiagnostics
import com.tioledger.ui.navigation.RootRoute
import com.tioledger.ui.navigation.TioNavigationGraphs
import com.tioledger.ui.shell.TioAppShell

class MainActivity : ComponentActivity() {
    private val currentRoute = mutableStateOf<RootRoute>(TioNavigationGraphs.root.mainEntry)
    private val settingsRefreshToken = mutableStateOf(0L)
    private var notificationSettingsOpen = false
    private val reminderSettingsService: AndroidReminderSettingsService by lazy {
        (application as TioAndroidApplication)
            .koinApplication
            .koin
            .get()
    }
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            reminderSettingsService.onPermissionStateChanged()
            settingsRefreshToken.value += 1L
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ReminderNavigationIntent.routeOrNull(intent)?.let { route -> currentRoute.value = route }
        val diagnostics =
            (application as TioAndroidApplication)
                .koinApplication
                .koin
                .get<StartupDiagnostics>()

        setContent {
            TioAppShell(
                diagnostics = diagnostics,
                darkTheme = false,
                currentRoute = currentRoute.value,
                settingsContent = { onNavigate ->
                    AndroidReminderSettingsRoute(
                        settingsService = reminderSettingsService,
                        refreshToken = settingsRefreshToken.value,
                        onLaunchRuntimePermission = notificationPermissionLauncher::launch,
                        onOpenNotificationSettings = ::openNotificationSettings,
                        onNavigate = onNavigate,
                    )
                },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (notificationSettingsOpen) {
            notificationSettingsOpen = false
            reminderSettingsService.onPermissionStateChanged()
            settingsRefreshToken.value += 1L
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        ReminderNavigationIntent.routeOrNull(intent)?.let { route -> currentRoute.value = route }
    }

    private fun openNotificationSettings() {
        notificationSettingsOpen = true
        startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
        )
    }
}
