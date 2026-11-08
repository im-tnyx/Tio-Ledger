package com.tioledger.ui.navigation

import com.tioledger.ui.components.TioNavigationItem

class MainBottomNavigationModel internal constructor(
    val routes: List<MainRoute>,
    val items: List<TioNavigationItem>,
) {
    fun routeFor(item: TioNavigationItem): MainRoute? {
        return items.indexOf(item).takeIf { index -> index >= 0 }?.let(routes::get)
    }

    fun navigate(
        item: TioNavigationItem,
        onNavigate: (MainRoute) -> Unit,
    ) {
        routeFor(item)?.let(onNavigate)
    }
}

fun MainGraph.bottomNavigationModel(currentRoute: MainRoute): MainBottomNavigationModel {
    val routes = bottomNavigationRoutes
    val selectedRoute = currentRoute.bottomNavigationParent()
    val items =
        routes.map { route ->
            TioNavigationItem(
                label = route.title,
                icon = route.icon,
                selected = route == selectedRoute,
            )
        }

    return MainBottomNavigationModel(
        routes = routes,
        items = items,
    )
}

private fun MainRoute.bottomNavigationParent(): MainRoute =
    when (this) {
        MainRoute.Categories,
        MainRoute.IncomeCategories,
        MainRoute.ExpenseCategories,
        MainRoute.Budgets,
        MainRoute.Settings,
        MainRoute.Configuration,
        MainRoute.AccountSettings,
        MainRoute.AccountManager,
        MainRoute.AccountAdd,
        is MainRoute.AccountEdit,
        MainRoute.AccountGroup,
        MainRoute.BudgetSettings,
        MainRoute.TransactionSettings,
        MainRoute.RepeatSettings,
        MainRoute.CurrencySettings,
        MainRoute.SubCurrencySettings,
        MainRoute.PasscodeSettings,
        MainRoute.AlarmSettings,
        MainRoute.ApplicationIconSettings,
        MainRoute.LanguageSettings,
        MainRoute.AdTrackingSettings,
        MainRoute.CopyPasteSettings,
        MainRoute.PcManager,
        MainRoute.Backup,
        MainRoute.Style,
        MainRoute.Help,
        MainRoute.Feedback,
        -> MainRoute.More
        else -> this
    }
