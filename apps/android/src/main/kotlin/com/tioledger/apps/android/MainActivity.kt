package com.tioledger.apps.android

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.mutableStateOf
import com.tioledger.apps.android.reminders.AndroidNotificationPermissionStatus
import com.tioledger.apps.android.reminders.AndroidReminderSettingsService
import com.tioledger.apps.android.reminders.ReminderNavigationIntent
import com.tioledger.apps.android.reminders.androidReminderSettingsRoute
import com.tioledger.apps.android.reminders.shouldReconcileNotificationPermissionChange
import com.tioledger.bootstrap.diagnostics.StartupDiagnostics
import com.tioledger.ui.design.ThemeMode
import com.tioledger.ui.navigation.MainRoute
import com.tioledger.ui.navigation.RootRoute
import com.tioledger.ui.navigation.TioNavigationGraphs
import com.tioledger.ui.shell.TioAppShell

class MainActivity : ComponentActivity() {
    private val currentRoute = mutableStateOf<RootRoute>(TioNavigationGraphs.root.mainEntry)
    private val settingsRefreshToken = mutableStateOf(0L)
    private val themeMode = mutableStateOf(ThemeMode.System)
    private var lastObservedNotificationPermissionStatus: AndroidNotificationPermissionStatus? = null
    private val backNavigationCallback =
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateBackOrFinish()
            }
        }
    private val reminderSettingsService: AndroidReminderSettingsService by lazy {
        (application as TioAndroidApplication)
            .koinApplication
            .koin
            .get()
    }
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            refreshNotificationPermissionState()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, backNavigationCallback)
        ReminderNavigationIntent.routeOrNull(intent)?.let(::replaceRouteFromExternalEvent)
        val diagnostics =
            (application as TioAndroidApplication)
                .koinApplication
                .koin
                .get<StartupDiagnostics>()
        lastObservedNotificationPermissionStatus = reminderSettingsService.snapshot().permissionStatus
        themeMode.value = loadThemeMode()

        setContent {
            TioAppShell(
                diagnostics = diagnostics,
                darkTheme = isSystemInDarkTheme(),
                themeMode = themeMode.value,
                onThemeModeChanged = ::updateThemeMode,
                currentRoute = currentRoute.value,
                onNavigate = ::navigateTo,
                settingsContent = { onNavigate ->
                    androidReminderSettingsRoute(
                        settingsService = reminderSettingsService,
                        refreshToken = settingsRefreshToken.value,
                        onLaunchRuntimePermission = notificationPermissionLauncher::launch,
                        onOpenNotificationSettings = ::openNotificationSettings,
                        onNavigate = onNavigate,
                    )
                },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        refreshNotificationPermissionState()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        ReminderNavigationIntent.routeOrNull(intent)?.let(::replaceRouteFromExternalEvent)
    }

    private fun refreshNotificationPermissionState() {
        val currentStatus = reminderSettingsService.snapshot().permissionStatus
        if (
            shouldReconcileNotificationPermissionChange(
                previous = lastObservedNotificationPermissionStatus,
                current = currentStatus,
            )
        ) {
            reminderSettingsService.onPermissionStateChanged()
        }
        lastObservedNotificationPermissionStatus = currentStatus
        settingsRefreshToken.value += 1L
    }

    private fun openNotificationSettings() {
        startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
        )
    }

    private fun loadThemeMode(): ThemeMode {
        return when (getSharedPreferences(THEME_PREFERENCES, MODE_PRIVATE).getString(THEME_MODE_KEY, null)) {
            "light" -> ThemeMode.Light
            "dark" -> ThemeMode.Dark
            else -> ThemeMode.System
        }
    }

    private fun updateThemeMode(mode: ThemeMode) {
        themeMode.value = mode
        getSharedPreferences(THEME_PREFERENCES, MODE_PRIVATE)
            .edit()
            .putString(
                THEME_MODE_KEY,
                when (mode) {
                    ThemeMode.System -> "system"
                    ThemeMode.Light -> "light"
                    ThemeMode.Dark -> "dark"
                },
            ).apply()
    }

    private fun navigateTo(route: RootRoute) {
        if (currentRoute.value == route) return
        currentRoute.value = route
    }

    private fun replaceRouteFromExternalEvent(route: RootRoute) {
        currentRoute.value = route
    }

    private fun navigateBackOrFinish() {
        val targetRoute = systemBackTargetOrNull(currentRoute.value)
        if (targetRoute != null) {
            currentRoute.value = targetRoute
            return
        }

        backNavigationCallback.isEnabled = false
        onBackPressedDispatcher.onBackPressed()
        backNavigationCallback.isEnabled = true
    }
}

private const val THEME_PREFERENCES = "tio_ledger_preferences"
private const val THEME_MODE_KEY = "theme_mode"

internal fun systemBackTargetOrNull(currentRoute: RootRoute): RootRoute? =
    when (currentRoute) {
        RootRoute.Splash -> null
        is RootRoute.Main ->
            when (currentRoute.destination) {
                MainRoute.Dashboard,
                MainRoute.Transactions,
                MainRoute.Reports,
                MainRoute.Accounts,
                MainRoute.More,
                -> null
                MainRoute.Categories,
                MainRoute.IncomeCategories,
                MainRoute.ExpenseCategories,
                MainRoute.Budgets,
                MainRoute.Settings,
                MainRoute.Configuration,
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
                -> RootRoute.Main(MainRoute.More)
                MainRoute.AccountSettings -> RootRoute.Main(MainRoute.Configuration)
                MainRoute.AccountManager,
                MainRoute.AccountGroup,
                -> RootRoute.Main(MainRoute.AccountSettings)
                MainRoute.SmsTransactionReview,
                -> RootRoute.Main(MainRoute.Transactions)
                is MainRoute.LoanDetails -> RootRoute.Main(MainRoute.Loans)
                MainRoute.Loans,
                -> TioNavigationGraphs.root.mainEntry
            }
    }
