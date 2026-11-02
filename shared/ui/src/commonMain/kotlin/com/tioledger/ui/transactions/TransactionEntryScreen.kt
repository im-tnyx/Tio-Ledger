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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tioledger.ui.components.TioAppBar
import com.tioledger.ui.components.TioEmptyState
import com.tioledger.ui.components.TioErrorState
import com.tioledger.ui.components.TioIcon
import com.tioledger.ui.components.TioListItem
import com.tioledger.ui.components.TioLoadingState
import com.tioledger.ui.design.TioIconToken
import com.tioledger.ui.design.TioSpacing
import org.koin.compose.koinInject

@Composable
fun TransactionEntryRoute(
    viewModel: TransactionEntryViewModel = koinInject(),
    onNavigateBack: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    val event by viewModel.event.collectAsState()

    LaunchedEffect(event) {
        if (event is TransactionEntryEvent.TransactionSaved) {
            viewModel.onAction(TransactionEntryAction.EventConsumed)
            onNavigateBack()
        }
    }

    TransactionEntryScreen(
        state = state,
        onAction = viewModel::onAction,
        onNavigateBack = onNavigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
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
                title = state.transactionType.displayName,
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("‹ Trans.")
                    }
                },
                actions = {
                    TextButton(onClick = {}) {
                        Text("☆≡")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> {
                TioLoadingState(
                    label = "Loading transaction form...",
                    modifier = Modifier.padding(padding),
                )
            }
            state.loadErrorMessage != null -> {
                TioErrorState(
                    title = "Transaction entry unavailable",
                    message = state.loadErrorMessage,
                    retryLabel = "Retry",
                    onRetry = { onAction(TransactionEntryAction.Retry) },
                    modifier = Modifier.padding(padding),
                )
            }
            state.accountOptions.isEmpty() -> {
                TioEmptyState(
                    title = "No accounts",
                    message = "Create an account before recording a transaction.",
                    modifier = Modifier.padding(padding),
                )
            }
            !state.isTransfer && state.categoryOptions.isEmpty() -> {
                TioEmptyState(
                    title = "No categories",
                    message = "Create a category before recording a ${state.transactionType.displayName.lowercase()} transaction.",
                    modifier = Modifier.padding(padding),
                )
            }
            else -> {
                Column(
                    modifier =
                        Modifier
                            .padding(padding)
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = TioSpacing.lg, vertical = TioSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(TioSpacing.sm),
                ) {
                    TransactionTypeSelector(
                        selectedType = state.transactionType,
                        onTypeSelected = { onAction(TransactionEntryAction.TypeChanged(it)) },
                    )

                    InlineMessageCard(
                        message = state.validationErrorMessage,
                        tone = MessageTone.Error,
                        onDismiss = { onAction(TransactionEntryAction.MessageDismissed) },
                    )
                    InlineMessageCard(
                        message = state.persistenceErrorMessage,
                        tone = MessageTone.Error,
                        onDismiss = { onAction(TransactionEntryAction.MessageDismissed) },
                    )
                    InlineMessageCard(
                        message = state.saveSuccessMessage,
                        tone = MessageTone.Success,
                        onDismiss = { onAction(TransactionEntryAction.MessageDismissed) },
                    )

                    ReferenceDateRow(
                        selectedDate = state.selectedDate,
                        onDateClicked = { onAction(TransactionEntryAction.DateClicked) },
                    )

                    ReferenceAmountRow(
                        amount = state.amount,
                        onAmountChanged = { onAction(TransactionEntryAction.AmountChanged(it)) },
                        transactionType = state.transactionType,
                    )

                    if (state.isTransfer) {
                        if (state.isFeeEnabled) {
                            ReferenceFeeRow(
                                fee = state.fee,
                                onEdit = { onAction(TransactionEntryAction.FeesClicked) },
                                onRemove = { onAction(TransactionEntryAction.FeeRemoved) },
                            )
                        } else {
                            TextButton(onClick = { onAction(TransactionEntryAction.FeesClicked) }) {
                                Text("Fees")
                            }
                        }
                    }

                    if (state.isTransfer) {
                        ReferenceSelectorRow(
                            label = "From account",
                            value = state.selectedAccount?.name,
                            onClick = { onAction(TransactionEntryAction.SourceAccountClicked) },
                        )
                        ReferenceSelectorRow(
                            label = "To account",
                            value = state.selectedTargetAccount?.name,
                            onClick = { onAction(TransactionEntryAction.TargetAccountClicked) },
                        )
                    } else {
                        ReferenceSelectorRow(
                            label = "Category",
                            value = state.selectedCategory?.name,
                            onClick = { onAction(TransactionEntryAction.CategoryClicked) },
                        )
                        ReferenceSelectorRow(
                            label = "Account",
                            value = state.selectedAccount?.name,
                            onClick = { onAction(TransactionEntryAction.SourceAccountClicked) },
                        )
                    }

                    ReferenceNoteRow(
                        note = state.note,
                        onNoteChanged = { onAction(TransactionEntryAction.NoteChanged(it)) },
                    )

                    HorizontalDivider()

                    ReferenceDescriptionRow()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(TioSpacing.md),
                    ) {
                        Button(
                            onClick = { onAction(TransactionEntryAction.SaveClicked) },
                            modifier = Modifier.weight(1.6f).semantics { contentDescription = "Save transaction button" },
                            enabled = state.canSave,
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = state.transactionType.color,
                                    contentColor = state.transactionType.saveContentColor,
                                ),
                        ) {
                            Text(if (state.isSaving) "Saving..." else "Save")
                        }
                        TextButton(
                            onClick = { onAction(TransactionEntryAction.SaveClicked) },
                            modifier = Modifier.weight(1f).border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium),
                            enabled = state.canSave,
                        ) {
                            Text("Continue")
                        }
                    }
                }
            }
        }
    }

    if (state.activeAccountPicker != null) {
        AccountPickerDialog(
            title =
                if (state.activeAccountPicker == TransactionAccountPickerTarget.Source) {
                    "Select account"
                } else {
                    "Select destination account"
                },
            options = state.accountOptions,
            onDismiss = { onAction(TransactionEntryAction.AccountPickerDismissed) },
            onSelected = { onAction(TransactionEntryAction.AccountSelected(it)) },
        )
    }

    if (state.isCategoryPickerVisible) {
        CategoryPickerDialog(
            options = state.categoryOptions,
            onDismiss = { onAction(TransactionEntryAction.CategoryPickerDismissed) },
            onSelected = { onAction(TransactionEntryAction.CategorySelected(it)) },
        )
    }

    if (state.isFeeSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { onAction(TransactionEntryAction.FeeSheetDismissed) },
        ) {
            FeeBottomSheetContent(
                fee = state.fee,
                onFeeChanged = { onAction(TransactionEntryAction.FeeChanged(it)) },
                onCancel = { onAction(TransactionEntryAction.FeeSheetDismissed) },
                onSave = { onAction(TransactionEntryAction.FeeConfirmed) },
            )
        }
    }
}

@Composable
private fun TransactionTypeSelector(
    selectedType: TransactionType,
    onTypeSelected: (TransactionType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().semantics { contentDescription = "Transaction type selector" },
        horizontalArrangement = Arrangement.spacedBy(TioSpacing.sm),
    ) {
        TransactionType.entries.forEach { type ->
            val selected = type == selectedType
            TextButton(
                onClick = { onTypeSelected(type) },
                modifier =
                    Modifier
                        .weight(1f)
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) type.color else MaterialTheme.colorScheme.outline,
                            shape = MaterialTheme.shapes.small,
                        ),
            ) {
                Text(
                    text = type.displayName,
                    color = if (selected) type.color else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private val TransactionType.color: Color
    get() =
        when (this) {
            TransactionType.Income -> Color(0xFF4EA1FF)
            TransactionType.Expense -> Color(0xFFFF5D5D)
            TransactionType.Transfer -> MaterialThemeFallbackTransferColor
        }

private val MaterialThemeFallbackTransferColor = Color(0xFFE7E7EA)

private val TransactionType.saveContentColor: Color
    get() =
        when (this) {
            TransactionType.Income, TransactionType.Expense -> Color.White
            TransactionType.Transfer -> Color(0xFF202124)
        }

@Composable
private fun ReferenceDateRow(
    selectedDate: String,
    onDateClicked: () -> Unit,
) {
    ReferenceSelectorRow(
        label = "Date",
        value = selectedDate,
        onClick = onDateClicked,
        trailing = "↻\nRep/Inst.",
    )
}

@Composable
private fun ReferenceAmountRow(
    amount: String,
    onAmountChanged: (String) -> Unit,
    transactionType: TransactionType,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = TioSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TioSpacing.md),
    ) {
        Text("Amount", modifier = Modifier.width(104.dp), style = MaterialTheme.typography.bodyLarge)
        TextField(
            value = amount,
            onValueChange = onAmountChanged,
            modifier = Modifier.weight(1f).semantics { contentDescription = "Transaction amount input" },
            placeholder = { Text("0.00") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = transactionType.color),
            colors =
                TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    errorContainerColor = Color.Transparent,
                    focusedIndicatorColor = transactionType.color,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                ),
        )
    }
}

@Composable
private fun ReferenceFeeRow(
    fee: String,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit).padding(vertical = TioSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TioSpacing.md),
    ) {
        Text("Fees", modifier = Modifier.width(104.dp), style = MaterialTheme.typography.bodyLarge)
        Text(
            text = "₹ $fee",
            modifier = Modifier.weight(1f).padding(vertical = TioSpacing.sm),
            style = MaterialTheme.typography.bodyLarge,
        )
        IconButton(onClick = onRemove) {
            Text("×", style = MaterialTheme.typography.headlineSmall)
        }
    }
    HorizontalDivider()
}

@Composable
private fun FeeBottomSheetContent(
    fee: String,
    onFeeChanged: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(TioSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(TioSpacing.md),
    ) {
        Text("Fees", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = fee,
            onValueChange = onFeeChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Fee amount") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(TioSpacing.md),
        ) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Cancel")
            }
            Button(onClick = onSave, modifier = Modifier.weight(1f)) {
                Text("Save")
            }
        }
    }
}

@Composable
private fun ReferenceSelectorRow(
    label: String,
    value: String?,
    onClick: () -> Unit,
    trailing: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = TioSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TioSpacing.md),
    ) {
        Text(label, modifier = Modifier.width(104.dp), style = MaterialTheme.typography.bodyLarge)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = value ?: "",
                color = if (value == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = TioSpacing.sm),
            )
            HorizontalDivider()
        }
        if (trailing != null) {
            Text(
                text = trailing,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun ReferenceNoteRow(
    note: String,
    onNoteChanged: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = TioSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TioSpacing.md),
    ) {
        Text("Note", modifier = Modifier.width(104.dp), style = MaterialTheme.typography.bodyLarge)
        TextField(
            value = note,
            onValueChange = onNoteChanged,
            modifier = Modifier.weight(1f).semantics { contentDescription = "Transaction note input" },
            singleLine = true,
            placeholder = { Text("Note") },
            colors =
                TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    errorContainerColor = Color.Transparent,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                ),
        )
        Text("!", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ReferenceDescriptionRow() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = TioSpacing.sm, vertical = TioSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Description",
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text("▣", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AmountInputCard(
    amount: String,
    currencyCode: String,
    onAmountChanged: (String) -> Unit,
    transactionType: TransactionType,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(TioSpacing.lg)) {
            Text(
                text = "Amount",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(TioSpacing.sm))
            OutlinedTextField(
                value = amount,
                onValueChange = onAmountChanged,
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Transaction amount input" },
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
                prefix = { Text("$currencyCode ", style = MaterialTheme.typography.headlineLarge) },
            )
        }
    }
}

@Composable
private fun AccountSelectorCard(
    label: String,
    selectedAccount: String?,
    subtitle: String?,
    onAccountClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SelectorCard(
        label = label,
        value = selectedAccount ?: "Select account",
        subtitle = subtitle,
        icon = TioIconToken.Account,
        onClick = onAccountClicked,
        modifier = modifier.semantics { contentDescription = "$label selector" },
    )
}

@Composable
private fun CategorySelectorCard(
    selectedCategory: String?,
    subtitle: String?,
    onCategoryClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SelectorCard(
        label = "Category",
        value = selectedCategory ?: "Select category",
        subtitle = subtitle,
        icon = TioIconToken.Category,
        onClick = onCategoryClicked,
        modifier = modifier.semantics { contentDescription = "Category selector" },
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
        subtitle = "Uses current device time",
        icon = TioIconToken.Calendar,
        onClick = onDateClicked,
        modifier = modifier.semantics { contentDescription = "Date selector" },
    )
}

@Composable
private fun SelectorCard(
    label: String,
    value: String,
    subtitle: String?,
    icon: TioIconToken,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(TioSpacing.xs))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(TioSpacing.lg)) {
            Text(
                text = "Note",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(TioSpacing.sm))
            OutlinedTextField(
                value = note,
                onValueChange = onNoteChanged,
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Transaction note input" },
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
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
                text = "Tag functionality remains out of scope for this milestone",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun AccountPickerDialog(
    title: String,
    options: List<TransactionAccountOption>,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(TioSpacing.xs)) {
                options.forEach { account ->
                    TioListItem(
                        title = account.name,
                        subtitle = account.subtitle,
                        onClick = { onSelected(account.id) },
                    )
                }
            }
        },
    )
}

@Composable
private fun CategoryPickerDialog(
    options: List<TransactionCategoryOption>,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        title = { Text("Select category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(TioSpacing.xs)) {
                options.forEach { category ->
                    TioListItem(
                        title = category.name,
                        subtitle = category.subtitle,
                        onClick = { onSelected(category.id) },
                    )
                }
            }
        },
    )
}

private enum class MessageTone {
    Error,
    Success,
}

@Composable
private fun InlineMessageCard(
    message: String?,
    tone: MessageTone,
    onDismiss: () -> Unit,
) {
    if (message == null) {
        return
    }

    val containerColor =
        when (tone) {
            MessageTone.Error -> MaterialTheme.colorScheme.errorContainer
            MessageTone.Success -> MaterialTheme.colorScheme.secondaryContainer
        }
    val contentColor =
        when (tone) {
            MessageTone.Error -> MaterialTheme.colorScheme.onErrorContainer
            MessageTone.Success -> MaterialTheme.colorScheme.onSecondaryContainer
        }

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(TioSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(TioSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                color = contentColor,
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    }
}
