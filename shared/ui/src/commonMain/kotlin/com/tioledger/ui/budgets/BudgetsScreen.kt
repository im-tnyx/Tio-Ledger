@file:Suppress("FunctionName")

package com.tioledger.ui.budgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tioledger.budget.engine.BudgetProgressStatus
import com.tioledger.domain.model.BudgetPeriodType
import com.tioledger.ui.components.TioAppBar
import com.tioledger.ui.components.TioBottomNavigation
import com.tioledger.ui.components.TioCard
import com.tioledger.ui.components.TioEmptyState
import com.tioledger.ui.components.TioErrorState
import com.tioledger.ui.components.TioFilterChip
import com.tioledger.ui.components.TioFloatingActionButton
import com.tioledger.ui.components.TioLoadingState
import com.tioledger.ui.components.TioNavigationItem
import com.tioledger.ui.design.TioSpacing
import com.tioledger.ui.navigation.MainRoute
import com.tioledger.ui.navigation.TioNavigationGraphs
import org.koin.compose.koinInject

@Composable
fun BudgetsRoute(
    viewModel: BudgetsViewModel = koinInject(),
    onNavigate: (MainRoute) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    BudgetsScreen(
        state = state,
        onAction = viewModel::onAction,
        onNavigate = onNavigate,
    )
}

@Composable
fun BudgetsScreen(
    state: BudgetsUiState,
    onAction: (BudgetsAction) -> Unit,
    onNavigate: (MainRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navigationRoutes = TioNavigationGraphs.main.bottomNavigationRoutes
    val navigationItems =
        navigationRoutes.map { route ->
            TioNavigationItem(
                label = route.title,
                icon = route.icon,
                selected = route == MainRoute.Budgets,
            )
        }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TioAppBar(title = "Budgets") },
        floatingActionButton = {
            TioFloatingActionButton(
                onClick = { onAction(BudgetsAction.AddClicked) },
                contentDescription = "Add budget",
            )
        },
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
        ) {
            state.successMessage?.let { message ->
                BudgetSuccessMessage(
                    message = message,
                    onDismiss = { onAction(BudgetsAction.MessageDismissed) },
                )
            }
            when {
                state.isLoading -> TioLoadingState(label = "Loading budgets")
                state.loadErrorMessage != null -> {
                    TioErrorState(
                        title = "Budgets unavailable",
                        message = state.loadErrorMessage,
                        retryLabel = "Retry",
                        onRetry = { onAction(BudgetsAction.Retry) },
                    )
                }
                state.isEmpty -> {
                    TioEmptyState(
                        title = "No budgets",
                        message = "Create a recurring budget to compare your target with current spending.",
                        action = {
                            TextButton(onClick = { onAction(BudgetsAction.AddClicked) }) {
                                Text("Add budget")
                            }
                        },
                    )
                }
                else -> BudgetList(state.budgets, onAction)
            }
        }
    }

    state.editor?.let { editor ->
        if (state.isCategoryPickerVisible) {
            BudgetCategoryPickerDialog(
                options = state.categoryOptions,
                selectedCategoryId = editor.categoryId,
                onAction = onAction,
            )
        } else {
            BudgetEditorDialog(
                editor = editor,
                categoryLabel = state.categoryOptions.selectedName(editor.categoryId),
                isSaving = state.isSaving,
                validationErrorMessage = state.validationErrorMessage,
                persistenceErrorMessage = state.persistenceErrorMessage,
                onAction = onAction,
            )
        }
    }
}

@Composable
private fun BudgetList(
    budgets: List<BudgetRowUiModel>,
    onAction: (BudgetsAction) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(TioSpacing.md),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(TioSpacing.lg),
    ) {
        items(budgets, key = BudgetRowUiModel::id) { budget ->
            BudgetCard(
                budget = budget,
                onClick = { onAction(BudgetsAction.EditClicked(budget.id)) },
            )
        }
    }
}

@Composable
private fun BudgetCard(
    budget: BudgetRowUiModel,
    onClick: () -> Unit,
) {
    val progress = budget.utilizationPermille.coerceIn(0, 1_000) / 1_000f
    val statusColor = budget.status.toStatusColor()
    val semanticDescription =
        "${budget.name}, ${budget.categoryLabel}, ${budget.periodLabel}, " +
            "spent ${budget.spentLabel} of ${budget.targetLabel}, " +
            "remaining ${budget.remainingLabel}, ${budget.statusLabel}"

    TioCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .semantics { contentDescription = semanticDescription },
        elevated = true,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(TioSpacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = budget.name,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "${budget.categoryLabel} · ${budget.periodLabel}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = budget.statusLabel,
                    modifier = Modifier.padding(start = TioSpacing.sm),
                    style = MaterialTheme.typography.labelLarge,
                    color = statusColor,
                )
            }

            Text(
                text = budget.periodDateRange,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(TioSpacing.sm),
            ) {
                BudgetAmountColumn(
                    label = "Target",
                    amount = budget.targetLabel,
                    modifier = Modifier.weight(1f),
                )
                BudgetAmountColumn(
                    label = "Spent",
                    amount = budget.spentLabel,
                    modifier = Modifier.weight(1f),
                )
                BudgetAmountColumn(
                    label = "Remaining",
                    amount = budget.remainingLabel,
                    modifier = Modifier.weight(1f),
                )
            }

            Text(
                text = "Select to edit",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BudgetAmountColumn(
    label: String,
    amount: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = amount,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun BudgetSuccessMessage(
    message: String,
    onDismiss: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = TioSpacing.lg, vertical = TioSpacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(end = TioSpacing.sm),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        TextButton(onClick = onDismiss) {
            Text("Dismiss")
        }
    }
}

@Composable
private fun BudgetEditorDialog(
    editor: BudgetEditorUiState,
    categoryLabel: String,
    isSaving: Boolean,
    validationErrorMessage: String?,
    persistenceErrorMessage: String?,
    onAction: (BudgetsAction) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onAction(BudgetsAction.EditorDismissed) },
        title = { Text(if (editor.isEditing) "Edit budget" else "Add budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(TioSpacing.md)) {
                OutlinedTextField(
                    value = editor.name,
                    onValueChange = { onAction(BudgetsAction.NameChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Name") },
                    singleLine = true,
                    enabled = !isSaving,
                )
                OutlinedTextField(
                    value = editor.amount,
                    onValueChange = { onAction(BudgetsAction.AmountChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Amount") },
                    singleLine = true,
                    enabled = !isSaving,
                )
                OutlinedTextField(
                    value = editor.currencyCode,
                    onValueChange = { onAction(BudgetsAction.CurrencyChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Currency code") },
                    supportingText = { Text("Use a 3-letter ISO code") },
                    singleLine = true,
                    enabled = !isSaving,
                )

                Text(
                    text = "Period",
                    style = MaterialTheme.typography.labelLarge,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(TioSpacing.sm)) {
                    BudgetPeriodType.entries
                        .filterNot { it == BudgetPeriodType.CUSTOM }
                        .forEach { period ->
                            TioFilterChip(
                                label = period.editorLabel(),
                                selected = editor.periodType == period,
                                onClick = {
                                    if (!isSaving) {
                                        onAction(BudgetsAction.PeriodChanged(period))
                                    }
                                },
                            )
                        }
                }

                Text(
                    text = "Category scope",
                    style = MaterialTheme.typography.labelLarge,
                )
                TextButton(
                    onClick = { onAction(BudgetsAction.CategoryClicked) },
                    enabled = !isSaving,
                ) {
                    Text(categoryLabel)
                }

                validationErrorMessage?.let { BudgetDialogError(it) }
                persistenceErrorMessage?.let { BudgetDialogError(it) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAction(BudgetsAction.SaveClicked) },
                enabled = !isSaving,
            ) {
                Text(
                    when {
                        isSaving -> "Saving"
                        editor.isEditing -> "Save"
                        else -> "Add"
                    },
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onAction(BudgetsAction.EditorDismissed) },
                enabled = !isSaving,
            ) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun BudgetCategoryPickerDialog(
    options: List<BudgetCategoryOption>,
    selectedCategoryId: String?,
    onAction: (BudgetsAction) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onAction(BudgetsAction.CategoryPickerDismissed) },
        title = { Text("Select category scope") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(TioSpacing.xs),
            ) {
                items(options, key = { it.id ?: "all-expenses" }) { option ->
                    TextButton(
                        onClick = { onAction(BudgetsAction.CategorySelected(option.id)) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        val prefix = if (option.id == selectedCategoryId) "✓ " else ""
                        Text("$prefix${option.name}")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = { onAction(BudgetsAction.CategoryPickerDismissed) }) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun BudgetDialogError(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
    )
}

@Composable
private fun BudgetProgressStatus.toStatusColor(): Color =
    when (this) {
        BudgetProgressStatus.ON_TRACK -> MaterialTheme.colorScheme.primary
        BudgetProgressStatus.WARNING -> MaterialTheme.colorScheme.tertiary
        BudgetProgressStatus.REACHED, BudgetProgressStatus.EXCEEDED -> MaterialTheme.colorScheme.error
    }

private fun List<BudgetCategoryOption>.selectedName(categoryId: String?): String =
    when {
        categoryId == null -> "All expenses"
        else -> firstOrNull { it.id == categoryId }?.name ?: "Unavailable category"
    }

private fun BudgetPeriodType.editorLabel(): String =
    when (this) {
        BudgetPeriodType.WEEKLY -> "Week"
        BudgetPeriodType.MONTHLY -> "Month"
        BudgetPeriodType.YEARLY -> "Year"
        BudgetPeriodType.CUSTOM -> "Custom"
    }
