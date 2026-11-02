@file:Suppress("FunctionName")

package com.tioledger.ui.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.tioledger.ui.components.TioAppBar
import com.tioledger.ui.components.TioBottomNavigation
import com.tioledger.ui.design.TioSpacing
import com.tioledger.ui.navigation.MainRoute
import com.tioledger.ui.navigation.TioNavigationGraphs
import com.tioledger.ui.navigation.bottomNavigationModel

private data class TransactionSetting(
    val label: String,
    val value: String,
)

private val transactionSettings =
    listOf(
        TransactionSetting("Monthly Start Date", "1"),
        TransactionSetting("Weekly Start Day", "Sunday"),
        TransactionSetting("Carry-over Setting", "Off"),
        TransactionSetting("Period Setting", "Monthly"),
        TransactionSetting("Income-Expenses Color Setting", "Exp."),
        TransactionSetting("Autocomplete", "On"),
        TransactionSetting("Time Input", "None, Desc."),
        TransactionSetting("Start Screen (Daily/Calendar)", "Daily"),
        TransactionSetting("Swipe", "To Change Date"),
        TransactionSetting("Show description", "Off"),
        TransactionSetting("Input order", "From Amount"),
        TransactionSetting("Note button setting", "Off"),
    )

@Composable
fun TransactionSettingsRoute(
    onNavigate: (MainRoute) -> Unit = {},
    onNavigateBack: () -> Unit = {},
) {
    TransactionSettingsScreen(
        onNavigate = onNavigate,
        onNavigateBack = onNavigateBack,
    )
}

@Composable
fun TransactionSettingsScreen(
    onNavigate: (MainRoute) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bottomNavigation = TioNavigationGraphs.main.bottomNavigationModel(MainRoute.TransactionSettings)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TioAppBar(
                title = MainRoute.TransactionSettings.title,
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("‹ Settings")
                    }
                },
            )
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
        LazyColumn(
            modifier =
                Modifier
                    .padding(padding)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .fillMaxSize(),
        ) {
            items(transactionSettings, key = TransactionSetting::label) { setting ->
                TransactionSettingRow(setting)
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun TransactionSettingRow(setting: TransactionSetting) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "${setting.label}: ${setting.value}" }
                .padding(horizontal = TioSpacing.lg, vertical = TioSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(TioSpacing.md),
    ) {
        Text(
            text = setting.label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = setting.value,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
