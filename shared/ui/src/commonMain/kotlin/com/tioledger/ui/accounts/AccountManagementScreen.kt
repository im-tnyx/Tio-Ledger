@file:Suppress("FunctionName")

package com.tioledger.ui.accounts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.tioledger.ui.components.TioAddIcon
import com.tioledger.ui.components.TioAppBar
import com.tioledger.ui.components.TioBackIcon
import com.tioledger.ui.components.TioBottomNavigation
import com.tioledger.ui.components.TioEmptyState
import com.tioledger.ui.design.TioDimensions
import com.tioledger.ui.design.TioSpacing
import com.tioledger.ui.navigation.MainRoute
import com.tioledger.ui.navigation.TioNavigationGraphs
import com.tioledger.ui.navigation.bottomNavigationModel
import org.koin.compose.koinInject

@Composable
fun AccountManagementRoute(
    viewModel: AccountsViewModel = koinInject(),
    onNavigate: (MainRoute) -> Unit = {},
    onNavigateBack: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    AccountManagementScreen(
        state = state,
        onAction = viewModel::onAction,
        onNavigate = onNavigate,
        onNavigateBack = onNavigateBack,
        onAccountClick = { accountId ->
            onNavigate(MainRoute.AccountEdit(accountId))
        },
    )
}

@Composable
fun AccountManagementScreen(
    state: AccountsUiState,
    onAction: (AccountsAction) -> Unit,
    onNavigate: (MainRoute) -> Unit,
    onNavigateBack: () -> Unit,
    onAccountClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bottomNavigation = TioNavigationGraphs.main.bottomNavigationModel(MainRoute.AccountManager)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TioAppBar(
                title = MainRoute.AccountManager.title,
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics { contentDescription = "Navigate back" },
                    ) {
                        TioBackIcon()
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onNavigate(MainRoute.AccountAdd) },
                        modifier = Modifier.semantics { contentDescription = "Add account" },
                    ) {
                        TioAddIcon()
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
        Column(
            modifier =
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
        ) {
            when {
                state.isLoading -> AccountsLoading()
                state.errorMessage != null -> AccountsError(state.errorMessage) { onAction(AccountsAction.Retry) }
                state.isEmpty -> {
                    TioEmptyState(
                        title = "No accounts",
                        message = "Accounts will appear here after they are added.",
                    )
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        state.groups.forEach { group ->
                            item(key = "header-${group.id}") {
                                Text(
                                    text = group.title,
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = TioSpacing.lg, vertical = TioSpacing.sm),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                )
                            }
                            items(group.accounts, key = { it.id }) { account ->
                                AccountManagementRow(
                                    accountName = account.name,
                                    onClick = { onAccountClick(account.id) },
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountManagementRow(
    accountName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = TioDimensions.accountRowHeight)
                .clickable(onClick = onClick)
                .semantics { contentDescription = accountName }
                .padding(horizontal = TioSpacing.lg, vertical = TioSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = accountName,
            style = MaterialTheme.typography.bodyLarge,
            modifier =
                Modifier
                    .weight(1f)
                    .padding(horizontal = TioSpacing.md),
            maxLines = 1,
        )
    }
}
