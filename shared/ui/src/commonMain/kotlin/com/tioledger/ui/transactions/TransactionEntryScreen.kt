@file:Suppress("FunctionName")

package com.tioledger.ui.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import com.tioledger.ui.components.TioAppBar
import com.tioledger.ui.components.TioEmptyState
import com.tioledger.ui.components.TioIcon
import com.tioledger.ui.design.TioIconToken
import com.tioledger.ui.design.TioSpacing
import org.koin.compose.koinInject

@Composable
fun TransactionEntryRoute(
    viewModel: TransactionEntryViewModel = koinInject(),
    onNavigateBack: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    TransactionEntryScreen(
        state = state,
        onAction = viewModel::onAction,
        onNavigateBack = onNavigateBack,
    )
}

@Composable
fun TransactionEntryScreen(
    state: TransactionEntryUiState,
    onAction: (TransactionEntryAction) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TioAppBar(
                title = "Add Transaction",
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        TioIcon(TioIconToken.Close, contentDescription = "Cancel")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> TransactionEntryLoading()
            state.errorMessage != null -> TransactionEntryError(state.errorMessage)
            else -> {
                Column(
                    modifier =
                        Modifier
                            .padding(padding)
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(TioSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(TioSpacing.lg),
                ) {
                    TransactionTypeSelector(
                        selectedType = state.transactionType,
                        onTypeSelected = { onAction(TransactionEntryAction.TypeChanged(it)) },
                    )

                    AmountInputCard(
                        amount = state.amount,
                        onAmountChanged = { onAction(TransactionEntryAction.AmountChanged(it)) },
                        transactionType = state.transactionType,
                    )

                    AccountSelectorCard(
                        selectedAccount = state.selectedAccount,
                        onAccountClicked = { onAction(TransactionEntryAction.AccountClicked) },
                    )

                    CategorySelectorCard(
                        selectedCategory = state.selectedCategory,
                        onCategoryClicked = { onAction(TransactionEntryAction.CategoryClicked) },
                    )

                    DateSelectorCard(
                        selectedDate = state.selectedDate,
                        onDateClicked = { onAction(TransactionEntryAction.DateClicked) },
                    )

                    NoteInputCard(
                        note = state.note,
                        onNoteChanged = { onAction(TransactionEntryAction.NoteChanged(it)) },
                    )

                    TagsPlaceholderCard()

                    Spacer(modifier = Modifier.height(TioSpacing.md))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(TioSpacing.md),
                    ) {
                        TextButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = { onAction(TransactionEntryAction.SaveClicked) },
                            modifier =
                                Modifier.weight(1f).semantics {
                                    contentDescription = "Save transaction button"
                                },
                            enabled = state.canSave,
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionTypeSelector(
    selectedType: TransactionType,
    onTypeSelected: (TransactionType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val types = TransactionType.entries
    TabRow(
        selectedTabIndex = types.indexOf(selectedType),
        modifier =
            modifier.semantics {
                contentDescription = "Transaction type selector"
            },
    ) {
        types.forEach { type ->
            Tab(
                selected = type == selectedType,
                onClick = { onTypeSelected(type) },
                text = { Text(type.displayName) },
            )
        }
    }
}

@Composable
private fun AmountInputCard(
    amount: String,
    onAmountChanged: (String) -> Unit,
    transactionType: TransactionType,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
    ) {
        Column(
            modifier = Modifier.padding(TioSpacing.lg),
        ) {
            Text(
                text = "Amount",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(TioSpacing.sm))
            OutlinedTextField(
                value = amount,
                onValueChange = onAmountChanged,
                modifier =
                    Modifier.fillMaxWidth().semantics {
                        contentDescription = "Transaction amount input"
                    },
                textStyle =
                    MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color =
                            when (transactionType) {
                                TransactionType.Expense -> MaterialTheme.colorScheme.error
                                TransactionType.Income -> MaterialTheme.colorScheme.primary
                                TransactionType.Transfer -> MaterialTheme.colorScheme.secondary
                            },
                    ),
                placeholder = { Text("0.00", style = MaterialTheme.typography.headlineLarge) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                prefix = { Text("₹ ", style = MaterialTheme.typography.headlineLarge) },
            )
        }
    }
}

@Composable
private fun AccountSelectorCard(
    selectedAccount: String?,
    onAccountClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SelectorCard(
        label = "Account",
        value = selectedAccount ?: "Select account",
        icon = TioIconToken.Account,
        onClick = onAccountClicked,
        modifier =
            modifier.semantics {
                contentDescription = "Account selector"
            },
    )
}

@Composable
private fun CategorySelectorCard(
    selectedCategory: String?,
    onCategoryClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SelectorCard(
        label = "Category",
        value = selectedCategory ?: "Select category",
        icon = TioIconToken.Category,
        onClick = onCategoryClicked,
        modifier =
            modifier.semantics {
                contentDescription = "Category selector"
            },
    )
}

@Composable
private fun DateSelectorCard(
    selectedDate: String,
    onDateClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SelectorCard(
        label = "Date",
        value = selectedDate,
        icon = TioIconToken.Calendar,
        onClick = onDateClicked,
        modifier =
            modifier.semantics {
                contentDescription = "Date selector"
            },
    )
}

@Composable
private fun SelectorCard(
    label: String,
    value: String,
    icon: TioIconToken,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
    ) {
        Row(
            modifier = Modifier.padding(TioSpacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TioIcon(icon, contentDescription = null)
            Spacer(modifier = Modifier.width(TioSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (value.startsWith("Select")) FontWeight.Normal else FontWeight.Medium,
                    color =
                        if (value.startsWith("Select")) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                )
            }
            TioIcon(TioIconToken.Settings, contentDescription = "Select $label")
        }
    }
}

@Composable
private fun NoteInputCard(
    note: String,
    onNoteChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
    ) {
        Column(
            modifier = Modifier.padding(TioSpacing.lg),
        ) {
            Text(
                text = "Note",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(TioSpacing.sm))
            OutlinedTextField(
                value = note,
                onValueChange = onNoteChanged,
                modifier =
                    Modifier.fillMaxWidth().semantics {
                        contentDescription = "Transaction note input"
                    },
                placeholder = { Text("Add a note (optional)") },
                minLines = 3,
                maxLines = 5,
            )
        }
    }
}

@Composable
private fun TagsPlaceholderCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
    ) {
        Column(
            modifier = Modifier.padding(TioSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Tags",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(TioSpacing.sm))
            Text(
                text = "Tag functionality coming in v2",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun TransactionEntryLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(TioSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(TioSpacing.md))
        Text("Loading transaction form...")
    }
}

@Composable
private fun TransactionEntryError(
    errorMessage: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(TioSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        TioEmptyState(
            title = "Error",
            message = errorMessage,
        )
    }
}
