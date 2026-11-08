@file:Suppress("FunctionName")

package com.tioledger.ui.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tioledger.domain.model.CategoryType
import com.tioledger.ui.accounts.AccountEditRoute
import com.tioledger.ui.accounts.AccountManagementRoute
import com.tioledger.ui.accounts.AccountsRoute
import com.tioledger.ui.budgets.BudgetsRoute
import com.tioledger.ui.categories.CategoriesRoute
import com.tioledger.ui.components.TioAppBar
import com.tioledger.ui.components.TioBottomNavigation
import com.tioledger.ui.components.TioEmptyState
import com.tioledger.ui.design.ThemeMode
import com.tioledger.ui.design.TioSpacing
import com.tioledger.ui.loans.LoanDetailsRoute
import com.tioledger.ui.loans.LoansRoute
import com.tioledger.ui.more.AccountGroupEditRoute
import com.tioledger.ui.more.AccountGroupRoute
import com.tioledger.ui.more.AccountSettingsRoute
import com.tioledger.ui.more.CurrencySettingsRoute
import com.tioledger.ui.more.MoreRoute
import com.tioledger.ui.more.RepeatSettingsRoute
import com.tioledger.ui.more.StyleRoute
import com.tioledger.ui.more.TransactionSettingsRoute
import com.tioledger.ui.navigation.MainRoute
import com.tioledger.ui.navigation.RootRoute
import com.tioledger.ui.navigation.TioNavigationGraphs
import com.tioledger.ui.navigation.bottomNavigationModel
import com.tioledger.ui.reports.ReportsRoute
import com.tioledger.ui.sms.SmsTransactionReviewRoute
import com.tioledger.ui.transactions.TransactionsRoute

@Composable
fun RootNavigationHost(
    currentRoute: RootRoute = TioNavigationGraphs.root.startRoute,
    onNavigate: (RootRoute) -> Unit = {},
    settingsContent: @Composable ((MainRoute) -> Unit) -> Unit = { onSettingsNavigate ->
        MainPlaceholderDestination(
            destination = MainRoute.Settings,
            onNavigate = onSettingsNavigate,
        )
    },
    themeMode: ThemeMode = ThemeMode.System,
    onThemeModeChanged: (ThemeMode) -> Unit = {},
    content: @Composable (RootRoute) -> Unit = { route ->
        when (route) {
            RootRoute.Splash -> Splash()
            is RootRoute.Main -> {
                when (val destination = route.destination) {
                    MainRoute.Accounts ->
                        AccountsRoute(
                            onNavigate = { target ->
                                onNavigate(RootRoute.Main(target))
                            },
                        )
                    MainRoute.Budgets ->
                        BudgetsRoute(
                            onNavigate = { target ->
                                onNavigate(RootRoute.Main(target))
                            },
                        )
                    MainRoute.Categories ->
                        CategoriesRoute(
                            initialType = CategoryType.EXPENSE,
                            onNavigate = { target ->
                                onNavigate(RootRoute.Main(target))
                            },
                        )
                    MainRoute.IncomeCategories ->
                        CategoriesRoute(
                            initialType = CategoryType.INCOME,
                            onNavigate = { target ->
                                onNavigate(RootRoute.Main(target))
                            },
                        )
                    MainRoute.ExpenseCategories ->
                        CategoriesRoute(
                            initialType = CategoryType.EXPENSE,
                            onNavigate = { target ->
                                onNavigate(RootRoute.Main(target))
                            },
                        )
                    MainRoute.Loans ->
                        LoansRoute(
                            onNavigate = { target ->
                                onNavigate(RootRoute.Main(target))
                            },
                            onNavigateBack = {
                                onNavigate(RootRoute.Main(MainRoute.Accounts))
                            },
                        )
                    is MainRoute.LoanDetails ->
                        LoanDetailsRoute(
                            loanId = destination.loanId,
                            onNavigateBack = {
                                onNavigate(RootRoute.Main(MainRoute.Accounts))
                            },
                        )
                    MainRoute.Transactions ->
                        TransactionsRoute(
                            onReviewSms = {
                                onNavigate(RootRoute.Main(MainRoute.SmsTransactionReview))
                            },
                            onNavigate = { target ->
                                onNavigate(RootRoute.Main(target))
                            },
                        )
                    MainRoute.Reports ->
                        ReportsRoute(
                            onNavigate = { target ->
                                onNavigate(RootRoute.Main(target))
                            },
                        )
                    MainRoute.More ->
                        MoreRoute(
                            onNavigate = { target ->
                                onNavigate(RootRoute.Main(target))
                            },
                        )
                    MainRoute.Configuration ->
                        MoreRoute(
                            onNavigate = { target ->
                                onNavigate(RootRoute.Main(target))
                            },
                        )
                    MainRoute.AccountSettings ->
                        AccountSettingsRoute(
                            onNavigate = { target ->
                                onNavigate(RootRoute.Main(target))
                            },
                            onNavigateBack = {
                                onNavigate(RootRoute.Main(MainRoute.More))
                            },
                        )
                    MainRoute.AccountManager ->
                        AccountManagementRoute(
                            onNavigate = { target ->
                                onNavigate(RootRoute.Main(target))
                            },
                            onNavigateBack = {
                                onNavigate(RootRoute.Main(MainRoute.AccountSettings))
                            },
                        )
                    MainRoute.AccountAdd ->
                        AccountEditRoute(
                            accountId = null,
                            onNavigateBack = {
                                onNavigate(RootRoute.Main(MainRoute.AccountManager))
                            },
                        )
                    is MainRoute.AccountEdit ->
                        AccountEditRoute(
                            accountId = destination.accountId,
                            onNavigateBack = {
                                onNavigate(RootRoute.Main(MainRoute.AccountManager))
                            },
                        )
                    MainRoute.AccountGroup ->
                        AccountGroupRoute(
                            onNavigate = { target ->
                                onNavigate(RootRoute.Main(target))
                            },
                            onNavigateBack = {
                                onNavigate(RootRoute.Main(MainRoute.AccountSettings))
                            },
                        )
                    MainRoute.AccountGroupAdd ->
                        AccountGroupEditRoute(
                            groupId = null,
                            onNavigateBack = {
                                onNavigate(RootRoute.Main(MainRoute.AccountGroup))
                            },
                        )
                    is MainRoute.AccountGroupEdit ->
                        AccountGroupEditRoute(
                            groupId = destination.groupId,
                            onNavigateBack = {
                                onNavigate(RootRoute.Main(MainRoute.AccountGroup))
                            },
                        )
                    MainRoute.BudgetSettings ->
                        BudgetsRoute(
                            onNavigate = { target ->
                                onNavigate(RootRoute.Main(target))
                            },
                        )
                    MainRoute.TransactionSettings ->
                        TransactionSettingsRoute(
                            onNavigate = { target ->
                                onNavigate(RootRoute.Main(target))
                            },
                            onNavigateBack = {
                                onNavigate(RootRoute.Main(MainRoute.More))
                            },
                        )
                    MainRoute.RepeatSettings ->
                        RepeatSettingsRoute(
                            onNavigate = { target ->
                                onNavigate(RootRoute.Main(target))
                            },
                            onNavigateBack = {
                                onNavigate(RootRoute.Main(MainRoute.More))
                            },
                        )
                    MainRoute.Style ->
                        StyleRoute(
                            themeMode = themeMode,
                            onThemeModeChanged = onThemeModeChanged,
                            onNavigate = { target ->
                                onNavigate(RootRoute.Main(target))
                            },
                            onNavigateBack = {
                                onNavigate(RootRoute.Main(MainRoute.More))
                            },
                        )
                    MainRoute.SmsTransactionReview ->
                        SmsTransactionReviewRoute(
                            onNavigateBack = {
                                onNavigate(RootRoute.Main(MainRoute.Transactions))
                            },
                        )
                    MainRoute.CurrencySettings ->
                        CurrencySettingsRoute(
                            onNavigateBack = {
                                onNavigate(RootRoute.Main(MainRoute.More))
                            },
                            onNavigate = { target ->
                                onNavigate(RootRoute.Main(target))
                            },
                        )
                    MainRoute.Settings ->
                        settingsContent { target ->
                            onNavigate(RootRoute.Main(target))
                        }
                    else ->
                        MainPlaceholderDestination(
                            destination = destination,
                            onNavigate = { target ->
                                onNavigate(RootRoute.Main(target))
                            },
                        )
                }
            }
        }
    },
) {
    content(currentRoute)
}

@Composable
internal fun MainPlaceholderDestination(
    destination: MainRoute,
    onNavigate: (MainRoute) -> Unit,
) {
    val bottomNavigation = TioNavigationGraphs.main.bottomNavigationModel(destination)

    Scaffold(
        topBar = { TioAppBar(title = destination.title) },
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
