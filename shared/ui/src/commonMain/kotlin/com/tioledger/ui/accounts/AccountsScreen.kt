@file:Suppress("FunctionName")

package com.tioledger.ui.accounts

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import com.tioledger.domain.model.AccountType
import com.tioledger.ui.components.TioAddIcon
import com.tioledger.ui.components.TioAmountText
import com.tioledger.ui.components.TioAmountTone
import com.tioledger.ui.components.TioAppBar
import com.tioledger.ui.components.TioBottomNavigation
import com.tioledger.ui.components.TioEmptyState
import com.tioledger.ui.components.TioFilterChip
import com.tioledger.ui.components.TioIcon
import com.tioledger.ui.components.TioSearchField
import com.tioledger.ui.design.TioDimensions
import com.tioledger.ui.design.TioIconToken
import com.tioledger.ui.design.TioSpacing
import com.tioledger.ui.navigation.MainRoute
import com.tioledger.ui.navigation.TioNavigationGraphs
import com.tioledger.ui.navigation.bottomNavigationModel
import org.koin.compose.koinInject

@Composable
fun AccountsRoute(
    viewModel: AccountsViewModel = koinInject(),
    onNavigate: (MainRoute) -> Unit = {},
    onNavigateBack: (() -> Unit)? = null,
    destination: MainRoute = MainRoute.Accounts,
) {
    val state by viewModel.uiState.collectAsState()
    AccountsScreen(
        state = state,
        onAction = viewModel::onAction,
        onNavigate = onNavigate,
        onNavigateBack = onNavigateBack,
        destination = destination,
    )
}

@Composable
fun AccountsScreen(
    state: AccountsUiState,
    onAction: (AccountsAction) -> Unit,
    onNavigate: (MainRoute) -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    destination: MainRoute = MainRoute.Accounts,
    modifier: Modifier = Modifier,
) {
    val bottomNavigation = TioNavigationGraphs.main.bottomNavigationModel(destination)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TioAppBar(
                title = destination.title,
                navigationIcon =
                    onNavigateBack?.let { navigateBack ->
                        {
                            IconButton(
                                onClick = navigateBack,
                                modifier = Modifier.semantics { contentDescription = "Navigate back" },
                            ) {
                                com.tioledger.ui.components.TioBackIcon()
                            }
                        }
                    },
                actions = {
                    IconButton(
                        onClick = { onAction(AccountsAction.AddClicked) },
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
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .fillMaxSize(),
        ) {
            AccountsSummaryRow(state.summary)
            TioSearchField(
                value = state.searchQuery,
                onValueChange = { onAction(AccountsAction.SearchChanged(it)) },
                modifier = Modifier.padding(horizontal = TioSpacing.lg, vertical = TioSpacing.sm),
                placeholder = "Search accounts",
            )
            when {
                state.isLoading -> AccountsLoading()
                state.errorMessage != null -> AccountsError(state.errorMessage) { onAction(AccountsAction.Retry) }
                state.isEmpty -> {
                    TioEmptyState(
                        title = "No accounts",
                        message = "Accounts will appear here after they are added.",
                    )
                }
                else ->
                    AccountsGroupedList(
                        groups = state.groups,
                        onArchiveRequested =
                            if (destination == MainRoute.AccountManager) {
                                { account ->
                                    onAction(AccountsAction.ArchiveRequested(account.id, account.name))
                                }
                            } else {
                                null
                            },
                    )
            }
        }
    }
    if (state.isCreateDialogVisible) {
        AlertDialog(
            onDismissRequest = { onAction(AccountsAction.CreateDismissed) },
            title = { Text("Add account") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(TioSpacing.md)) {
                    OutlinedTextField(
                        value = state.draftName,
                        onValueChange = { onAction(AccountsAction.NameChanged(it)) },
                        label = { Text("Name") },
                        singleLine = true,
                        enabled = !state.isSaving,
                    )
                    Text("Account Group", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(TioSpacing.sm),
                    ) {
                        state.availableGroups.forEach { group ->
                            TioFilterChip(
                                label = group.title,
                                selected = state.draftGroupId == group.id,
                                onClick = { onAction(AccountsAction.GroupChanged(group.id)) },
                            )
                        }
                    }
                    Text(
                        text = "Currency: ${state.draftCurrencyCode}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    state.createErrorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = { onAction(AccountsAction.SaveClicked) }, enabled = !state.isSaving) {
                    Text(if (state.isSaving) "Saving" else "Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(AccountsAction.CreateDismissed) }, enabled = !state.isSaving) { Text("Cancel") }
            },
        )
    }
    state.archiveAccountId?.let {
        AlertDialog(
            onDismissRequest = { onAction(AccountsAction.ArchiveDismissed) },
            title = { Text("Archive account?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(TioSpacing.md)) {
                    Text(
                        "${state.archiveAccountName.orEmpty()} will be removed from active accounts. " +
                            "Its transactions, ledger history, balance, and account identity will be preserved.",
                    )
                    state.archiveErrorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { onAction(AccountsAction.ArchiveConfirmed) },
                    enabled = !state.isArchiving,
                ) {
                    Text(if (state.isArchiving) "Archiving" else "Archive")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { onAction(AccountsAction.ArchiveDismissed) },
                    enabled = !state.isArchiving,
                ) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun AccountsSummaryRow(summary: AccountsSummaryUiModel) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription =
                        "Assets ${summary.assets}, Liabilities ${summary.liabilities}, Total ${summary.total}"
                }
                .padding(horizontal = TioSpacing.lg, vertical = TioSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(TioSpacing.md),
    ) {
        SummaryColumn("Assets", summary.assets, TioAmountTone.Positive, Modifier.weight(1f))
        SummaryColumn("Liabilities", summary.liabilities, TioAmountTone.Negative, Modifier.weight(1f))
        SummaryColumn("Total", summary.total, TioAmountTone.Neutral, Modifier.weight(1f))
    }
}

@Composable
private fun SummaryColumn(
    label: String,
    amount: String,
    tone: TioAmountTone,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TioAmountText(amount = amount, tone = tone)
    }
}

@Composable
private fun AccountsGroupedList(
    groups: List<AccountGroupUiModel>,
    onArchiveRequested: ((AccountRowUiModel) -> Unit)?,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        groups.forEach { group ->
            item(key = "header-${group.id}") {
                AccountGroupHeader(group)
            }
            items(group.accounts, key = { it.id }) { account ->
                AccountRow(account, onArchiveRequested)
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun AccountGroupHeader(group: AccountGroupUiModel) {
    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) { }
                .padding(horizontal = TioSpacing.lg, vertical = TioSpacing.sm),
    ) {
        val measurer = rememberTextMeasurer()
        val titleStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
        val amountStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
        val titleWidth = measurer.measure(group.title, style = titleStyle, softWrap = false).size.width
        val amountWidth = group.totals.maxOfOrNull { measurer.measure(it, style = amountStyle, softWrap = false).size.width } ?: 0
        val gap = with(LocalDensity.current) { TioSpacing.md.roundToPx() }
        if (titleWidth + amountWidth + gap <= constraints.maxWidth) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(text = group.title, modifier = Modifier.weight(1f), style = titleStyle)
                AccountGroupTotals(group)
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(TioSpacing.sm)) {
                Text(text = group.title, style = titleStyle)
                AccountGroupTotals(group, modifier = Modifier.align(Alignment.End))
            }
        }
    }
}

@Composable
private fun AccountGroupTotals(
    group: AccountGroupUiModel,
    modifier: Modifier = Modifier,
) {
    val color =
        if (group.type == AccountType.CREDIT_CARD || group.type == AccountType.LOAN_LINKED) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        group.totals.forEach { total ->
            // Header totals may wrap at large font sizes; keep the complete sign/currency/amount.
            Text(
                text = total,
                color = color,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun AccountRow(
    account: AccountRowUiModel,
    onArchiveRequested: ((AccountRowUiModel) -> Unit)?,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = TioDimensions.accountRowHeight)
                .semantics {
                    contentDescription = "${account.name}, ${account.typeLabel}, ${account.balance}"
                }
                .padding(horizontal = TioSpacing.lg, vertical = TioSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TioIcon(TioIconToken.Account, contentDescription = "${account.typeLabel} account")
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(horizontal = TioSpacing.md),
        ) {
            Text(
                text = account.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
            )
            Text(
                text = account.currencyCode,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TioAmountText(
            amount = account.balance,
            tone = if (account.isLiability) TioAmountTone.Negative else TioAmountTone.Neutral,
        )
        onArchiveRequested?.let { archive ->
            IconButton(
                onClick = { archive(account) },
                modifier = Modifier.semantics { contentDescription = "Archive ${account.name}" },
            ) {
                TioIcon(TioIconToken.Delete, contentDescription = null)
            }
        }
    }
}

@Composable
private fun AccountsLoading() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(TioSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text(
            text = "Loading accounts",
            modifier = Modifier.padding(top = TioSpacing.md),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun AccountsError(
    message: String,
    onRetry: () -> Unit,
) {
    TioEmptyState(
        title = "Accounts unavailable",
        message = message,
        action = {
            TextButton(onClick = onRetry) {
                Text("Retry")
            }
        },
    )
}
