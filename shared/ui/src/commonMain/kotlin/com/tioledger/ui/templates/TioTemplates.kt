@file:Suppress("FunctionName")

package com.tioledger.ui.templates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.tioledger.ui.components.TioAccountRow
import com.tioledger.ui.components.TioAmountText
import com.tioledger.ui.components.TioAmountTone
import com.tioledger.ui.components.TioAppBar
import com.tioledger.ui.components.TioBottomNavigation
import com.tioledger.ui.components.TioCard
import com.tioledger.ui.components.TioCategoryRow
import com.tioledger.ui.components.TioEmptyState
import com.tioledger.ui.components.TioFloatingActionButton
import com.tioledger.ui.components.TioListItem
import com.tioledger.ui.components.TioNavigationItem
import com.tioledger.ui.components.TioSearchField
import com.tioledger.ui.components.TioSectionHeader
import com.tioledger.ui.design.TioDimensions
import com.tioledger.ui.design.TioIconToken
import com.tioledger.ui.design.TioSpacing

data class TioListTemplateItem(
    val title: String,
    val subtitle: String,
    val amount: String? = null,
)

data class TioDashboardSummaryItem(
    val label: String,
    val value: String,
    val tone: TioAmountTone = TioAmountTone.Neutral,
)

@Composable
fun TioListScreenTemplate(
    title: String,
    items: List<TioListTemplateItem>,
    modifier: Modifier = Modifier,
    searchValue: String = "",
    onSearchValueChange: (String) -> Unit = {},
    emptyTitle: String = "No records",
    emptyMessage: String = "Records will appear here after you add them.",
) {
    Scaffold(
        modifier = modifier,
        topBar = { TioAppBar(title = title) },
        floatingActionButton = { TioFloatingActionButton(onClick = {}) },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .padding(padding)
                    .padding(TioSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(TioSpacing.md),
        ) {
            TioSearchField(
                value = searchValue,
                onValueChange = onSearchValueChange,
            )
            if (items.isEmpty()) {
                TioEmptyState(
                    title = emptyTitle,
                    message = emptyMessage,
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(TioSpacing.xs)) {
                    items(items) { item ->
                        TioListItem(
                            title = item.title,
                            subtitle = item.subtitle,
                            trailing =
                                item.amount?.let { amount ->
                                    { TioAmountText(amount = amount) }
                                },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TioDetailScreenTemplate(
    title: String,
    summary: String,
    rows: List<TioListTemplateItem>,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TioAppBar(title = title) },
    ) { padding ->
        LazyColumn(
            modifier =
                Modifier
                    .padding(padding)
                    .padding(TioSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(TioSpacing.md),
        ) {
            item {
                TioCard(elevated = true) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            items(rows) { row ->
                TioListItem(
                    title = row.title,
                    subtitle = row.subtitle,
                    trailing =
                        row.amount?.let { amount ->
                            { TioAmountText(amount = amount) }
                        },
                )
            }
        }
    }
}

@Composable
fun TioFormScreenTemplate(
    title: String,
    fields: List<String>,
    modifier: Modifier = Modifier,
    primaryActionLabel: String = "Save",
    secondaryActionLabel: String = "Cancel",
) {
    Scaffold(
        modifier = modifier,
        topBar = { TioAppBar(title = title) },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .padding(padding)
                    .padding(TioSpacing.lg)
                    .widthIn(max = TioDimensions.dialogMaxWidth),
            verticalArrangement = Arrangement.spacedBy(TioSpacing.md),
        ) {
            fields.forEach { field ->
                TioSearchField(
                    value = "",
                    onValueChange = {},
                    placeholder = field,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(TioSpacing.md),
            ) {
                OutlinedButton(
                    onClick = {},
                    modifier = Modifier.weight(1f),
                ) {
                    Text(secondaryActionLabel)
                }
                Button(
                    onClick = {},
                    modifier = Modifier.weight(1f),
                ) {
                    Text(primaryActionLabel)
                }
            }
        }
    }
}

@Composable
fun TioDashboardTemplate(
    summaryItems: List<TioDashboardSummaryItem>,
    modifier: Modifier = Modifier,
    navigationItems: List<TioNavigationItem> = TioTemplatePreviewData.navigationItems,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TioAppBar(title = "Tio Ledger") },
        bottomBar = {
            TioBottomNavigation(
                items = navigationItems,
                onItemSelected = {},
            )
        },
        floatingActionButton = { TioFloatingActionButton(onClick = {}) },
    ) { padding ->
        LazyColumn(
            modifier =
                Modifier
                    .padding(padding)
                    .padding(TioSpacing.lg),
            contentPadding = PaddingValues(bottom = TioSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(TioSpacing.md),
        ) {
            item {
                TioSectionHeader(title = "Overview")
            }
            items(summaryItems) { item ->
                TioCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        TioAmountText(
                            amount = item.value,
                            tone = item.tone,
                        )
                    }
                }
            }
            item {
                TioSectionHeader(title = "Accounts")
                TioAccountRow(
                    name = "Cash",
                    accountType = "Wallet",
                    balance = "₹0.00",
                )
            }
            item {
                TioSectionHeader(title = "Categories")
                TioCategoryRow(
                    name = "Food",
                    subtitle = "Monthly placeholder",
                )
            }
        }
    }
}

object TioTemplatePreviewData {
    val navigationItems: List<TioNavigationItem> =
        listOf(
            TioNavigationItem("Home", TioIconToken.Home, selected = true),
            TioNavigationItem("Calendar", TioIconToken.Calendar, selected = false),
            TioNavigationItem("Stats", TioIconToken.Analytics, selected = false),
            TioNavigationItem("Settings", TioIconToken.Settings, selected = false),
        )

    val listItems: List<TioListTemplateItem> =
        listOf(
            TioListTemplateItem("Cash", "Wallet", "₹0.00"),
            TioListTemplateItem("Food", "Category", "₹0.00"),
            TioListTemplateItem("Transfer", "Placeholder", "₹0.00"),
        )

    val summaryItems: List<TioDashboardSummaryItem> =
        listOf(
            TioDashboardSummaryItem("Income", "₹0.00", TioAmountTone.Positive),
            TioDashboardSummaryItem("Expense", "₹0.00", TioAmountTone.Negative),
            TioDashboardSummaryItem("Total", "₹0.00", TioAmountTone.Neutral),
        )
}
