@file:Suppress("FunctionName")

package com.tioledger.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.tioledger.ui.components.TioAppBar
import com.tioledger.ui.components.TioBottomNavigation
import com.tioledger.ui.components.TioIcon
import com.tioledger.ui.design.TioIconToken
import com.tioledger.ui.design.TioSpacing
import com.tioledger.ui.navigation.MainRoute
import com.tioledger.ui.navigation.TioNavigationGraphs
import com.tioledger.ui.navigation.bottomNavigationModel

private enum class ReflectionTiming(val label: String) {
    ON_DATE("On the date"),
    FIRST_DAY_OF_MONTH("On the first day of every month"),
}

@Composable
fun RepeatSettingsRoute(
    onNavigate: (MainRoute) -> Unit = {},
    onNavigateBack: () -> Unit = {},
) {
    RepeatSettingsScreen(
        onNavigate = onNavigate,
        onNavigateBack = onNavigateBack,
    )
}

@Composable
fun RepeatSettingsScreen(
    onNavigate: (MainRoute) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var timing by remember { mutableStateOf(ReflectionTiming.ON_DATE) }
    var timingDialogVisible by remember { mutableStateOf(false) }
    val bottomNavigation = TioNavigationGraphs.main.bottomNavigationModel(MainRoute.RepeatSettings)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TioAppBar(
                title = MainRoute.RepeatSettings.title,
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("‹ Settings")
                    }
                },
                actions = {
                    TextButton(onClick = {}) {
                        TioIcon(
                            token = TioIconToken.Delete,
                            contentDescription = "Delete repeat setting",
                        )
                    }
                    TextButton(onClick = {}) {
                        TioIcon(
                            token = TioIconToken.Add,
                            contentDescription = "Add repeat setting",
                        )
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
        Column(
            modifier =
                Modifier
                    .padding(padding)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .fillMaxSize(),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { timingDialogVisible = true }
                        .semantics {
                            contentDescription = "Timing of reflection: ${timing.label}"
                        }
                        .padding(horizontal = TioSpacing.lg, vertical = TioSpacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Timing of reflection", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = timing.label,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }

    if (timingDialogVisible) {
        AlertDialog(
            onDismissRequest = { timingDialogVisible = false },
            title = { Text("Timing of reflection") },
            text = {
                Column {
                    ReflectionTiming.entries.forEach { option ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        timing = option
                                        timingDialogVisible = false
                                    }
                                    .padding(vertical = TioSpacing.md),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = option.label,
                                color =
                                    if (option == timing) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                            )
                            if (option == timing) {
                                Text("✓", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { timingDialogVisible = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}
