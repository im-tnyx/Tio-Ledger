package com.tioledger.apps.android.reminders

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.tioledger.apps.android.R
import com.tioledger.ui.components.TioAppBar
import com.tioledger.ui.components.TioBottomNavigation
import com.tioledger.ui.components.TioIconAvatar
import com.tioledger.ui.components.TioSecondaryButton
import com.tioledger.ui.components.TioSectionHeader
import com.tioledger.ui.design.TioDimensions
import com.tioledger.ui.design.TioIconToken
import com.tioledger.ui.design.TioSpacing
import com.tioledger.ui.navigation.MainRoute
import com.tioledger.ui.navigation.TioNavigationGraphs
import com.tioledger.ui.navigation.bottomNavigationModel

internal enum class ReminderSettingsPermissionAction {
    NONE,
    REQUEST_PERMISSION,
    OPEN_NOTIFICATION_SETTINGS,
}

internal fun AndroidNotificationPermissionStatus.settingsAction(): ReminderSettingsPermissionAction =
    when (this) {
        AndroidNotificationPermissionStatus.NOT_REQUIRED,
        AndroidNotificationPermissionStatus.GRANTED -> ReminderSettingsPermissionAction.NONE

        AndroidNotificationPermissionStatus.NOT_REQUESTED -> ReminderSettingsPermissionAction.REQUEST_PERMISSION
        AndroidNotificationPermissionStatus.DENIED,
        AndroidNotificationPermissionStatus.REVOKED -> ReminderSettingsPermissionAction.OPEN_NOTIFICATION_SETTINGS
    }

@Composable
fun AndroidReminderSettingsRoute(
    settingsService: AndroidReminderSettingsService,
    refreshToken: Long,
    onLaunchRuntimePermission: (String) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onNavigate: (MainRoute) -> Unit,
) {
    var snapshot by remember(settingsService) { mutableStateOf(settingsService.snapshot()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val preferenceWriteError = stringResource(R.string.settings_reminders_save_error)
    val permissionRequestError = stringResource(R.string.settings_notifications_permission_error)
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    fun refresh(clearError: Boolean = true) {
        snapshot = settingsService.snapshot()
        if (clearError) {
            errorMessage = null
        }
    }

    LaunchedEffect(refreshToken) {
        refresh()
    }

    DisposableEffect(activity, settingsService) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    settingsService.onPermissionStateChanged()
                    refresh()
                }
            }
        activity?.lifecycle?.addObserver(observer)
        onDispose {
            activity?.lifecycle?.removeObserver(observer)
        }
    }

    AndroidReminderSettingsScreen(
        preferences = snapshot.preferences,
        permissionStatus = snapshot.permissionStatus,
        errorMessage = errorMessage,
        onEmiEnabledChange = { enabled ->
            val stored = settingsService.setEmiRemindersEnabled(enabled)
            refresh(clearError = false)
            errorMessage = if (stored) null else preferenceWriteError
        },
        onBudgetEnabledChange = { enabled ->
            val stored = settingsService.setBudgetRemindersEnabled(enabled)
            refresh(clearError = false)
            errorMessage = if (stored) null else preferenceWriteError
        },
        onPermissionAction = {
            when (snapshot.permissionStatus.settingsAction()) {
                ReminderSettingsPermissionAction.NONE -> Unit
                ReminderSettingsPermissionAction.OPEN_NOTIFICATION_SETTINGS -> {
                    errorMessage = null
                    onOpenNotificationSettings()
                }
                ReminderSettingsPermissionAction.REQUEST_PERMISSION -> {
                    val runtimePermission = snapshot.runtimePermission
                    if (runtimePermission == null || !settingsService.markPermissionRequestAttempted()) {
                        refresh(clearError = false)
                        errorMessage = permissionRequestError
                    } else {
                        refresh()
                        onLaunchRuntimePermission(runtimePermission)
                    }
                }
            }
        },
        onNavigate = onNavigate,
    )
}

@Composable
internal fun AndroidReminderSettingsScreen(
    preferences: AndroidReminderPreferences,
    permissionStatus: AndroidNotificationPermissionStatus,
    errorMessage: String?,
    onEmiEnabledChange: (Boolean) -> Unit,
    onBudgetEnabledChange: (Boolean) -> Unit,
    onPermissionAction: () -> Unit,
    onNavigate: (MainRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bottomNavigation = TioNavigationGraphs.main.bottomNavigationModel(MainRoute.Settings)
    val permissionTitle = permissionStatusTitle(permissionStatus)
    val permissionDescription = permissionStatusDescription(permissionStatus)
    val permissionAction = permissionStatus.settingsAction()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TioAppBar(title = stringResource(R.string.settings_title))
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
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
        ) {
            TioSectionHeader(title = stringResource(R.string.settings_reminders_section))
            Text(
                text = stringResource(R.string.settings_reminders_description),
                modifier = Modifier.padding(horizontal = TioSpacing.lg, vertical = TioSpacing.xs),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ReminderToggleRow(
                title = stringResource(R.string.settings_emi_reminders_title),
                supportingText = stringResource(R.string.settings_emi_reminders_description),
                checked = preferences.emiRemindersEnabled,
                onCheckedChange = onEmiEnabledChange,
            )
            HorizontalDivider()
            ReminderToggleRow(
                title = stringResource(R.string.settings_budget_reminders_title),
                supportingText = stringResource(R.string.settings_budget_reminders_description),
                checked = preferences.budgetRemindersEnabled,
                onCheckedChange = onBudgetEnabledChange,
            )

            TioSectionHeader(
                title = stringResource(R.string.settings_notification_delivery_section),
                modifier = Modifier.padding(top = TioSpacing.md),
            )
            NotificationStatusRow(
                title = permissionTitle,
                supportingText = permissionDescription,
            )

            when (permissionAction) {
                ReminderSettingsPermissionAction.NONE -> Unit
                ReminderSettingsPermissionAction.REQUEST_PERMISSION -> {
                    TioSecondaryButton(
                        label = stringResource(R.string.settings_notifications_allow_action),
                        onClick = onPermissionAction,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = TioSpacing.lg),
                    )
                }
                ReminderSettingsPermissionAction.OPEN_NOTIFICATION_SETTINGS -> {
                    TioSecondaryButton(
                        label = stringResource(R.string.settings_notifications_open_settings_action),
                        onClick = onPermissionAction,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = TioSpacing.lg),
                    )
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = TioSpacing.lg, vertical = TioSpacing.md),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun ReminderToggleRow(
    title: String,
    supportingText: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = TioDimensions.minTouchTarget)
                .padding(horizontal = TioSpacing.lg, vertical = TioSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TioSpacing.md),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(TioSpacing.xs),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.semantics { contentDescription = title },
        )
    }
}

@Composable
private fun NotificationStatusRow(
    title: String,
    supportingText: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = TioSpacing.lg, vertical = TioSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TioIconAvatar(TioIconToken.Notification)
        Spacer(modifier = Modifier.width(TioSpacing.md))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(TioSpacing.xs),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun permissionStatusTitle(status: AndroidNotificationPermissionStatus): String =
    stringResource(
        when (status) {
            AndroidNotificationPermissionStatus.NOT_REQUIRED -> R.string.settings_notifications_status_available
            AndroidNotificationPermissionStatus.NOT_REQUESTED -> R.string.settings_notifications_status_not_requested
            AndroidNotificationPermissionStatus.GRANTED -> R.string.settings_notifications_status_available
            AndroidNotificationPermissionStatus.DENIED -> R.string.settings_notifications_status_denied
            AndroidNotificationPermissionStatus.REVOKED -> R.string.settings_notifications_status_disabled
        },
    )

@Composable
private fun permissionStatusDescription(status: AndroidNotificationPermissionStatus): String =
    stringResource(
        when (status) {
            AndroidNotificationPermissionStatus.NOT_REQUIRED -> R.string.settings_notifications_not_required_description
            AndroidNotificationPermissionStatus.NOT_REQUESTED -> R.string.settings_notifications_not_requested_description
            AndroidNotificationPermissionStatus.GRANTED -> R.string.settings_notifications_granted_description
            AndroidNotificationPermissionStatus.DENIED -> R.string.settings_notifications_denied_description
            AndroidNotificationPermissionStatus.REVOKED -> R.string.settings_notifications_revoked_description
        },
    )
