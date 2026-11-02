@file:Suppress("FunctionName")

package com.tioledger.ui.more

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tioledger.ui.components.TioAppBar
import com.tioledger.ui.components.TioBottomNavigation
import com.tioledger.ui.components.TioListItem
import com.tioledger.ui.navigation.MainRoute
import com.tioledger.ui.navigation.TioNavigationGraphs
import com.tioledger.ui.navigation.bottomNavigationModel

private data class AccountSettingEntry(
    val title: String,
    val value: String? = null,
    val route: MainRoute? = null,
)

private val accountSettings =
    listOf(
        AccountSettingEntry("Account Group", route = MainRoute.AccountGroup),
        AccountSettingEntry("Accounts Setting"),
        AccountSettingEntry("Include in totals"),
        AccountSettingEntry("Transfer-Expense setting"),
        AccountSettingEntry("Deleted accounts"),
        AccountSettingEntry("Card expenses display config", value = "A. At the time"),
    )

@Composable
fun AccountSettingsRoute(
    onNavigate: (MainRoute) -> Unit = {},
    onNavigateBack: () -> Unit = {},
) {
    AccountSettingsScreen(
        onNavigate = onNavigate,
        onNavigateBack = onNavigateBack,
    )
}

@Composable
fun AccountSettingsScreen(
    onNavigate: (MainRoute) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bottomNavigation = TioNavigationGraphs.main.bottomNavigationModel(MainRoute.AccountSettings)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TioAppBar(
                title = "Accounts",
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("‹ Settings")
                    }
                },
            )
        },
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
            items(accountSettings, key = AccountSettingEntry::title) { entry ->
                TioListItem(
                    title = entry.title,
                    trailing =
                        entry.value?.let { value ->
                            {
                                Text(
                                    text = value,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        },
                    onClick = entry.route?.let { route -> { onNavigate(route) } },
                )
            }
        }
    }
}
