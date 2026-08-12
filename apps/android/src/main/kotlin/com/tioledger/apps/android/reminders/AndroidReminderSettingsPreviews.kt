@file:Suppress("FunctionName", "UnusedPrivateMember")

package com.tioledger.apps.android.reminders

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.tioledger.ui.design.TioLedgerTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview
@Composable
private fun ReminderSettingsLightPreview() {
    ReminderSettingsPreview(
        darkTheme = false,
        permissionStatus = AndroidNotificationPermissionStatus.NOT_REQUESTED,
    )
}

@Preview
@Composable
private fun ReminderSettingsDarkPreview() {
    ReminderSettingsPreview(
        darkTheme = true,
        permissionStatus = AndroidNotificationPermissionStatus.GRANTED,
    )
}

@Preview
@Composable
private fun ReminderSettingsLargeTextPreview() {
    ReminderSettingsPreview(
        darkTheme = false,
        permissionStatus = AndroidNotificationPermissionStatus.DENIED,
        fontScale = 1.4f,
        errorMessage = "Could not save the reminder preference. Your saved setting was restored.",
    )
}

@Composable
private fun ReminderSettingsPreview(
    darkTheme: Boolean,
    permissionStatus: AndroidNotificationPermissionStatus,
    fontScale: Float = 1f,
    errorMessage: String? = null,
) {
    TioLedgerTheme(darkTheme = darkTheme) {
        val density = LocalDensity.current
        CompositionLocalProvider(
            LocalDensity provides Density(density = density.density, fontScale = fontScale),
        ) {
            Box(modifier = Modifier.width(360.dp)) {
                androidReminderSettingsScreen(
                    preferences =
                        AndroidReminderPreferences(
                            emiRemindersEnabled = true,
                            budgetRemindersEnabled = false,
                        ),
                    permissionStatus = permissionStatus,
                    errorMessage = errorMessage,
                    onEmiEnabledChange = {},
                    onBudgetEnabledChange = {},
                    onPermissionAction = {},
                    onNavigate = {},
                )
            }
        }
    }
}
