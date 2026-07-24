@file:Suppress("FunctionName")

package com.tioledger.ui.sms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tioledger.domain.model.SmsMissingField
import com.tioledger.domain.model.SmsParseConfidence
import com.tioledger.domain.model.SmsPaymentRail
import com.tioledger.domain.model.SmsTransactionDirection
import com.tioledger.ui.components.TioAppBar
import com.tioledger.ui.components.TioEmptyState
import com.tioledger.ui.components.TioErrorState
import com.tioledger.ui.components.TioIcon
import com.tioledger.ui.components.TioListItem
import com.tioledger.ui.components.TioLoadingState
import com.tioledger.ui.design.TioIconToken
import com.tioledger.ui.design.TioSpacing
import com.tioledger.ui.transactions.datePickerMillisToTransactionDateSelection
import com.tioledger.ui.transactions.transactionTimestampToDatePickerMillis
import kotlinx.datetime.TimeZone
import org.koin.compose.koinInject

@Composable
fun SmsTransactionReviewRoute(
    viewModel: SmsTransactionReviewViewModel = koinInject(),
    onNavigateBack: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    val event by viewModel.event.collectAsState()

    LaunchedEffect(event) {
        when (event) {
            is SmsTransactionReviewEvent.TransactionSaved, SmsTransactionReviewEvent.Rejected -> {
                viewModel.onAction(SmsTransactionReviewAction.EventConsumed)
                onNavigateBack()
            }
            is SmsTransactionReviewEvent.DateSelectionRequested, null -> Unit
        }
    }

    SmsTransactionReviewScreen(
        state = state,
        onAction = viewModel::onAction,
        onNavigateBack = onNavigateBack,
    )

    val dateRequest = event as? SmsTransactionReviewEvent.DateSelectionRequested
    if (dateRequest != null) {
        SmsReviewDatePickerDialog(
            currentTimestamp = dateRequest.currentTimestamp,
            onDismiss = { viewModel.onAction(SmsTransactionReviewAction.EventConsumed) },
            onDateSelected = { timestamp, label ->
                viewModel.onAction(
                    SmsTransactionReviewAction.DateSelected(
                        timestamp = timestamp,
                        label = label,
                    ),
                )
            },
        )
    }
}

@Composable
fun SmsTransactionReviewScreen(
    state: SmsTransactionReviewUiState,
    onAction: (SmsTransactionReviewAction) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TioAppBar(
                title = "Review SMS",
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        TioIcon(TioIconToken.Close, contentDescription = "Close SMS review")
                    }
                },
            )
        },
    ) { padding ->
        when (state.stage) {
            SmsReviewStage.INPUT ->
                SmsInputContent(
                    state = state,
                    onAction = onAction,
                    modifier = Modifier.padding(padding),
                )
            SmsReviewStage.PARSING ->
                TioLoadingState(
                    label = "Preparing transaction review...",
                    modifier = Modifier.padding(padding),
                )
            SmsReviewStage.DISABLED ->
                TioEmptyState(
                    title = "SMS review is unavailable",
                    message = state.statusMessage ?: "This experimental feature is disabled.",
                    modifier = Modifier.padding(padding),
                )
            SmsReviewStage.IGNORED ->
                ResultStateContent(
                    title = "No transaction detected",
                    message = state.statusMessage.orEmpty(),
                    evidence = state.evidence,
                    onTryAnother = { onAction(SmsTransactionReviewAction.TryAnotherMessage) },
                    modifier = Modifier.padding(padding),
                )
            SmsReviewStage.UNSUPPORTED ->
                UnsupportedStateContent(
                    state = state,
                    onTryAnother = { onAction(SmsTransactionReviewAction.TryAnotherMessage) },
                    modifier = Modifier.padding(padding),
                )
            SmsReviewStage.REVIEW ->
                SmsReviewContent(
                    state = state,
                    onAction = onAction,
                    modifier = Modifier.padding(padding),
                )
            SmsReviewStage.SAVED ->
                TioLoadingState(
                    label = "Transaction saved. Returning to transactions...",
                    modifier = Modifier.padding(padding),
                )
            SmsReviewStage.REJECTED ->
                TioLoadingState(
                    label = "Suggestion rejected. Returning...",
                    modifier = Modifier.padding(padding),
                )
        }
    }

    val accountPicker = state.activeAccountPicker
    if (accountPicker != null) {
        val options =
            if (accountPicker == SmsReviewAccountPickerTarget.DESTINATION) {
                state.visibleAccountOptions.filterNot { it.id == state.selectedAccountId }
            } else {
                state.visibleAccountOptions
            }
        SmsAccountPickerDialog(
            title =
                if (accountPicker == SmsReviewAccountPickerTarget.SOURCE) {
                    "Select account"
                } else {
                    "Select destination account"
                },
            options = options,
            onDismiss = { onAction(SmsTransactionReviewAction.AccountPickerDismissed) },
            onSelected = { onAction(SmsTransactionReviewAction.AccountSelected(it)) },
        )
    }

    if (state.isCategoryPickerVisible) {
        SmsCategoryPickerDialog(
            options = state.visibleCategoryOptions,
            onDismiss = { onAction(SmsTransactionReviewAction.CategoryPickerDismissed) },
            onSelected = { onAction(SmsTransactionReviewAction.CategorySelected(it)) },
        )
    }
}

@Composable
private fun SmsInputContent(
    state: SmsTransactionReviewUiState,
    onAction: (SmsTransactionReviewAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(TioSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(TioSpacing.lg),
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(modifier = Modifier.padding(TioSpacing.lg)) {
                Text(
                    text = "Private, assisted review",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(TioSpacing.sm))
                Text(
                    text =
                        "Paste one bank, card, UPI, wallet, or ATM message. " +
                            "Parsing stays deterministic and nothing is saved until you confirm.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        InlineMessageCard(
            message = state.validationErrorMessage,
            onDismiss = { onAction(SmsTransactionReviewAction.MessageDismissed) },
        )

        OutlinedTextField(
            value = state.messageInput,
            onValueChange = { onAction(SmsTransactionReviewAction.MessageChanged(it)) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp)
                    .semantics { contentDescription = "Transaction SMS input" },
            label = { Text("Message text") },
            placeholder = { Text("Paste a transaction alert") },
            minLines = 5,
        )

        OutlinedTextField(
            value = state.defaultCurrencyCode,
            onValueChange = { onAction(SmsTransactionReviewAction.DefaultCurrencyChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Default currency") },
            supportingText = { Text("Used only when the message has no currency marker") },
            singleLine = true,
        )

        Button(
            onClick = { onAction(SmsTransactionReviewAction.ParseClicked) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Prepare SMS transaction review" },
            enabled = state.canParse,
        ) {
            Text("Prepare review")
        }
    }
}

@Composable
private fun ResultStateContent(
    title: String,
    message: String,
    evidence: List<SmsReviewEvidenceUiModel>,
    onTryAnother: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(TioSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(TioSpacing.lg),
    ) {
        TioEmptyState(
            title = title,
            message = message,
            action = {
                TextButton(onClick = onTryAnother) {
                    Text("Try another message")
                }
            },
        )
        EvidenceCard(evidence)
    }
}

@Composable
private fun UnsupportedStateContent(
    state: SmsTransactionReviewUiState,
    onTryAnother: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(TioSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(TioSpacing.lg),
    ) {
        TioErrorState(
            title = "Review could not be prepared",
            message = state.statusMessage.orEmpty(),
            retryLabel = "Try another message",
            onRetry = onTryAnother,
        )
        MissingFieldsCard(state.unresolvedFields)
        EvidenceCard(state.evidence)
    }
}

@Composable
private fun SmsReviewContent(
    state: SmsTransactionReviewUiState,
    onAction: (SmsTransactionReviewAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(TioSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(TioSpacing.lg),
    ) {
        ConfidenceCard(state.confidence)
        MissingFieldsCard(state.unresolvedFields)
        InlineMessageCard(
            message = state.validationErrorMessage,
            onDismiss = { onAction(SmsTransactionReviewAction.MessageDismissed) },
        )
        InlineMessageCard(
            message = state.persistenceErrorMessage,
            onDismiss = { onAction(SmsTransactionReviewAction.MessageDismissed) },
        )

        DirectionSelector(
            selectedDirection = state.direction,
            onDirectionSelected = { onAction(SmsTransactionReviewAction.DirectionChanged(it)) },
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(TioSpacing.md),
        ) {
            OutlinedTextField(
                value = state.amount,
                onValueChange = { onAction(SmsTransactionReviewAction.AmountChanged(it)) },
                modifier = Modifier.weight(2f),
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
            )
            OutlinedTextField(
                value = state.currencyCode,
                onValueChange = { onAction(SmsTransactionReviewAction.CurrencyChanged(it)) },
                modifier = Modifier.weight(1f),
                label = { Text("Currency") },
                singleLine = true,
            )
        }

        SelectorCard(
            label = if (state.direction == SmsTransactionDirection.TRANSFER_CANDIDATE) "From account" else "Account",
            value = state.selectedAccount?.name ?: "Select account",
            subtitle = state.selectedAccount?.subtitle,
            onClick = { onAction(SmsTransactionReviewAction.SourceAccountClicked) },
        )

        if (state.direction == SmsTransactionDirection.TRANSFER_CANDIDATE) {
            SelectorCard(
                label = "To account",
                value = state.selectedDestinationAccount?.name ?: "Select destination account",
                subtitle = state.selectedDestinationAccount?.subtitle,
                onClick = { onAction(SmsTransactionReviewAction.DestinationAccountClicked) },
            )
        } else {
            SelectorCard(
                label = "Category",
                value = state.selectedCategory?.name ?: "Select category",
                subtitle = state.selectedCategory?.subtitle,
                onClick = { onAction(SmsTransactionReviewAction.CategoryClicked) },
            )
        }

        SelectorCard(
            label = "Transaction date",
            value = state.selectedDate.ifBlank { "Select date" },
            subtitle = null,
            onClick = { onAction(SmsTransactionReviewAction.DateClicked) },
        )

        OutlinedTextField(
            value = state.note,
            onValueChange = { onAction(SmsTransactionReviewAction.NoteChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Merchant, counterparty, or note") },
            minLines = 2,
        )

        state.paymentRail?.let { rail ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Text(
                    text = "Detected payment rail: ${rail.displayName()}",
                    modifier = Modifier.padding(TioSpacing.lg),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        EvidenceCard(state.evidence)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(TioSpacing.md),
        ) {
            TextButton(
                onClick = { onAction(SmsTransactionReviewAction.RejectClicked) },
                modifier =
                    Modifier
                        .weight(1f)
                        .semantics { contentDescription = "Reject SMS transaction suggestion" },
                enabled = !state.isSaving,
            ) {
                Text("Reject")
            }
            Button(
                onClick = { onAction(SmsTransactionReviewAction.SaveClicked) },
                modifier =
                    Modifier
                        .weight(1f)
                        .semantics { contentDescription = "Save confirmed SMS transaction" },
                enabled = state.canSave,
            ) {
                Text(if (state.isSaving) "Saving..." else "Save")
            }
        }
    }
}

@Composable
private fun ConfidenceCard(confidence: SmsParseConfidence?) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(TioSpacing.lg)) {
            Text(
                text = "Assisted suggestion",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(TioSpacing.xs))
            Text(
                text = "Confidence: ${confidence?.displayName() ?: "Needs review"}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Review every field. Nothing is saved until you select Save.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun DirectionSelector(
    selectedDirection: SmsTransactionDirection?,
    onDirectionSelected: (SmsTransactionDirection) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(TioSpacing.sm)) {
        Text(
            text = "Transaction type",
            style = MaterialTheme.typography.labelLarge,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(TioSpacing.sm),
        ) {
            SmsTransactionDirection.entries.forEach { direction ->
                FilterChip(
                    selected = selectedDirection == direction,
                    onClick = { onDirectionSelected(direction) },
                    label = { Text(direction.displayName()) },
                )
            }
        }
    }
}

@Composable
private fun SelectorCard(
    label: String,
    value: String,
    subtitle: String?,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        TextButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun MissingFieldsCard(fields: List<SmsMissingField>) {
    if (fields.isEmpty()) return
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(modifier = Modifier.padding(TioSpacing.lg)) {
            Text(
                text = "Check these fields",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            fields.distinct().forEach { field ->
                Text(
                    text = "• ${field.displayName()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

@Composable
private fun EvidenceCard(evidence: List<SmsReviewEvidenceUiModel>) {
    if (evidence.isEmpty()) return
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(TioSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(TioSpacing.sm),
        ) {
            Text(
                text = "Why this was suggested",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            evidence.forEach { item ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = item.label,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(modifier = Modifier.width(TioSpacing.md))
                    Text(
                        text = item.value,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun InlineMessageCard(
    message: String?,
    onDismiss: () -> Unit,
) {
    if (message == null) return
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(TioSpacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    }
}

@Composable
private fun SmsAccountPickerDialog(
    title: String,
    options: List<SmsReviewAccountUiModel>,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (options.isEmpty()) {
                Text("No compatible accounts are available for this currency.")
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(options, key = SmsReviewAccountUiModel::id) { option ->
                        TioListItem(
                            title = option.name,
                            subtitle = option.subtitle,
                            onClick = { onSelected(option.id) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}

@Composable
private fun SmsCategoryPickerDialog(
    options: List<SmsReviewCategoryUiModel>,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select category") },
        text = {
            if (options.isEmpty()) {
                Text("No compatible categories are available for this transaction type.")
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(options, key = SmsReviewCategoryUiModel::id) { option ->
                        TioListItem(
                            title = option.name,
                            subtitle = option.subtitle,
                            onClick = { onSelected(option.id) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SmsReviewDatePickerDialog(
    currentTimestamp: Long,
    onDismiss: () -> Unit,
    onDateSelected: (Long, String) -> Unit,
) {
    val timeZone = TimeZone.currentSystemDefault()
    val datePickerState =
        androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis =
                transactionTimestampToDatePickerMillis(
                    timestamp = currentTimestamp,
                    timeZone = timeZone,
                ),
        )
    val selectedDateMillis = datePickerState.selectedDateMillis

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    selectedDateMillis?.let { pickerTimestamp ->
                        val selection =
                            datePickerMillisToTransactionDateSelection(
                                pickerTimestamp = pickerTimestamp,
                                timeZone = timeZone,
                            )
                        onDateSelected(selection.timestamp, selection.label)
                    }
                },
                enabled = selectedDateMillis != null,
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}

private fun SmsTransactionDirection.displayName(): String =
    when (this) {
        SmsTransactionDirection.INCOME -> "Income"
        SmsTransactionDirection.EXPENSE -> "Expense"
        SmsTransactionDirection.TRANSFER_CANDIDATE -> "Transfer"
    }

private fun SmsParseConfidence.displayName(): String =
    when (this) {
        SmsParseConfidence.HIGH -> "High"
        SmsParseConfidence.MEDIUM -> "Medium"
        SmsParseConfidence.LOW -> "Low"
    }

private fun SmsPaymentRail.displayName(): String =
    when (this) {
        SmsPaymentRail.BANK -> "Bank"
        SmsPaymentRail.CARD -> "Card"
        SmsPaymentRail.UPI -> "UPI"
        SmsPaymentRail.WALLET -> "Wallet"
        SmsPaymentRail.ATM -> "ATM"
    }

private fun SmsMissingField.displayName(): String =
    when (this) {
        SmsMissingField.AMOUNT -> "Amount"
        SmsMissingField.CURRENCY -> "Currency"
        SmsMissingField.DIRECTION -> "Transaction type"
        SmsMissingField.ACCOUNT -> "Account"
        SmsMissingField.CATEGORY -> "Category"
        SmsMissingField.DESTINATION_ACCOUNT -> "Destination account"
        SmsMissingField.TRANSACTION_TIME -> "Transaction date or time"
    }
