@file:Suppress("FunctionName")

package com.tioledger.ui.more

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tioledger.ui.components.TioAppBar
import com.tioledger.ui.components.TioBottomNavigation
import com.tioledger.ui.components.TioIconAvatar
import com.tioledger.ui.components.TioListItem
import com.tioledger.ui.navigation.MainRoute
import com.tioledger.ui.navigation.TioNavigationGraphs
import com.tioledger.ui.navigation.bottomNavigationModel

private data class ConfigurationEntry(
    val title: String,
    val subtitle: String,
    val route: MainRoute,
)

private val configurationEntries =
    listOf(
        ConfigurationEntry(
            title = MainRoute.AccountSettings.title,
            subtitle = "Account groups and account preferences",
            route = MainRoute.AccountSettings,
        ),
        ConfigurationEntry(
            title = MainRoute.TransactionSettings.title,
            subtitle = "Transaction entry and history preferences",
            route = MainRoute.TransactionSettings,
        ),
        ConfigurationEntry(
            title = MainRoute.Categories.title,
            subtitle = "Income and expense categories",
            route = MainRoute.Categories,
        ),
        ConfigurationEntry(
            title = MainRoute.Budgets.title,
            subtitle = "Budget plans and progress",
            route = MainRoute.Budgets,
        ),
        ConfigurationEntry(
            title = MainRoute.CurrencySettings.title,
            subtitle = "Currency preferences",
            route = MainRoute.CurrencySettings,
        ),
        ConfigurationEntry(
            title = MainRoute.RepeatSettings.title,
            subtitle = "Repeat transaction reflection timing",
            route = MainRoute.RepeatSettings,
        ),
        ConfigurationEntry(
            title = MainRoute.Settings.title,
            subtitle = "Reminder and notification settings",
            route = MainRoute.Settings,
        ),
    )

@Composable
fun ConfigurationRoute(
    onNavigate: (MainRoute) -> Unit = {},
) {
    ConfigurationScreen(onNavigate = onNavigate)
}

@Composable
fun ConfigurationScreen(
    onNavigate: (MainRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bottomNavigation = TioNavigationGraphs.main.bottomNavigationModel(MainRoute.Configuration)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TioAppBar(title = MainRoute.Configuration.title) },
        bottomBar = {
            TioBottomNavigation(
                items = bottomNavigation.items,
                onItemSelected = { selectedItem ->
                    bottomNavigation.navigate(selectedItem, onNavigate)
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier =
                Modifier
                    .padding(padding)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .fillMaxSize(),
        ) {
            items(configurationEntries, key = ConfigurationEntry::title) { entry ->
                TioListItem(
                    title = entry.title,
                    subtitle = entry.subtitle,
                    leading = { TioIconAvatar(entry.route.icon) },
                    onClick = { onNavigate(entry.route) },
                )
            }
        }
    }
}
