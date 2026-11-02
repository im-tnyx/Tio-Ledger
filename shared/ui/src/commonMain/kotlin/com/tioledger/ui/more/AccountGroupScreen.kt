@file:Suppress("FunctionName")

package com.tioledger.ui.more

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tioledger.ui.components.TioAppBar
import com.tioledger.ui.components.TioBottomNavigation
import com.tioledger.ui.components.TioListItem
import com.tioledger.ui.navigation.MainRoute
import com.tioledger.ui.navigation.TioNavigationGraphs
import com.tioledger.ui.navigation.bottomNavigationModel

private data class AccountGroupEntry(
    val title: String,
    val value: String? = null,
    val route: MainRoute? = null,
)

private val accountGroups =
    listOf(
        AccountGroupEntry("Cash"),
        AccountGroupEntry("Accounts"),
        AccountGroupEntry("Card", value = "Card"),
        AccountGroupEntry("Debit Card", value = "Debit Cards"),
        AccountGroupEntry("Savings"),
        AccountGroupEntry("Top-Up/Prepaid"),
        AccountGroupEntry("Investments"),
        AccountGroupEntry("Overdrafts"),
        AccountGroupEntry("Loan", route = MainRoute.Loans),
        AccountGroupEntry("Insurance"),
        AccountGroupEntry("Others"),
    )

@Composable
fun AccountGroupRoute(
    onNavigate: (MainRoute) -> Unit = {},
    onNavigateBack: () -> Unit = {},
) {
    AccountGroupScreen(
        onNavigate = onNavigate,
        onNavigateBack = onNavigateBack,
    )
}

@Composable
fun AccountGroupScreen(
    onNavigate: (MainRoute) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bottomNavigation = TioNavigationGraphs.main.bottomNavigationModel(MainRoute.AccountGroup)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TioAppBar(
                title = MainRoute.AccountGroup.title,
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("‹ Accounts")
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Text(
                            text = "✎",
                            modifier = Modifier.size(48.dp),
                            fontSize = 28.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                    IconButton(onClick = {}) {
                        Text(
                            text = "+",
                            modifier = Modifier.size(48.dp),
                            fontSize = 32.sp,
                            textAlign = TextAlign.Center,
                        )
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
            items(accountGroups, key = AccountGroupEntry::title) { entry ->
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
