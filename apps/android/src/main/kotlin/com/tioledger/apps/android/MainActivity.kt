package com.tioledger.apps.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import com.tioledger.apps.android.reminders.ReminderNavigationIntent
import com.tioledger.bootstrap.diagnostics.StartupDiagnostics
import com.tioledger.ui.navigation.RootRoute
import com.tioledger.ui.navigation.TioNavigationGraphs
import com.tioledger.ui.shell.TioAppShell

class MainActivity : ComponentActivity() {
    private val currentRoute = mutableStateOf<RootRoute>(TioNavigationGraphs.root.mainEntry)

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
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        ReminderNavigationIntent.routeOrNull(intent)?.let { route -> currentRoute.value = route }
    }
}
