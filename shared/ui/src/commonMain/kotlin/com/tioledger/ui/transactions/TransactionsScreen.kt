@file:Suppress("FunctionName")

package com.tioledger.ui.transactions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.tioledger.domain.model.TransactionType
import com.tioledger.ui.components.TioAmountTone
import com.tioledger.ui.components.TioAppBar
import com.tioledger.ui.components.TioBottomNavigation
import com.tioledger.ui.components.TioEmptyState
import com.tioledger.ui.components.TioErrorState
import com.tioledger.ui.components.TioFloatingActionButton
import com.tioledger.ui.components.TioLoadingState
import com.tioledger.ui.components.TioNavigationItem
import com.tioledger.ui.components.TioTransactionRow
import com.tioledger.ui.design.TioIconToken
import com.tioledger.ui.navigation.MainRoute
import com.tioledger.ui.navigation.TioNavigationGraphs
import org.koin.compose.koinInject

@Composable
fun TransactionsRoute(
    viewModel: TransactionsViewModel = koinInject(),
    onAddTransaction: () -> Unit = {},
    onNavigate: (MainRoute) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    TransactionsScreen(
        state = state,
        onAction = viewModel::onAction,
        onAddTransaction = onAddTransaction,
        onNavigate = onNavigate,
    )
}

@Composable
fun TransactionsScreen(
    state: TransactionsUiState,
    onAction: (TransactionsAction) -> Unit,
    onAddTransaction: () -> Unit,
    onNavigate: (MainRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navigationRoutes = TioNavigationGraphs.main.bottomNavigationRoutes
    val navigationItems =
        navigationRoutes.map { route ->
            TioNavigationItem(
                label = route.title,
                icon = route.icon,
                selected = route == MainRoute.Transactions,
            )
        }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TioAppBar(title = "Transactions") },
        floatingActionButton = {
            TioFloatingActionButton(
                onClick = onAddTransaction,
                contentDescription = "Add transaction",
            )
        },
        bottomBar = {
            TioBottomNavigation(
                items = navigationItems,
                onItemSelected = { selectedItem ->
                    val selectedIndex = navigationItems.indexOf(selectedItem)
                    if (selectedIndex >= 0) {
                        onNavigate(navigationRoutes[selectedIndex])
                    }
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
            when {
                state.isLoading -> {
                    TioLoadingState(label = "Loading transactions")
                }
                state.errorMessage != null -> {
                    TioErrorState(
                        title = "Transactions unavailable",
                        message = state.errorMessage,
                        retryLabel = "Retry",
                        onRetry = { onAction(TransactionsAction.Retry) },
                    )
                }
                state.isEmpty -> {
                    TioEmptyState(
                        title = "No transactions",
                        message = "Income, expenses, and transfers will appear here after they are recorded.",
                        action = {
                            TextButton(onClick = onAddTransaction) {
                                Text("Add transaction")
                            }
                        },
                    )
                }
                else -> TransactionsList(state.transactions)
            }
        }
    }
}

@Composable
private fun TransactionsList(transactions: List<TransactionRowUiModel>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(transactions, key = TransactionRowUiModel::id) { transaction ->
            TioTransactionRow(
                title = transaction.title,
                subtitle = transaction.subtitle,
                amount = transaction.amount,
                amountTone = transaction.type.toAmountTone(),
                icon = transaction.type.toIcon(),
                modifier =
                    Modifier.semantics {
                        contentDescription =
                            "${transaction.title}, ${transaction.subtitle}, ${transaction.amount}"
                    },
            )
            HorizontalDivider()
        }
    }
}

private fun TransactionType.toAmountTone(): TioAmountTone =
    when (this) {
        TransactionType.INCOME, TransactionType.LOAN_DISBURSEMENT -> TioAmountTone.Positive
        TransactionType.EXPENSE, TransactionType.REPAYMENT -> TioAmountTone.Negative
        TransactionType.TRANSFER, TransactionType.ADJUSTMENT -> TioAmountTone.Neutral
    }

private fun TransactionType.toIcon(): TioIconToken =
    when (this) {
        TransactionType.TRANSFER -> TioIconToken.Transfer
        TransactionType.LOAN_DISBURSEMENT, TransactionType.REPAYMENT -> TioIconToken.Loan
        TransactionType.INCOME, TransactionType.EXPENSE, TransactionType.ADJUSTMENT -> TioIconToken.Transaction
    }
