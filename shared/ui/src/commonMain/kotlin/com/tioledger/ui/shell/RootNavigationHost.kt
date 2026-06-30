@file:Suppress("FunctionName")

package com.tioledger.ui.shell

import androidx.compose.runtime.Composable
import com.tioledger.ui.accounts.AccountsRoute
import com.tioledger.ui.navigation.TioNavigationGraphs
import com.tioledger.ui.navigation.TioRoute

@Composable
fun RootNavigationHost(
    currentRoute: TioRoute = TioNavigationGraphs.main.startRoute,
    content: @Composable (TioRoute) -> Unit = { route ->
        when (route) {
            TioRoute.Splash -> Splash()
            TioRoute.Main, TioRoute.Accounts -> AccountsRoute()
        }
    },
) {
    content(currentRoute)
}
