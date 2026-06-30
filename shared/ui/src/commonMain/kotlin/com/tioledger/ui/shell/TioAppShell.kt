@file:Suppress("FunctionName")

package com.tioledger.ui.shell

import androidx.compose.runtime.Composable
import com.tioledger.bootstrap.diagnostics.StartupDiagnostics
import com.tioledger.ui.navigation.TioRoute

@Composable
fun TioAppShell(
    diagnostics: StartupDiagnostics,
    darkTheme: Boolean,
    currentRoute: TioRoute = TioRoute.Accounts,
) {
    val route = if (diagnostics.koinStarted) currentRoute else TioRoute.Splash
    TioRootScaffold(darkTheme = darkTheme) {
        RootNavigationHost(currentRoute = route)
    }
}
