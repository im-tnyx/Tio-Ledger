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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.tioledger.domain.model.CategoryType
import com.tioledger.ui.components.TioAppBar
import com.tioledger.ui.components.TioBottomNavigation
import com.tioledger.ui.components.TioCategoryRow
import com.tioledger.ui.components.TioEmptyState
import com.tioledger.ui.components.TioErrorState
import com.tioledger.ui.components.TioFilterChip
import com.tioledger.ui.components.TioFloatingActionButton
import com.tioledger.ui.components.TioLoadingState
import com.tioledger.ui.components.TioNavigationItem
import com.tioledger.ui.components.TioSectionHeader
import com.tioledger.ui.design.TioSpacing
import com.tioledger.ui.navigation.MainRoute
import com.tioledger.ui.navigation.TioNavigationGraphs
import org.koin.compose.koinInject

@Composable
fun CategoriesRoute(
    viewModel: CategoriesViewModel = koinInject(),
    onNavigate: (MainRoute) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    CategoriesScreen(
        state = state,
        onAction = viewModel::onAction,
        onNavigate = onNavigate,
    )
}

@Composable
fun CategoriesScreen(
    state: CategoriesUiState,
    onAction: (CategoriesAction) -> Unit,
    onNavigate: (MainRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navigationRoutes = TioNavigationGraphs.main.bottomNavigationRoutes
    val navigationItems =
        navigationRoutes.map { route ->
            TioNavigationItem(
                label = route.title,
                icon = route.icon,
                selected = route == MainRoute.Categories,
            )
        }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TioAppBar(title = "Categories") },
        floatingActionButton = {
            TioFloatingActionButton(
                onClick = { onAction(CategoriesAction.AddClicked) },
                contentDescription = "Add category",
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
                else -> CategoriesGroupedList(state.groups)
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
private fun CategoriesGroupedList(groups: List<CategoryGroupUiModel>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        groups.forEach { group ->
            item(key = "header-${group.type}") {
                TioSectionHeader(title = "${group.title} (${group.categories.size})")
            }
            items(group.categories, key = CategoryRowUiModel::id) { category ->
                TioCategoryRow(
                    name = category.name,
                    subtitle = if (category.isDefault) "Default category" else null,
                    modifier =
                        Modifier.semantics {
                            contentDescription =
                                if (category.isDefault) {
                                    "${category.name}, default ${group.title.lowercase()} category"
                                } else {
                                    "${category.name}, ${group.title.lowercase()} category"
                                }
                        },
                )
                HorizontalDivider()
            }
        }
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
            modifier = Modifier.weight(1f),
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
