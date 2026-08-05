package com.tioledger.ui.navigation

import com.tioledger.ui.components.TioNavigationItem

data class MainBottomNavigationModel internal constructor(
    val routes: List<MainRoute>,
    val items: List<TioNavigationItem>,
) {
    fun routeFor(item: TioNavigationItem): MainRoute? =
        items.indexOf(item).takeIf { index -> index >= 0 }?.let(routes::get)

    fun navigate(
        item: TioNavigationItem,
        onNavigate: (MainRoute) -> Unit,
    ) {
        routeFor(item)?.let(onNavigate)
    }
}

fun MainGraph.bottomNavigationModel(currentRoute: MainRoute): MainBottomNavigationModel {
    val routes = bottomNavigationRoutes
    val items =
        routes.map { route ->
            TioNavigationItem(
                label = route.title,
                icon = route.icon,
                selected = route == currentRoute,
            )
        }

    return MainBottomNavigationModel(
        routes = routes,
        items = items,
    )
}
