@file:Suppress("FunctionName")

package com.tioledger.ui.shell

import androidx.compose.runtime.Composable
import com.tioledger.bootstrap.diagnostics.StartupDiagnostics
import com.tioledger.ui.navigation.RootRoute
import com.tioledger.ui.navigation.TioNavigationGraphs

@Composable
fun TioAppShell(
    diagnostics: StartupDiagnostics,
    darkTheme: Boolean,
    currentRoute: RootRoute = TioNavigationGraphs.root.mainEntry,
) {
    val route = if (diagnostics.koinStarted) currentRoute else TioNavigationGraphs.root.startRoute
    TioRootScaffold(darkTheme = darkTheme) {
        RootNavigationHost(currentRoute = route)
    }
}
