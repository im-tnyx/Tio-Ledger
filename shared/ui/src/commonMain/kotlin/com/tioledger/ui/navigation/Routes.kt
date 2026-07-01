package com.tioledger.ui.navigation

import com.tioledger.ui.design.TioIconToken

sealed interface AppRoute {
    val path: String
}

sealed interface RootRoute : AppRoute {
    data object Splash : RootRoute {
        override val path: String = "splash"
    }

    data class Main(
        val destination: MainRoute = MainRoute.Dashboard,
    ) : RootRoute {
        override val path: String = "main/${destination.path}"
    }
}

sealed interface MainRoute : AppRoute {
    val title: String
    val icon: TioIconToken

    data object Dashboard : MainRoute {
        override val path: String = "dashboard"
        override val title: String = "Dashboard"
        override val icon: TioIconToken = TioIconToken.Home
    }

    data object Accounts : MainRoute {
        override val path: String = "accounts"
        override val title: String = "Accounts"
        override val icon: TioIconToken = TioIconToken.Account
    }

    data object Transactions : MainRoute {
        override val path: String = "transactions"
        override val title: String = "Transactions"
        override val icon: TioIconToken = TioIconToken.Transaction
    }

    data object Categories : MainRoute {
        override val path: String = "categories"
        override val title: String = "Categories"
        override val icon: TioIconToken = TioIconToken.Category
    }

    data object Reports : MainRoute {
        override val path: String = "reports"
        override val title: String = "Reports"
        override val icon: TioIconToken = TioIconToken.Analytics
    }

    data object Loans : MainRoute {
        override val path: String = "loans"
        override val title: String = "Loans"
        override val icon: TioIconToken = TioIconToken.Loan
    }

    data object Settings : MainRoute {
        override val path: String = "settings"
        override val title: String = "Settings"
        override val icon: TioIconToken = TioIconToken.Settings
    }
}

data class RootGraph(
    val startRoute: RootRoute,
    val mainEntry: RootRoute.Main,
    val routes: List<RootRoute>,
)

data class MainGraph(
    val startRoute: MainRoute,
    val routes: List<MainRoute>,
    val bottomNavigationRoutes: List<MainRoute>,
)

object TioNavigationGraphs {
    val main: MainGraph =
        MainGraph(
            startRoute = MainRoute.Dashboard,
            routes =
                listOf(
                    MainRoute.Dashboard,
                    MainRoute.Accounts,
                    MainRoute.Transactions,
                    MainRoute.Categories,
                    MainRoute.Reports,
                    MainRoute.Loans,
                    MainRoute.Settings,
                ),
            bottomNavigationRoutes =
                listOf(
                    MainRoute.Dashboard,
                    MainRoute.Accounts,
                    MainRoute.Transactions,
                    MainRoute.Categories,
                    MainRoute.Reports,
                ),
        )

    val root: RootGraph =
        RootGraph(
            startRoute = RootRoute.Splash,
            mainEntry = RootRoute.Main(main.startRoute),
            routes = listOf(RootRoute.Splash, RootRoute.Main(main.startRoute)),
        )
}
