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
        val destination: MainRoute = MainRoute.Transactions,
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

    data object SmsTransactionReview : MainRoute {
        override val path: String = "transactions/sms-review"
        override val title: String = "Review SMS"
        override val icon: TioIconToken = TioIconToken.Transaction
    }

    data object Categories : MainRoute {
        override val path: String = "categories"
        override val title: String = "Categories"
        override val icon: TioIconToken = TioIconToken.Category
    }

    data object IncomeCategories : MainRoute {
        override val path: String = "settings/categories/income"
        override val title: String = "Income Category Setting"
        override val icon: TioIconToken = TioIconToken.Category
    }

    data object ExpenseCategories : MainRoute {
        override val path: String = "settings/categories/expense"
        override val title: String = "Expenses Category Setting"
        override val icon: TioIconToken = TioIconToken.Category
    }

    data object Budgets : MainRoute {
        override val path: String = "budgets"
        override val title: String = "Budgets"
        override val icon: TioIconToken = TioIconToken.Budget
    }

    data object Reports : MainRoute {
        override val path: String = "reports"
        override val title: String = "Stats"
        override val icon: TioIconToken = TioIconToken.Analytics
    }

    data object More : MainRoute {
        override val path: String = "more"
        override val title: String = "More"
        override val icon: TioIconToken = TioIconToken.More
    }

    data object Configuration : MainRoute {
        override val path: String = "settings/configuration"
        override val title: String = "Configuration"
        override val icon: TioIconToken = TioIconToken.Configuration
    }

    data object AccountSettings : MainRoute {
        override val path: String = "settings/accounts"
        override val title: String = "Accounts Setting"
        override val icon: TioIconToken = TioIconToken.Account
    }

    data object AccountManager : MainRoute {
        override val path: String = "settings/accounts/manage"
        override val title: String = "Accounts"
        override val icon: TioIconToken = TioIconToken.Account
    }

    data object AccountGroup : MainRoute {
        override val path: String = "settings/account-group"
        override val title: String = "Account Group"
        override val icon: TioIconToken = TioIconToken.Account
    }

    data object BudgetSettings : MainRoute {
        override val path: String = "settings/budgets"
        override val title: String = "Budget Setting"
        override val icon: TioIconToken = TioIconToken.Budget
    }

    data object TransactionSettings : MainRoute {
        override val path: String = "settings/transactions"
        override val title: String = "Transaction Settings"
        override val icon: TioIconToken = TioIconToken.Transaction
    }

    data object RepeatSettings : MainRoute {
        override val path: String = "settings/repeat"
        override val title: String = "Repeat Setting"
        override val icon: TioIconToken = TioIconToken.Settings
    }

    data object CurrencySettings : MainRoute {
        override val path: String = "settings/currency"
        override val title: String = "Currency settings"
        override val icon: TioIconToken = TioIconToken.Currency
    }

    data object SubCurrencySettings : MainRoute {
        override val path: String = "settings/currency/sub"
        override val title: String = "Sub Currency Setting"
        override val icon: TioIconToken = TioIconToken.Currency
    }

    data object PasscodeSettings : MainRoute {
        override val path: String = "settings/passcode"
        override val title: String = "Passcode"
        override val icon: TioIconToken = TioIconToken.Passcode
    }

    data object AlarmSettings : MainRoute {
        override val path: String = "settings/alarm"
        override val title: String = "Alarm Setting"
        override val icon: TioIconToken = TioIconToken.Alarm
    }

    data object ApplicationIconSettings : MainRoute {
        override val path: String = "settings/application-icon"
        override val title: String = "Application Icon"
        override val icon: TioIconToken = TioIconToken.ApplicationIcon
    }

    data object LanguageSettings : MainRoute {
        override val path: String = "settings/language"
        override val title: String = "Language Setting"
        override val icon: TioIconToken = TioIconToken.Language
    }

    data object AdTrackingSettings : MainRoute {
        override val path: String = "settings/ad-tracking"
        override val title: String = "Allow ad tracking"
        override val icon: TioIconToken = TioIconToken.AdTracking
    }

    data object CopyPasteSettings : MainRoute {
        override val path: String = "settings/copy-paste"
        override val title: String = "Copy-Paste Settings"
        override val icon: TioIconToken = TioIconToken.CopyPaste
    }

    data object PcManager : MainRoute {
        override val path: String = "settings/pc-manager"
        override val title: String = "PC Manager"
        override val icon: TioIconToken = TioIconToken.Computer
    }

    data object Backup : MainRoute {
        override val path: String = "settings/backup"
        override val title: String = "Backup"
        override val icon: TioIconToken = TioIconToken.Backup
    }

    data object Style : MainRoute {
        override val path: String = "settings/style"
        override val title: String = "Style"
        override val icon: TioIconToken = TioIconToken.Style
    }

    data object Help : MainRoute {
        override val path: String = "settings/help"
        override val title: String = "Help"
        override val icon: TioIconToken = TioIconToken.Help
    }

    data object Feedback : MainRoute {
        override val path: String = "settings/feedback"
        override val title: String = "Feedback"
        override val icon: TioIconToken = TioIconToken.Feedback
    }

    data object Loans : MainRoute {
        override val path: String = "loans"
        override val title: String = "Loans"
        override val icon: TioIconToken = TioIconToken.Loan
    }

    data class LoanDetails(
        val loanId: String,
    ) : MainRoute {
        override val path: String = "loans/$loanId"
        override val title: String = "Loan details"
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
            startRoute = MainRoute.Transactions,
            routes =
                listOf(
                    MainRoute.Dashboard,
                    MainRoute.Accounts,
                    MainRoute.Transactions,
                    MainRoute.SmsTransactionReview,
                    MainRoute.Categories,
                    MainRoute.IncomeCategories,
                    MainRoute.ExpenseCategories,
                    MainRoute.Budgets,
                    MainRoute.Reports,
                    MainRoute.More,
                    MainRoute.Configuration,
                    MainRoute.AccountSettings,
                    MainRoute.AccountManager,
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
                    MainRoute.Loans,
                    MainRoute.Settings,
                ),
            bottomNavigationRoutes =
                listOf(
                    MainRoute.Transactions,
                    MainRoute.Reports,
                    MainRoute.Accounts,
                    MainRoute.More,
                ),
        )

    val root: RootGraph =
        RootGraph(
            startRoute = RootRoute.Splash,
            mainEntry = RootRoute.Main(main.startRoute),
            routes = listOf(RootRoute.Splash, RootRoute.Main(main.startRoute)),
        )
}
