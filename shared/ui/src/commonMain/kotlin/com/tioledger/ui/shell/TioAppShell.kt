@file:Suppress("FunctionName")

package com.tioledger.ui.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tioledger.bootstrap.diagnostics.StartupDiagnostics
import com.tioledger.ui.navigation.MainRoute
import com.tioledger.ui.navigation.RootRoute
import com.tioledger.ui.navigation.TioNavigationGraphs

@Composable
fun TioAppShell(
    diagnostics: StartupDiagnostics,
    darkTheme: Boolean,
    currentRoute: RootRoute = TioNavigationGraphs.root.mainEntry,
    onNavigate: ((RootRoute) -> Unit)? = null,
    settingsContent: @Composable ((MainRoute) -> Unit) -> Unit = { onNavigate ->
        MainPlaceholderDestination(
            destination = MainRoute.Settings,
            onNavigate = onNavigate,
        )
    },
) {
    var activeRoute by remember(currentRoute) { mutableStateOf(currentRoute) }
    val route =
        if (diagnostics.koinStarted) {
            onNavigate?.let { currentRoute } ?: activeRoute
        } else {
            TioNavigationGraphs.root.startRoute
        }

    TioRootScaffold(darkTheme = darkTheme) {
        RootNavigationHost(
            currentRoute = route,
            onNavigate = { destination ->
                if (onNavigate != null) {
                    onNavigate(destination)
                } else {
                    activeRoute = destination
                }
            },
            settingsContent = settingsContent,
        )
    }
}
