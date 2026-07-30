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
import com.tioledger.ui.budgets.BudgetsRoute
import com.tioledger.ui.categories.CategoriesRoute
import com.tioledger.ui.components.TioAppBar
import com.tioledger.ui.components.TioBottomNavigation
import com.tioledger.ui.components.TioEmptyState
import com.tioledger.ui.components.TioNavigationItem
import com.tioledger.ui.design.TioSpacing
import com.tioledger.ui.loans.LoanDetailsRoute
import com.tioledger.ui.loans.LoansRoute
import com.tioledger.ui.navigation.MainGraph
import com.tioledger.ui.navigation.MainRoute
import com.tioledger.ui.navigation.RootRoute
import com.tioledger.ui.navigation.TioNavigationGraphs
import com.tioledger.ui.reports.ReportsRoute
import com.tioledger.ui.sms.SmsTransactionReviewRoute
import com.tioledger.ui.transactions.TransactionEntryHost
import com.tioledger.ui.transactions.TransactionsRoute

@Composable
fun RootNavigationHost(
    currentRoute: RootRoute = TioNavigationGraphs.root.startRoute,
    onNavigate: (RootRoute) -> Unit = {},
    content: @Composable (RootRoute) -> Unit = { route ->
        when (route) {
            RootRoute.Splash -> Splash()
            is RootRoute.Main -> {
                when (val destination = route.destination) {
                    MainRoute.Accounts -> AccountsRoute()
                    MainRoute.Budgets ->
                        BudgetsRoute(
                            onNavigate = { target ->
                                onNavigate(RootRoute.Main(target))
                            },
                        )
                    MainRoute.Categories ->
                        CategoriesRoute(
                            onNavigate = { target ->
                                onNavigate(RootRoute.Main(target))
                            },
                        )
                    MainRoute.Loans ->
                        LoansRoute(
                            onNavigate = { target ->
                                onNavigate(RootRoute.Main(target))
                            },
                        )
                    is MainRoute.LoanDetails ->
                        LoanDetailsRoute(
                            loanId = destination.loanId,
                            onNavigateBack = {
                                onNavigate(RootRoute.Main(MainRoute.Loans))
                            },
                        )
                    MainRoute.Transactions ->
                        TransactionsRoute(
                            onAddTransaction = {
                                onNavigate(RootRoute.Main(MainRoute.TransactionEntry))
                            },
                            onReviewSms = {
                                onNavigate(RootRoute.Main(MainRoute.SmsTransactionReview))
                            },
                            onNavigate = { target ->
                                onNavigate(RootRoute.Main(target))
                            },
                        )
                    MainRoute.TransactionEntry ->
                        TransactionEntryHost(
                            onNavigateBack = {
                                onNavigate(RootRoute.Main(MainRoute.Transactions))
                            },
                        )
                    MainRoute.Reports ->
                        ReportsRoute(
                            onNavigate = { target ->
                                onNavigate(RootRoute.Main(target))
                            },
                        )
                    MainRoute.SmsTransactionReview ->
                        SmsTransactionReviewRoute(
                            onNavigateBack = {
                                onNavigate(RootRoute.Main(MainRoute.Transactions))
                            },
                        )
                    else -> MainPlaceholderDestination(destination)
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
