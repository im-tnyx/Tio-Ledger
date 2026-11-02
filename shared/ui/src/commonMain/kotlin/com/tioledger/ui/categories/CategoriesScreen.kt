@file:Suppress("FunctionName")

package com.tioledger.ui.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.tioledger.domain.model.CategoryType
import com.tioledger.ui.components.TioAppBar
import com.tioledger.ui.components.TioBottomNavigation
import com.tioledger.ui.components.TioEmptyState
import com.tioledger.ui.components.TioErrorState
import com.tioledger.ui.components.TioFilterChip
import com.tioledger.ui.components.TioLoadingState
import com.tioledger.ui.components.TioSectionHeader
import com.tioledger.ui.design.TioSpacing
import com.tioledger.ui.navigation.MainRoute
import com.tioledger.ui.navigation.TioNavigationGraphs
import com.tioledger.ui.navigation.bottomNavigationModel
import org.koin.compose.koinInject

@Composable
fun CategoriesRoute(
    initialType: CategoryType = CategoryType.EXPENSE,
    viewModel: CategoriesViewModel = koinInject(),
    onNavigate: (MainRoute) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    CategoriesScreen(
        state = state,
        initialType = initialType,
        onAction = viewModel::onAction,
        onNavigate = onNavigate,
    )
}

@Composable
fun CategoriesScreen(
    state: CategoriesUiState,
    onAction: (CategoriesAction) -> Unit,
    onNavigate: (MainRoute) -> Unit,
    initialType: CategoryType = CategoryType.EXPENSE,
    modifier: Modifier = Modifier,
) {
    val bottomNavigation = TioNavigationGraphs.main.bottomNavigationModel(MainRoute.Categories)
    var selectedType by remember { mutableStateOf(initialType) }
    var subcategoryEnabled by remember { mutableStateOf(true) }
    val selectedGroup = state.groups.firstOrNull { it.type == selectedType }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TioAppBar(
                title = if (selectedType == CategoryType.EXPENSE) "Exp." else "Income",
                actions = {
                    TextButton(onClick = { onAction(CategoriesAction.AddClicked) }) {
                        Text("+")
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
                modifier = Modifier.fillMaxWidth().padding(horizontal = TioSpacing.lg, vertical = TioSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Subcategory", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Switch(checked = subcategoryEnabled, onCheckedChange = { subcategoryEnabled = it })
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = TioSpacing.lg),
                horizontalArrangement = Arrangement.spacedBy(TioSpacing.sm),
            ) {
                TioFilterChip(
                    label = "Exp.",
                    selected = selectedType == CategoryType.EXPENSE,
                    onClick = { selectedType = CategoryType.EXPENSE },
                )
                TioFilterChip(
                    label = "Income",
                    selected = selectedType == CategoryType.INCOME,
                    onClick = { selectedType = CategoryType.INCOME },
                )
            }
            state.successMessage?.let { message ->
                CategorySuccessMessage(
                    message = message,
                    onDismiss = { onAction(CategoriesAction.MessageDismissed) },
                )
            }
            when {
                state.isLoading -> TioLoadingState(label = "Loading categories")
                state.loadErrorMessage != null -> {
                    TioErrorState(
                        title = "Categories unavailable",
                        message = state.loadErrorMessage,
                        retryLabel = "Retry",
                        onRetry = { onAction(CategoriesAction.Retry) },
                    )
                }
                state.isEmpty -> {
                    TioEmptyState(
                        title = "No categories",
                        message = "Income and expense categories will appear here after they are added.",
                        action = {
                            TextButton(onClick = { onAction(CategoriesAction.AddClicked) }) {
                                Text("Add category")
                            }
                        },
                    )
                }
                else -> CategoriesGroupedList(selectedGroup)
            }
        }
    }

    if (state.isCreateDialogVisible) {
        CreateCategoryDialog(
            state = state,
            onAction = onAction,
        )
    }
}

@Composable
private fun CategoriesGroupedList(group: CategoryGroupUiModel?) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (group == null) {
            item { Text("No categories", modifier = Modifier.padding(TioSpacing.lg)) }
        } else {
            items(group.categories, key = CategoryRowUiModel::id) { category ->
                CategoryManagementRow(
                    category = category,
                    typeTitle = group.title,
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun CategoryManagementRow(
    category: CategoryRowUiModel,
    typeTitle: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "${category.name}, $typeTitle category"
                }
                .padding(horizontal = TioSpacing.lg, vertical = TioSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TioSpacing.md),
    ) {
        Text(
            text = "−",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.headlineMedium,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(category.name, style = MaterialTheme.typography.bodyLarge)
            if (category.isDefault) {
                Text(
                    text = "Default category",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Text("✎", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("☰", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CategorySuccessMessage(
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
private fun CreateCategoryDialog(
    state: CategoriesUiState,
    onAction: (CategoriesAction) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onAction(CategoriesAction.CreateDismissed) },
        title = { Text("Add category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(TioSpacing.md)) {
                OutlinedTextField(
                    value = state.draftName,
                    onValueChange = { onAction(CategoriesAction.NameChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Name") },
                    singleLine = true,
                    enabled = !state.isSaving,
                )
                Text(
                    text = "Type",
                    style = MaterialTheme.typography.labelLarge,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(TioSpacing.sm)) {
                    TioFilterChip(
                        label = "Expense",
                        selected = state.draftType == CategoryType.EXPENSE,
                        onClick = { onAction(CategoriesAction.TypeChanged(CategoryType.EXPENSE)) },
                    )
                    TioFilterChip(
                        label = "Income",
                        selected = state.draftType == CategoryType.INCOME,
                        onClick = { onAction(CategoriesAction.TypeChanged(CategoryType.INCOME)) },
                    )
                }
                state.validationErrorMessage?.let { message ->
                    CategoryDialogError(message)
                }
                state.persistenceErrorMessage?.let { message ->
                    CategoryDialogError(message)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAction(CategoriesAction.SaveClicked) },
                enabled = !state.isSaving,
            ) {
                Text(if (state.isSaving) "Saving" else "Add")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onAction(CategoriesAction.CreateDismissed) },
                enabled = !state.isSaving,
            ) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun CategoryDialogError(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
    )
}
