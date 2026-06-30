@file:Suppress("FunctionName")

package com.tioledger.ui.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.tioledger.domain.model.AccountType
import com.tioledger.ui.components.TioAmountText
import com.tioledger.ui.components.TioAmountTone
import com.tioledger.ui.components.TioAppBar
import com.tioledger.ui.components.TioBottomNavigation
import com.tioledger.ui.components.TioEmptyState
import com.tioledger.ui.components.TioIcon
import com.tioledger.ui.components.TioNavigationItem
import com.tioledger.ui.components.TioSearchField
import com.tioledger.ui.design.TioDimensions
import com.tioledger.ui.design.TioIconToken
import com.tioledger.ui.design.TioSpacing
import org.koin.compose.koinInject

@Composable
fun AccountsRoute(viewModel: AccountsViewModel = koinInject()) {
    val state by viewModel.uiState.collectAsState()
    AccountsScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}

@Composable
fun AccountsScreen(
    state: AccountsUiState,
    onAction: (AccountsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TioAppBar(
                title = "Accounts",
                actions = {
                    TioIcon(TioIconToken.Analytics, contentDescription = "Account statistics")
                    TioIcon(TioIconToken.Settings, contentDescription = "More account options")
                },
            )
        },
        bottomBar = {
            TioBottomNavigation(
                items =
                    listOf(
                        TioNavigationItem("Trans.", TioIconToken.Transaction, false),
                        TioNavigationItem("Stats", TioIconToken.Analytics, false),
                        TioNavigationItem("Accounts", TioIconToken.Account, true),
                        TioNavigationItem("More", TioIconToken.Settings, false),
                    ),
                onItemSelected = {},
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
                else -> AccountsGroupedList(state.groups)
            }
        }
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
private fun AccountsGroupedList(groups: List<AccountGroupUiModel>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        groups.forEach { group ->
            item(key = "header-${group.type}") {
                AccountGroupHeader(group)
            }
            items(group.accounts, key = { it.id }) { account ->
                AccountRow(account)
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun AccountGroupHeader(group: AccountGroupUiModel) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = TioSpacing.lg, vertical = TioSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = group.title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        TioAmountText(
            amount = group.total,
            tone =
                if (group.type == AccountType.CREDIT_CARD || group.type == AccountType.LOAN_LINKED) {
                    TioAmountTone.Negative
                } else {
                    TioAmountTone.Neutral
                },
        )
    }
}

@Composable
private fun AccountRow(account: AccountRowUiModel) {
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
