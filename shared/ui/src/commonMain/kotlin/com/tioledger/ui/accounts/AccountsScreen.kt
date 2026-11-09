@file:Suppress("FunctionName")

package com.tioledger.ui.accounts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tioledger.domain.model.AccountType
import com.tioledger.ui.components.TioAddIcon
import com.tioledger.ui.components.TioAmountText
import com.tioledger.ui.components.TioAmountTone
import com.tioledger.ui.components.TioAppBar
import com.tioledger.ui.components.TioBottomNavigation
import com.tioledger.ui.components.TioEmptyState
import com.tioledger.ui.components.TioFilterChip
import com.tioledger.ui.design.TioDimensions
import com.tioledger.ui.design.TioSpacing
import com.tioledger.ui.navigation.MainRoute
import com.tioledger.ui.navigation.TioNavigationGraphs
import com.tioledger.ui.navigation.bottomNavigationModel
import org.koin.compose.koinInject

@Composable
fun AccountsRoute(
    viewModel: AccountsViewModel = koinInject(),
    onNavigate: (MainRoute) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    AccountsScreen(
        state = state,
        onAction = viewModel::onAction,
        onNavigate = onNavigate,
    )
}

@Composable
fun AccountsScreen(
    state: AccountsUiState,
    onAction: (AccountsAction) -> Unit,
    onNavigate: (MainRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bottomNavigation = TioNavigationGraphs.main.bottomNavigationModel(MainRoute.Accounts)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TioAppBar(
                title = MainRoute.Accounts.title,
                actions = {
                    IconButton(
                        onClick = { onNavigate(MainRoute.AccountQuickAdd) },
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
            AccountsSummaryRow(state.summary)
            HorizontalDivider()
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
                        onAccountClick = { account ->
                            if (account.isLoan) {
                                if (account.loanId != null) {
                                    onNavigate(MainRoute.LoanDetails(account.loanId))
                                } else {
                                    onNavigate(MainRoute.Loans)
                                }
                            }
                        },
                    )
            }
        }
    }
}

@Composable
internal fun AddAccountDialog(
    state: AccountsUiState,
    onAction: (AccountsAction) -> Unit,
) {
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

@Composable
internal fun ArchiveAccountConfirmationDialog(
    accountName: String,
    errorMessage: String?,
    isArchiving: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Archive account?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(TioSpacing.md)) {
                Text(
                    "$accountName will be removed from active accounts. " +
                        "Its transactions, ledger history, balance, and account identity will be preserved.",
                )
                errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isArchiving,
            ) {
                Text(if (isArchiving) "Archiving" else "Archive")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isArchiving,
            ) {
                Text("Cancel")
            }
        },
    )
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
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
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
    onAccountClick: (AccountRowUiModel) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        groups.forEach { group ->
            item(key = "header-${group.id}") {
                AccountGroupHeader(group)
                HorizontalDivider()
            }
            if (group.type == AccountType.CREDIT_CARD) {
                group.accounts
                    .groupBy { it.billingDay }
                    .toSortedMap(compareBy<Int?> { it ?: 32 })
                    .forEach { (billingDay, cards) ->
                        if (billingDay != null) {
                            item(key = "billing-${group.id}-$billingDay") {
                                BillingDateSectionHeader(billingDay, cards.size)
                            }
                        }
                        items(cards, key = { it.id }) { account ->
                            AccountRow(account = account, onClick = { onAccountClick(account) })
                            HorizontalDivider()
                        }
                    }
            } else {
                items(group.accounts, key = { it.id }) { account ->
                    AccountRow(
                        account = account,
                        onClick = { onAccountClick(account) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun BillingDateSectionHeader(
    billingDay: Int,
    cardCount: Int,
) {
    Text(
        text = "Billing day $billingDay · $cardCount ${if (cardCount == 1) "card" else "cards"}",
        modifier = Modifier.fillMaxWidth().padding(horizontal = TioSpacing.lg, vertical = TioSpacing.sm),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun AccountGroupHeader(group: AccountGroupUiModel) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) { }
                .heightIn(min = TioDimensions.accountRowHeight)
                .padding(horizontal = TioSpacing.lg, vertical = TioSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = group.title,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
        if (group.type == AccountType.CREDIT_CARD) {
            CreditCardBalanceHeadingsAndTotals(group)
        } else {
            AccountGroupTotals(group, modifier = Modifier.padding(start = TioSpacing.sm))
        }
    }
}

@Composable
private fun CreditCardBalanceHeadingsAndTotals(group: AccountGroupUiModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(TioSpacing.sm)) {
        CreditCardBalanceColumn(label = "Balance Payable", amounts = group.totals, isPayable = true)
        CreditCardBalanceColumn(label = "Outst. Balance", amounts = group.totals)
    }
}

@Composable
private fun CreditCardBalanceColumn(
    label: String,
    amounts: List<String>,
    isPayable: Boolean = false,
) {
    Column(
        modifier = Modifier.widthIn(min = 86.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(TioSpacing.xs),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            maxLines = 2,
        )
        amounts.forEach { amount ->
            Text(
                text = amount,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isPayable) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun CreditCardBalanceCell(
    amount: String,
    isPayable: Boolean = false,
) {
    Column(
        modifier = Modifier.widthIn(min = 86.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(TioSpacing.xs),
    ) {
        Text(
            text = amount,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (isPayable) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun AccountGroupTotals(
    group: AccountGroupUiModel,
    modifier: Modifier = Modifier,
) {
    val color =
        if (group.type == AccountType.LOAN_LINKED) {
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
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun AccountRow(
    account: AccountRowUiModel,
    onClick: () -> Unit = {},
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = TioDimensions.accountRowHeight)
                .clickable(enabled = account.isLoan, onClick = onClick)
                .semantics {
                    val billing =
                        account.billingDay?.let { "Billing day $it, payment due day ${account.paymentDueDay}" }.orEmpty()
                    contentDescription = "${account.name}, ${account.typeLabel}, ${account.balance}, $billing"
                }
                .padding(horizontal = TioSpacing.lg, vertical = TioSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(TioSpacing.xs)) {
            Text(
                text = account.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
            )
            account.billingDay?.let { billingDay ->
                Text(
                    text = "Billing day $billingDay · Payment due day ${account.paymentDueDay}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
        if (account.accountType == AccountType.CREDIT_CARD) {
            Row(horizontalArrangement = Arrangement.spacedBy(TioSpacing.sm)) {
                CreditCardBalanceCell(amount = account.balance, isPayable = true)
                CreditCardBalanceCell(amount = account.balance)
            }
        } else {
            TioAmountText(
                amount = account.balance,
                tone = if (account.isLiability) TioAmountTone.Negative else TioAmountTone.Neutral,
            )
        }
    }
}

@Composable
internal fun AccountsLoading() {
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
internal fun AccountsError(
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
