@file:Suppress("FunctionName")

package com.tioledger.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.tioledger.application.usecase.analytics.SpendingReportPeriod
import com.tioledger.ui.components.TioAmountText
import com.tioledger.ui.components.TioAmountTone
import com.tioledger.ui.components.TioAppBar
import com.tioledger.ui.components.TioBottomNavigation
import com.tioledger.ui.components.TioCard
import com.tioledger.ui.components.TioCurrencyBadge
import com.tioledger.ui.components.TioEmptyState
import com.tioledger.ui.components.TioErrorState
import com.tioledger.ui.components.TioFilterChip
import com.tioledger.ui.components.TioLoadingState
import com.tioledger.ui.components.TioNavigationItem
import com.tioledger.ui.components.TioSummaryCard
import com.tioledger.ui.design.TioSpacing
import com.tioledger.ui.navigation.MainRoute
import com.tioledger.ui.navigation.TioNavigationGraphs
import org.koin.compose.koinInject

@Composable
fun ReportsRoute(
    viewModel: ReportsViewModel = koinInject(),
    onNavigate: (MainRoute) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    ReportsScreen(
        state = state,
        onAction = viewModel::onAction,
        onNavigate = onNavigate,
    )
}

@Composable
fun ReportsScreen(
    state: ReportsUiState,
    onAction: (ReportsAction) -> Unit,
    onNavigate: (MainRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navigationRoutes = TioNavigationGraphs.main.bottomNavigationRoutes
    val navigationItems =
        navigationRoutes.map { route ->
            TioNavigationItem(
                label = route.title,
                icon = route.icon,
                selected = false,
            )
        }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TioAppBar(title = "Reports") },
        bottomBar = {
            TioBottomNavigation(
                items = navigationItems,
                onItemSelected = { selectedItem ->
                    val selectedIndex = navigationItems.indexOf(selectedItem)
                    if (selectedIndex >= 0) {
                        onNavigate(navigationRoutes[selectedIndex])
                    }
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
            verticalArrangement = Arrangement.spacedBy(TioSpacing.md),
        ) {
            ReportsPeriodSelector(
                selectedPeriod = state.selectedPeriod,
                onPeriodSelected = { period ->
                    onAction(ReportsAction.PeriodSelected(period))
                },
            )

            when {
                state.isLoading -> TioLoadingState(label = "Loading spending analytics")
                state.loadErrorMessage != null -> {
                    TioErrorState(
                        title = "Reports unavailable",
                        message = state.loadErrorMessage,
                        retryLabel = "Retry",
                        onRetry = { onAction(ReportsAction.Retry) },
                    )
                }
                state.isEmpty -> {
                    TioEmptyState(
                        title = "No spending data",
                        message = "No income or expense transactions are available for the selected period.",
                    )
                }
                else ->
                    state.report?.let { report ->
                        ReportsContent(report = report)
                    }
            }
        }
    }
}

@Composable
private fun ReportsPeriodSelector(
    selectedPeriod: SpendingReportPeriod,
    onPeriodSelected: (SpendingReportPeriod) -> Unit,
) {
    Row(
        modifier = Modifier.padding(horizontal = TioSpacing.lg),
        horizontalArrangement = Arrangement.spacedBy(TioSpacing.sm),
    ) {
        SpendingReportPeriod.entries.forEach { period ->
            TioFilterChip(
                label = period.toChipLabel(),
                selected = selectedPeriod == period,
                onClick = { onPeriodSelected(period) },
            )
        }
    }
}

@Composable
private fun ReportsContent(report: ReportsPeriodUiModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(TioSpacing.md),
        contentPadding = PaddingValues(horizontal = TioSpacing.lg, vertical = TioSpacing.sm),
    ) {
        item {
            TioCard(elevated = true) {
                Column(verticalArrangement = Arrangement.spacedBy(TioSpacing.xs)) {
                    Text(
                        text = "${report.periodLabel} report",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = report.dateRangeLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        items(report.currencySections, key = ReportsCurrencySectionUiModel::currencyCode) { section ->
            ReportsCurrencySection(section)
        }
    }
}

@Composable
private fun ReportsCurrencySection(section: ReportsCurrencySectionUiModel) {
    TioCard(
        modifier = Modifier.fillMaxWidth(),
        elevated = true,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(TioSpacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Currency summary",
                    style = MaterialTheme.typography.titleMedium,
                )
                TioCurrencyBadge(section.currencyCode)
            }

            TioSummaryCard(
                label = "Income",
                value = section.incomeLabel,
                tone = TioAmountTone.Positive,
            )
            TioSummaryCard(
                label = "Expense",
                value = section.expenseLabel,
                tone = TioAmountTone.Negative,
            )
            TioSummaryCard(
                label = "Net",
                value = section.netLabel,
                tone = section.netMinorUnits.toAmountTone(),
            )

            BreakdownSection(
                title = "By category",
                rows = section.categoryBreakdown,
                emptyLabel = "No expense categories in this period.",
            )

            HorizontalDivider()

            BreakdownSection(
                title = "By account",
                rows = section.accountBreakdown,
                emptyLabel = "No spending accounts in this period.",
            )
        }
    }
}

@Composable
private fun BreakdownSection(
    title: String,
    rows: List<ReportsBreakdownRowUiModel>,
    emptyLabel: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(TioSpacing.sm)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
        )

        if (rows.isEmpty()) {
            Text(
                text = emptyLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            rows.forEach { row ->
                BreakdownRow(row)
            }
        }
    }
}

@Composable
private fun BreakdownRow(row: ReportsBreakdownRowUiModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = row.label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TioAmountText(
            amount = row.amountLabel,
            tone = TioAmountTone.Negative,
        )
    }
}

private fun SpendingReportPeriod.toChipLabel(): String =
    when (this) {
        SpendingReportPeriod.WEEKLY -> "Week"
        SpendingReportPeriod.MONTHLY -> "Month"
        SpendingReportPeriod.YEARLY -> "Year"
    }

private fun Long.toAmountTone(): TioAmountTone =
    when {
        this > 0L -> TioAmountTone.Positive
        this < 0L -> TioAmountTone.Negative
        else -> TioAmountTone.Neutral
    }
