@file:Suppress("FunctionName")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tioledger.ui.more

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tioledger.ui.components.TioAppBar
import com.tioledger.ui.components.TioBottomNavigation
import com.tioledger.ui.components.TioIcon
import com.tioledger.ui.components.TioListItem
import com.tioledger.ui.design.TioSpacing
import com.tioledger.ui.navigation.MainRoute
import com.tioledger.ui.navigation.TioNavigationGraphs
import com.tioledger.ui.navigation.bottomNavigationModel

private data class MoreEntry(
    val title: String,
    val route: MainRoute,
    val subtitle: String? = null,
    val value: String? = null,
)

private data class MoreSection(
    val title: String?,
    val entries: List<MoreEntry>,
)

private val moreSections =
    listOf(
        MoreSection(
            title = null,
            entries =
                listOf(
                    MoreEntry(
                        title = "Accounts Setting",
                        route = MainRoute.AccountSettings,
                        subtitle = "Account Group, Accounts, Include in totals, Transfer-Expense setting",
                    ),
                    MoreEntry("Budget Setting", MainRoute.BudgetSettings),
                ),
        ),
        MoreSection(
            title = "Trans.",
            entries =
                listOf(
                    MoreEntry(
                        title = "Transaction Settings",
                        route = MainRoute.TransactionSettings,
                        subtitle = "Monthly Start Date, Carry-over Setting, Period, Other",
                    ),
                    MoreEntry("Repeat Setting", MainRoute.RepeatSettings),
                ),
        ),
        MoreSection(
            title = "Category/Accounts",
            entries =
                listOf(
                    MoreEntry("Income Category Setting", MainRoute.IncomeCategories),
                    MoreEntry("Expenses Category Setting", MainRoute.ExpenseCategories),
                    MoreEntry(
                        title = "Accounts Setting",
                        route = MainRoute.AccountSettings,
                        subtitle = "Account Group, Accounts, Include in totals, Transfer-Expense setting",
                    ),
                    MoreEntry("Budget Setting", MainRoute.BudgetSettings),
                ),
        ),
        MoreSection(
            title = "Settings",
            entries =
                listOf(
                    MoreEntry(
                        title = "Main Currency Setting",
                        route = MainRoute.CurrencySettings,
                        subtitle = "INR(₹)",
                    ),
                    MoreEntry("Alarm Setting", MainRoute.AlarmSettings),
                    MoreEntry("Style", MainRoute.Style),
                ),
        ),
    )

@Composable
fun MoreRoute(onNavigate: (MainRoute) -> Unit = {}) {
    MoreScreen(onNavigate = onNavigate)
}

@Composable
fun MoreScreen(
    onNavigate: (MainRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bottomNavigation = TioNavigationGraphs.main.bottomNavigationModel(MainRoute.More)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TioAppBar(
                title = "Settings",
                actions = {
                    Text(
                        text = "0.1.0",
                        modifier = Modifier.padding(end = TioSpacing.lg),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
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
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding,
        ) {
            moreSections.forEach { section ->
                section.title?.let { title ->
                    item(key = "header-$title") {
                        Text(
                            text = title,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f))
                                    .padding(horizontal = TioSpacing.lg, vertical = TioSpacing.md),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                }
                items(section.entries) { entry ->
                    TioListItem(
                        title = entry.title,
                        subtitle = entry.subtitle,
                        leading = {
                            TioIcon(
                                token = entry.route.icon,
                                contentDescription = entry.title,
                            )
                        },
                        trailing =
                            entry.value?.let { value ->
                                {
                                    Text(
                                        text = value,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            },
                        onClick = { onNavigate(entry.route) },
                    )
                }
                item(key = "divider-${section.title ?: "top"}") {
                    HorizontalDivider()
                }
            }
        }
    }
}
