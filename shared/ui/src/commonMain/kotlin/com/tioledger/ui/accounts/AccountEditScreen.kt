@file:Suppress("FunctionName")

package com.tioledger.ui.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.tioledger.ui.components.TioAppBar
import com.tioledger.ui.components.TioBackIcon
import com.tioledger.ui.components.TioBottomNavigation
import com.tioledger.ui.components.TioEmptyState
import com.tioledger.ui.components.TioIcon
import com.tioledger.ui.design.TioIconToken
import com.tioledger.ui.design.TioSpacing
import com.tioledger.ui.navigation.MainRoute
import com.tioledger.ui.navigation.TioNavigationGraphs
import com.tioledger.ui.navigation.bottomNavigationModel
import org.koin.compose.koinInject

@Composable
fun AccountEditRoute(
    accountId: String,
    viewModel: AccountsViewModel = koinInject(),
    onNavigate: (MainRoute) -> Unit = {},
    onNavigateBack: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    AccountEditScreen(
        accountId = accountId,
        state = state,
        onAction = viewModel::onAction,
        onNavigate = onNavigate,
        onNavigateBack = onNavigateBack,
    )
}

@Composable
fun AccountEditScreen(
    accountId: String,
    state: AccountsUiState,
    onAction: (AccountsAction) -> Unit,
    onNavigate: (MainRoute) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val destination = MainRoute.AccountEdit(accountId)
    val bottomNavigation = TioNavigationGraphs.main.bottomNavigationModel(destination)
    val accountRow = state.groups.flatMap { it.accounts }.firstOrNull { it.id == accountId }
    val group = state.groups.firstOrNull { it.accounts.any { acc -> acc.id == accountId } }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TioAppBar(
                title = destination.title,
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics { contentDescription = "Navigate back" },
                    ) {
                        TioBackIcon()
                    }
                },
                actions = {
                    if (accountRow != null) {
                        IconButton(
                            onClick = {
                                onAction(AccountsAction.ArchiveRequested(accountRow.id, accountRow.name))
                            },
                            modifier = Modifier.semantics { contentDescription = "Archive ${accountRow.name}" },
                        ) {
                            TioIcon(TioIconToken.Delete, contentDescription = null)
                        }
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
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(TioSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(TioSpacing.md),
        ) {
            if (accountRow == null) {
                TioEmptyState(
                    title = "Account unavailable",
                    message = "The selected account could not be found or has been archived.",
                    action = {
                        TextButton(onClick = onNavigateBack) {
                            Text("Back to accounts")
                        }
                    },
                )
            } else {
                OutlinedTextField(
                    value = group?.title.orEmpty(),
                    onValueChange = {},
                    label = { Text("Group") },
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = accountRow.name,
                    onValueChange = {},
                    label = { Text("Name") },
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = "${accountRow.currencyCode} ${accountRow.balance}",
                    onValueChange = {},
                    label = { Text("Amount (Ledger-derived)") },
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        Text("Ledger-first balance. Amount adjustment workflow is deferred to Stage C.")
                    },
                )
                OutlinedTextField(
                    value = "Pending Stage B metadata specification",
                    onValueChange = {},
                    label = { Text("Description") },
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = "Enabled (Default)",
                    onValueChange = {},
                    label = { Text("Include in totals") },
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        Text("Include in totals setting is pending Stage B persistence approval.")
                    },
                )
                OutlinedTextField(
                    value = "Visible",
                    onValueChange = {},
                    label = { Text("Show/Hide Setting") },
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        Text("Visibility settings are pending Stage B persistence approval.")
                    },
                )
            }
        }
    }

    state.archiveAccountId?.let {
        ArchiveAccountConfirmationDialog(
            accountName = state.archiveAccountName.orEmpty(),
            errorMessage = state.archiveErrorMessage,
            isArchiving = state.isArchiving,
            onConfirm = {
                onAction(AccountsAction.ArchiveConfirmed)
                onNavigateBack()
            },
            onDismiss = { onAction(AccountsAction.ArchiveDismissed) },
        )
    }
}
