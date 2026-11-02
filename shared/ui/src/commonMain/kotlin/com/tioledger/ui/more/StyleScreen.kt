@file:Suppress("FunctionName")

package com.tioledger.ui.more

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tioledger.ui.components.TioAppBar
import com.tioledger.ui.components.TioBottomNavigation
import com.tioledger.ui.components.TioListItem
import com.tioledger.ui.design.ThemeMode
import com.tioledger.ui.navigation.MainRoute
import com.tioledger.ui.navigation.TioNavigationGraphs
import com.tioledger.ui.navigation.bottomNavigationModel

private data class ThemeOption(
    val mode: ThemeMode,
    val title: String,
    val subtitle: String,
)

private val themeOptions =
    listOf(
        ThemeOption(ThemeMode.System, "System default", "Follow the device appearance"),
        ThemeOption(ThemeMode.Light, "Light", "Use the light appearance"),
        ThemeOption(ThemeMode.Dark, "Dark", "Use the dark appearance"),
    )

@Composable
fun StyleRoute(
    themeMode: ThemeMode,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onNavigate: (MainRoute) -> Unit = {},
    onNavigateBack: () -> Unit = {},
) {
    StyleScreen(
        themeMode = themeMode,
        onThemeModeChanged = onThemeModeChanged,
        onNavigate = onNavigate,
        onNavigateBack = onNavigateBack,
    )
}

@Composable
fun StyleScreen(
    themeMode: ThemeMode,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onNavigate: (MainRoute) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bottomNavigation = TioNavigationGraphs.main.bottomNavigationModel(MainRoute.Style)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TioAppBar(
                title = "Style",
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
            item {
                Text(
                    text = "Theme",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            items(themeOptions, key = ThemeOption::mode) { option ->
                TioListItem(
                    title = option.title,
                    subtitle = option.subtitle,
                    trailing = {
                        Text(
                            text = if (themeMode == option.mode) "✓" else "",
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "${option.title} theme" },
                    onClick = { onThemeModeChanged(option.mode) },
                )
            }
        }
    }
}
