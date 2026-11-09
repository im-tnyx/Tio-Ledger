@file:Suppress("FunctionName")

package com.tioledger.ui.accounts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tioledger.domain.model.AccountType
import com.tioledger.ui.components.TioAppBar
import com.tioledger.ui.components.TioBackIcon
import com.tioledger.ui.components.TioIcon
import com.tioledger.ui.design.TioIconToken
import com.tioledger.ui.more.AccountGroupPolicy
import org.koin.compose.koinInject

@Composable
fun AccountEditRoute(
    accountId: String? = null,
    viewModel: AccountsViewModel = koinInject(),
    onNavigateBack: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(accountId) {
        if (accountId == null) {
            viewModel.onAction(AccountsAction.AddClicked)
        } else {
            viewModel.onAction(AccountsAction.EditAccountRequested(accountId))
        }
    }

    AccountEditScreen(
        accountId = accountId,
        state = state,
        onAction = viewModel::onAction,
        onNavigateBack = onNavigateBack,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AccountEditScreen(
    accountId: String?,
    state: AccountsUiState,
    onAction: (AccountsAction) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isAddMode = accountId == null
    val accountRow = if (!isAddMode) state.groups.flatMap { it.accounts }.firstOrNull { it.id == accountId } else null

    val selectedGroup =
        state.availableGroups.firstOrNull { it.id == state.draftGroupId }
            ?: state.availableGroups.firstOrNull()

    val isCreditCard = selectedGroup?.policy == AccountGroupPolicy.CREDIT_CARD
    val eligiblePaymentAccounts =
        state.groups.flatMap { it.accounts }
            .filter { account ->
                account.accountType == AccountType.CASH ||
                    account.accountType == AccountType.BANK ||
                    account.accountType == AccountType.WALLET
            }
            .distinctBy { it.id }
    val selectedPaymentAccount = eligiblePaymentAccounts.firstOrNull { it.id == state.draftPaymentAccountId }

    var showGroupPicker by remember { mutableStateOf(false) }
    var showPaymentAccountPicker by remember { mutableStateOf(false) }
    var showAmountDialog by remember { mutableStateOf(false) }
    var tempAmountInput by remember { mutableStateOf("") }
    var showSettlementPicker by remember { mutableStateOf(false) }
    var showPaymentDatePicker by remember { mutableStateOf(false) }

    var wasSaving by remember { mutableStateOf(false) }
    var wasArchiving by remember { mutableStateOf(false) }

    LaunchedEffect(state.isSaving, state.createErrorMessage) {
        if (wasSaving && !state.isSaving && state.createErrorMessage == null) {
            onNavigateBack()
        }
        wasSaving = state.isSaving
    }

    LaunchedEffect(state.isArchiving, state.archiveErrorMessage) {
        if (wasArchiving && !state.isArchiving && state.archiveErrorMessage == null) {
            onNavigateBack()
        }
        wasArchiving = state.isArchiving
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TioAppBar(
                title = if (isAddMode) "Add Account" else "Edit Account",
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics { contentDescription = "Navigate back" },
                    ) {
                        TioBackIcon()
                    }
                },
                actions = {
                    if (!isAddMode && accountRow != null) {
                        IconButton(
                            onClick = {
                                onAction(AccountsAction.ArchiveRequested(accountRow.id, accountRow.name))
                            },
                            modifier = Modifier.semantics { contentDescription = "Archive ${accountRow.name}" },
                        ) {
                            TioIcon(TioIconToken.Delete)
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            // Group Row
            AccountFormRow(
                label = "Group",
                isUnderlineActive = showGroupPicker,
            ) {
                Text(
                    text = selectedGroup?.title.orEmpty().ifEmpty { "Select Group" },
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { showGroupPicker = true }
                            .padding(vertical = 12.dp),
                )
            }

            // Name Row
            AccountFormRow(label = "Name") {
                BasicTextField(
                    value = state.draftName,
                    onValueChange = { onAction(AccountsAction.NameChanged(it)) },
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                )
            }

            // Amount Row
            AccountFormRow(label = "Amount") {
                val amountText =
                    if (isAddMode) {
                        if (state.draftOpeningBalance.isBlank()) {
                            "${state.currencySymbol} 0.00"
                        } else {
                            "${state.currencySymbol} ${state.draftOpeningBalance}"
                        }
                    } else {
                        accountRow?.balance ?: "${state.currencySymbol} 0.00"
                    }
                Text(
                    text = amountText,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable(enabled = isAddMode) {
                                tempAmountInput = state.draftOpeningBalance
                                showAmountDialog = true
                            }
                            .padding(vertical = 12.dp),
                )
            }

            if (isCreditCard) {
                AccountFormRow(
                    label = "Payment",
                    isUnderlineActive = showPaymentAccountPicker,
                ) {
                    Text(
                        text = selectedPaymentAccount?.name ?: "Select account",
                        color =
                            if (selectedPaymentAccount == null) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        fontSize = 15.sp,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { showPaymentAccountPicker = true }
                                .padding(vertical = 12.dp),
                    )
                }

                AccountFormRow(label = "Description") {
                    BasicTextField(
                        value = state.draftDescription,
                        onValueChange = { onAction(AccountsAction.DescriptionChanged(it)) },
                        textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = 1.dp,
                )
                Spacer(modifier = Modifier.height(16.dp))

                AccountFormRow(label = "Settlement Date") {
                    Text(
                        text = state.draftSettlementDate,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { showSettlementPicker = true }
                                .padding(vertical = 12.dp),
                    )
                }

                AccountFormRow(label = "Payment Date") {
                    Text(
                        text = state.draftPaymentDate,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { showPaymentDatePicker = true }
                                .padding(vertical = 12.dp),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Dates and payment account are saved as card settings. They do not trigger payments.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = selectedGroup?.policy?.title.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            state.createErrorMessage?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }

            // Save Button
            Button(
                onClick = { onAction(AccountsAction.SaveClicked) },
                enabled = !state.isSaving,
                shape = RoundedCornerShape(8.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp),
            ) {
                Text(
                    text =
                        when {
                            state.isSaving -> "Saving..."
                            state.pendingOpeningBalanceAccountId != null -> "Retry Opening Balance"
                            else -> "Save"
                        },
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showPaymentAccountPicker) {
        AlertDialog(
            onDismissRequest = { showPaymentAccountPicker = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            title = {
                Text(
                    text = "Payment account",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
            },
            text = {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .verticalScroll(rememberScrollState()),
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onAction(AccountsAction.PaymentAccountChanged(null))
                                    showPaymentAccountPicker = false
                                }
                                .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("None", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                        if (state.draftPaymentAccountId == null) {
                            Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                    eligiblePaymentAccounts.forEach { account ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onAction(AccountsAction.PaymentAccountChanged(account.id))
                                        showPaymentAccountPicker = false
                                    }
                                    .padding(vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = account.name,
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            if (account.id == state.draftPaymentAccountId) {
                                Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    if (eligiblePaymentAccounts.isEmpty()) {
                        Text(
                            text = "Add a cash or bank account to select a payment account.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPaymentAccountPicker = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
                }
            },
        )
    }

    // Account Group Modal Selection Dialog
    if (showGroupPicker) {
        AlertDialog(
            onDismissRequest = { showGroupPicker = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            title = {
                Text(
                    text = "Account Group",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    state.availableGroups
                        .filter { isAddMode || it.policy.accountType == accountRow?.accountType }
                        .forEach { group ->
                            val isSelected = group.id == state.draftGroupId
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onAction(AccountsAction.GroupChanged(group.id))
                                            showGroupPicker = false
                                        }
                                        .padding(vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = group.title,
                                    color =
                                        if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                    fontSize = 16.sp,
                                    modifier = Modifier.weight(1f),
                                )
                                if (isSelected) {
                                    Text(
                                        text = "✓",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showGroupPicker = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
                }
            },
        )
    }

    state.archiveAccountId?.let {
        ArchiveAccountConfirmationDialog(
            accountName = state.archiveAccountName.orEmpty(),
            errorMessage = state.archiveErrorMessage,
            isArchiving = state.isArchiving,
            onConfirm = {
                onAction(AccountsAction.ArchiveConfirmed)
            },
            onDismiss = { onAction(AccountsAction.ArchiveDismissed) },
        )
    }

    if (showAmountDialog) {
        AlertDialog(
            onDismissRequest = { showAmountDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            title = {
                Text(
                    text = "Amount",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
            },
            text = {
                OutlinedTextField(
                    value = tempAmountInput,
                    onValueChange = { tempAmountInput = it },
                    label = { Text("Initial amount (${state.currencySymbol})") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onAction(AccountsAction.OpeningBalanceChanged(tempAmountInput.trim()))
                        showAmountDialog = false
                    },
                ) {
                    Text("OK", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAmountDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        )
    }

    if (showSettlementPicker || showPaymentDatePicker) {
        val isSettlementDate = showSettlementPicker
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val selectedDate = if (isSettlementDate) state.draftSettlementDate else state.draftPaymentDate
        val title = if (isSettlementDate) "Settlement Date" else "Payment Date"
        val dismissSheet = {
            showSettlementPicker = false
            showPaymentDatePicker = false
        }

        ModalBottomSheet(
            onDismissRequest = dismissSheet,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
            ) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
                (1..31).forEach { day ->
                    val dayText = day.toString()
                    val isSelected = dayText == selectedDate
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isSettlementDate) {
                                        onAction(AccountsAction.SettlementDateChanged(dayText))
                                    } else {
                                        onAction(AccountsAction.PaymentDateChanged(dayText))
                                    }
                                    dismissSheet()
                                }
                                .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = dayText,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f),
                        )
                        if (isSelected) {
                            Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                TextButton(onClick = dismissSheet, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun AccountFormRow(
    label: String,
    isUnderlineActive: Boolean = false,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 15.sp,
            lineHeight = 18.sp,
            modifier = Modifier.width(95.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            content()
            HorizontalDivider(
                color = if (isUnderlineActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                thickness = 1.dp,
            )
        }
    }
}
