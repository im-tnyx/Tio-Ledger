@file:Suppress("FunctionName")

package com.tioledger.ui.loans

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import com.tioledger.ui.components.TioAppBar
import com.tioledger.ui.components.TioBottomNavigation
import com.tioledger.ui.components.TioCard
import com.tioledger.ui.components.TioEmptyState
import com.tioledger.ui.components.TioErrorState
import com.tioledger.ui.components.TioFloatingActionButton
import com.tioledger.ui.components.TioLoadingState
import com.tioledger.ui.components.TioNavigationItem
import com.tioledger.ui.design.TioSpacing
import com.tioledger.ui.navigation.MainRoute
import com.tioledger.ui.navigation.TioNavigationGraphs
import org.koin.compose.koinInject

@Composable
fun LoansRoute(
    viewModel: LoansViewModel = koinInject(),
    onNavigate: (MainRoute) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    LoansScreen(
        state = state,
        onAction = viewModel::onAction,
        onOpenDetails = { loanId -> onNavigate(MainRoute.LoanDetails(loanId)) },
        onNavigate = onNavigate,
    )
}

@Composable
fun LoansScreen(
    state: LoansUiState,
    onAction: (LoansAction) -> Unit,
    onOpenDetails: (String) -> Unit,
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
        topBar = { TioAppBar(title = "Loans") },
        floatingActionButton = {
            TioFloatingActionButton(
                onClick = { onAction(LoansAction.AddClicked) },
                contentDescription = "Add loan",
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
                LoanSuccessMessage(
                    message = message,
                    onDismiss = { onAction(LoansAction.MessageDismissed) },
                )
            }
            when {
                state.isLoading -> TioLoadingState(label = "Loading loans")
                state.loadErrorMessage != null -> {
                    TioErrorState(
                        title = "Loans unavailable",
                        message = state.loadErrorMessage,
                        retryLabel = "Retry",
                        onRetry = { onAction(LoansAction.Retry) },
                    )
                }
                state.isEmpty -> {
                    TioEmptyState(
                        title = "No loans",
                        message = "Add a loan to keep its contractual terms and amortization schedule together.",
                        action = {
                            TextButton(onClick = { onAction(LoansAction.AddClicked) }) {
                                Text("Add loan")
                            }
                        },
                    )
                }
                else -> {
                    LoanList(
                        loans = state.loans,
                        onOpenDetails = onOpenDetails,
                    )
                }
            }
        }
    }

    state.editor?.let { editor ->
        when (state.accountPicker) {
            LoanAccountPicker.LOAN_ACCOUNT -> {
                LoanAccountPickerDialog(
                    title = "Select linked loan account",
                    options = state.loanAccountOptions,
                    selectedAccountId = editor.loanAccountId,
                    emptyMessage = "Create an active LOAN_LINKED account before adding a loan.",
                    onSelected = { onAction(LoansAction.LoanAccountSelected(it)) },
                    onDismiss = { onAction(LoansAction.AccountPickerDismissed) },
                )
            }
            LoanAccountPicker.DISBURSED_ACCOUNT -> {
                LoanAccountPickerDialog(
                    title = "Select disbursed account",
                    options = state.disbursedAccountOptions,
                    selectedAccountId = editor.disbursedAccountId,
                    emptyMessage = "No active asset account uses the selected loan currency.",
                    onSelected = { onAction(LoansAction.DisbursedAccountSelected(it)) },
                    onDismiss = { onAction(LoansAction.AccountPickerDismissed) },
                )
            }
            null -> {
                LoanEditorDialog(
                    editor = editor,
                    loanAccountLabel = state.accountOptions.selectedLabel(editor.loanAccountId),
                    disbursedAccountLabel = state.accountOptions.selectedLabel(editor.disbursedAccountId),
                    isSaving = state.isSaving,
                    validationErrorMessage = state.validationErrorMessage,
                    persistenceErrorMessage = state.persistenceErrorMessage,
                    onAction = onAction,
                )
            }
        }
    }
}

@Composable
fun LoanDetailsRoute(
    loanId: String,
    viewModel: LoanDetailsViewModel = koinInject(),
    onNavigateBack: () -> Unit = {},
) {
    LaunchedEffect(loanId) {
        viewModel.load(loanId)
    }
    val state by viewModel.uiState.collectAsState()
    LoanDetailsScreen(
        state = state,
        onRetry = viewModel::retry,
        onNavigateBack = onNavigateBack,
    )
}

@Composable
fun LoanDetailsScreen(
    state: LoanDetailsUiState,
    onRetry: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TioAppBar(
                title = state.details?.name ?: "Loan details",
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Back")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> {
                Column(
                    modifier =
                        Modifier
                            .padding(padding)
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .fillMaxSize(),
                ) {
                    TioLoadingState(label = "Loading loan details")
                }
            }
            state.errorMessage != null -> {
                Column(
                    modifier =
                        Modifier
                            .padding(padding)
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .fillMaxSize(),
                ) {
                    TioErrorState(
                        title = "Loan details unavailable",
                        message = state.errorMessage,
                        retryLabel = "Retry",
                        onRetry = onRetry,
                    )
                }
            }
            state.details != null -> {
                LoanDetailsContent(
                    details = state.details,
                    contentPadding = padding,
                )
            }
            else -> {
                Column(
                    modifier =
                        Modifier
                            .padding(padding)
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .fillMaxSize(),
                ) {
                    TioEmptyState(
                        title = "Loan details",
                        message = "Select a loan to view its persisted schedule.",
                    )
                }
            }
        }
    }
}

@Composable
private fun LoanList(
    loans: List<LoanRowUiModel>,
    onOpenDetails: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(TioSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(TioSpacing.md),
    ) {
        items(loans, key = LoanRowUiModel::id) { loan ->
            LoanCard(
                loan = loan,
                onClick = { onOpenDetails(loan.id) },
            )
        }
    }
}

@Composable
private fun LoanCard(
    loan: LoanRowUiModel,
    onClick: () -> Unit,
) {
    val semanticDescription =
        "${loan.name}, ${loan.statusLabel}, outstanding ${loan.outstandingLabel}, " +
            "EMI ${loan.scheduledEmiLabel}, ${loan.remainingInstallmentsLabel}, " +
            "next due ${loan.nextDueDateLabel}"
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
                        text = loan.name,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = loan.statusLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Column(modifier = Modifier.padding(start = TioSpacing.md)) {
                    Text(
                        text = "Outstanding",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = loan.outstandingLabel,
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
            HorizontalDivider()
            LoanValueRow("Principal", loan.principalLabel)
            LoanValueRow("Scheduled EMI", loan.scheduledEmiLabel)
            LoanValueRow("Installments", loan.remainingInstallmentsLabel)
            LoanValueRow("Next due", loan.nextDueDateLabel)
            Text(
                text = "Select to view schedule",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LoanEditorDialog(
    editor: LoanEditorUiState,
    loanAccountLabel: String,
    disbursedAccountLabel: String,
    isSaving: Boolean,
    validationErrorMessage: String?,
    persistenceErrorMessage: String?,
    onAction: (LoansAction) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onAction(LoansAction.EditorDismissed) },
        title = { Text("Add loan") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(TioSpacing.md),
            ) {
                item {
                    Text(
                        text = "Monthly fixed-rate reducing-balance loan",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item {
                    OutlinedTextField(
                        value = editor.name,
                        onValueChange = { onAction(LoansAction.NameChanged(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Name") },
                        singleLine = true,
                        enabled = !isSaving,
                    )
                }
                item {
                    OutlinedTextField(
                        value = editor.principal,
                        onValueChange = { onAction(LoansAction.PrincipalChanged(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Principal") },
                        supportingText = { Text("Use at most 2 decimal places") },
                        singleLine = true,
                        enabled = !isSaving,
                    )
                }
                item {
                    OutlinedTextField(
                        value = editor.annualInterestRatePercent,
                        onValueChange = { onAction(LoansAction.InterestRateChanged(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Annual interest rate (%)") },
                        supportingText = { Text("Example: 8.75") },
                        singleLine = true,
                        enabled = !isSaving,
                    )
                }
                item {
                    OutlinedTextField(
                        value = editor.tenureMonths,
                        onValueChange = { onAction(LoansAction.TenureChanged(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Tenure (months)") },
                        singleLine = true,
                        enabled = !isSaving,
                    )
                }
                item {
                    OutlinedTextField(
                        value = editor.startDate,
                        onValueChange = { onAction(LoansAction.StartDateChanged(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Start date") },
                        supportingText = { Text("YYYY-MM-DD") },
                        singleLine = true,
                        enabled = !isSaving,
                    )
                }
                item {
                    AccountSelector(
                        label = "Linked loan account",
                        value = loanAccountLabel,
                        onClick = { onAction(LoansAction.LoanAccountClicked) },
                        enabled = !isSaving,
                    )
                }
                item {
                    AccountSelector(
                        label = "Disbursed account",
                        value = disbursedAccountLabel,
                        onClick = { onAction(LoansAction.DisbursedAccountClicked) },
                        enabled = !isSaving,
                    )
                }
                validationErrorMessage?.let { message ->
                    item { LoanDialogError(message) }
                }
                persistenceErrorMessage?.let { message ->
                    item { LoanDialogError(message) }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAction(LoansAction.SaveClicked) },
                enabled = !isSaving,
            ) {
                Text(if (isSaving) "Saving" else "Add")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onAction(LoansAction.EditorDismissed) },
                enabled = !isSaving,
            ) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun AccountSelector(
    label: String,
    value: String,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
        )
        TextButton(
            onClick = onClick,
            enabled = enabled,
        ) {
            Text(value)
        }
    }
}

@Composable
private fun LoanAccountPickerDialog(
    title: String,
    options: List<LoanAccountOption>,
    selectedAccountId: String?,
    emptyMessage: String,
    onSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (options.isEmpty()) {
                Text(emptyMessage)
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(TioSpacing.xs),
                ) {
                    items(options, key = LoanAccountOption::id) { option ->
                        TextButton(
                            onClick = { onSelected(option.id) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = if (option.id == selectedAccountId) "✓ ${option.label}" else option.label,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun LoanDetailsContent(
    details: LoanDetailsUiModel,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        modifier =
            Modifier
                .padding(contentPadding)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .fillMaxSize(),
        contentPadding = PaddingValues(TioSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(TioSpacing.md),
    ) {
        item {
            TioCard(elevated = true) {
                Column(verticalArrangement = Arrangement.spacedBy(TioSpacing.md)) {
                    Text(
                        text = details.statusLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    LoanSummaryValue("Outstanding principal", details.outstandingLabel)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(TioSpacing.md),
                    ) {
                        LoanSummaryValue(
                            label = "Scheduled EMI",
                            value = details.scheduledEmiLabel,
                            modifier = Modifier.weight(1f),
                        )
                        LoanSummaryValue(
                            label = "Total interest",
                            value = details.totalInterestLabel,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    LoanValueRow("Total payable", details.totalPayableLabel)
                    LoanValueRow("Remaining installments", details.remainingInstallmentsLabel)
                    LoanValueRow("Next due", details.nextDueDateLabel)
                }
            }
        }
        item {
            TioCard {
                Column(verticalArrangement = Arrangement.spacedBy(TioSpacing.sm)) {
                    Text("Loan terms", style = MaterialTheme.typography.titleMedium)
                    LoanValueRow("Principal", details.principalLabel)
                    LoanValueRow("Annual rate", details.annualRateLabel)
                    LoanValueRow("Tenure", details.tenureLabel)
                    LoanValueRow("Start date", details.startDateLabel)
                    LoanValueRow("Linked loan account", details.loanAccountLabel)
                    LoanValueRow("Disbursed account", details.disbursedAccountLabel)
                }
            }
        }
        item {
            Text(
                text = "Amortization schedule",
                style = MaterialTheme.typography.titleMedium,
            )
        }
        if (details.schedule.isEmpty()) {
            item {
                TioEmptyState(
                    title = "No installments",
                    message = "This loan does not have a persisted schedule.",
                )
            }
        } else {
            items(details.schedule, key = LoanInstallmentUiModel::id) { installment ->
                LoanInstallmentCard(installment)
            }
        }
    }
}

@Composable
private fun LoanInstallmentCard(installment: LoanInstallmentUiModel) {
    val semanticDescription =
        "Installment ${installment.installmentNumber}, due ${installment.dueDateLabel}, " +
            "payment ${installment.paymentLabel}, principal ${installment.principalLabel}, " +
            "interest ${installment.interestLabel}, opening ${installment.openingBalanceLabel}, " +
            "closing ${installment.closingBalanceLabel}, ${installment.statusLabel}"
    TioCard(
        modifier = Modifier.semantics { contentDescription = semanticDescription },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(TioSpacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Installment ${installment.installmentNumber}",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = installment.statusLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            LoanValueRow("Due date", installment.dueDateLabel)
            LoanValueRow("Payment", installment.paymentLabel)
            LoanValueRow("Principal", installment.principalLabel)
            LoanValueRow("Interest", installment.interestLabel)
            LoanValueRow("Opening balance", installment.openingBalanceLabel)
            LoanValueRow("Closing balance", installment.closingBalanceLabel)
        }
    }
}

@Composable
private fun LoanSummaryValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun LoanValueRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            modifier = Modifier.padding(start = TioSpacing.md),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun LoanSuccessMessage(
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
private fun LoanDialogError(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
    )
}

private fun List<LoanAccountOption>.selectedLabel(accountId: String?): String =
    firstOrNull { it.id == accountId }?.label ?: "Select account"
