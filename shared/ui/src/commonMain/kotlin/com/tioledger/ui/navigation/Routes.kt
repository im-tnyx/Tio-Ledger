package com.tioledger.ui.navigation

sealed interface TioRoute {
    val path: String

    data object Splash : TioRoute {
        override val path: String = "splash"
    }

    data object Main : TioRoute {
        override val path: String = "main"
    }

    data object Accounts : TioRoute {
        override val path: String = "accounts"
    }
}

data class RootGraph(
    val startRoute: TioRoute,
    val routes: List<TioRoute>,
)

data class MainGraph(
    val startRoute: TioRoute,
    val routes: List<TioRoute>,
)

object TioNavigationGraphs {
    val root =
        RootGraph(
            startRoute = TioRoute.Splash,
            routes = listOf(TioRoute.Splash, TioRoute.Main),
        )

    val main =
        MainGraph(
            startRoute = TioRoute.Accounts,
            routes = listOf(TioRoute.Accounts),
        )
}
