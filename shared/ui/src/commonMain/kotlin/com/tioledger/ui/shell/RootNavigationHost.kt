@file:Suppress("FunctionName")

package com.tioledger.ui.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tioledger.ui.accounts.AccountsRoute
import com.tioledger.ui.components.TioAppBar
import com.tioledger.ui.components.TioBottomNavigation
import com.tioledger.ui.components.TioEmptyState
import com.tioledger.ui.components.TioNavigationItem
import com.tioledger.ui.design.TioSpacing
import com.tioledger.ui.navigation.MainGraph
import com.tioledger.ui.navigation.MainRoute
import com.tioledger.ui.navigation.RootRoute
import com.tioledger.ui.navigation.TioNavigationGraphs

@Composable
fun RootNavigationHost(
    currentRoute: RootRoute = TioNavigationGraphs.root.startRoute,
    content: @Composable (RootRoute) -> Unit = { route ->
        when (route) {
            RootRoute.Splash -> Splash()
            is RootRoute.Main -> {
                when (route.destination) {
                    MainRoute.Accounts -> AccountsRoute()
                    else -> MainPlaceholderDestination(route.destination)
                }
            }
        }
    },
) {
    content(currentRoute)
}

@Composable
private fun MainPlaceholderDestination(destination: MainRoute) {
    Scaffold(
        topBar = { TioAppBar(title = destination.title) },
        bottomBar = {
            TioBottomNavigation(
                items = TioNavigationGraphs.main.bottomNavigationItems(destination),
                onItemSelected = {},
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(TioSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(TioSpacing.lg),
        ) {
            TioEmptyState(
                title = destination.title,
                message = "Navigation Graph v1 wires this destination as a placeholder only.",
            )
        }
    }
}

private fun MainGraph.bottomNavigationItems(currentRoute: MainRoute): List<TioNavigationItem> =
    bottomNavigationRoutes.map { route ->
        TioNavigationItem(
            label = route.title,
            icon = route.icon,
            selected = route == currentRoute,
        )
    }
