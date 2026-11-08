@file:Suppress("FunctionName")

package com.tioledger.ui.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

    var showGroupPicker by remember { mutableStateOf(false) }
    var showAmountDialog by remember { mutableStateOf(false) }
    var tempAmountInput by remember { mutableStateOf("") }
    var showPaymentPicker by remember { mutableStateOf(false) }
    var showSettlementPicker by remember { mutableStateOf(false) }
    var showPaymentDatePicker by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf("") }
    var includeInTotals by remember { mutableStateOf(true) }
    var showHideSetting by remember { mutableStateOf(true) }

    var wasSaving by remember { mutableStateOf(false) }

    LaunchedEffect(state.isSaving, state.createErrorMessage) {
        if (wasSaving && !state.isSaving && state.createErrorMessage == null) {
            onNavigateBack()
        }
        wasSaving = state.isSaving
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
                // Payment Row
                AccountFormRow(label = "Payment") {
                    Text(
                        text = state.draftPaymentAccount.orEmpty(),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { showPaymentPicker = true }
                                .padding(vertical = 12.dp),
                    )
                }

                // Description Row
                AccountFormRow(label = "Description") {
                    BasicTextField(
                        value = description,
                        onValueChange = { description = it },
                        textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = 1.dp,
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Settlement Date Row
                AccountFormRow(label = "Settlement\nDate") {
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

                // Payment Date Row
                AccountFormRow(label = "Payment\nDate") {
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

                // Card Outstanding Summary Box
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .padding(16.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(text = "Balance Payable", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                            Text(text = "10/01 ~ 10/31 (Pay: 11/01)", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(text = "Outst. Balance", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                            Text(text = "11/01 ~ 11/30 (Pay: 12/01)", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                // Description Row
                AccountFormRow(label = "Description") {
                    BasicTextField(
                        value = description,
                        onValueChange = { description = it },
                        textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Include in totals Row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Include in totals",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = includeInTotals,
                        onCheckedChange = { includeInTotals = it },
                        colors =
                            SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                            ),
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Show/Hide Setting Row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Show/Hide Setting",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = showHideSetting,
                        onCheckedChange = { showHideSetting = it },
                        colors =
                            SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                            ),
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
                    text = if (state.isSaving) "Saving..." else "Save",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
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
                    state.availableGroups.forEach { group ->
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
                onNavigateBack()
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

    if (showPaymentPicker) {
        val eligiblePaymentAccounts =
            state.groups
                .filter { it.type != AccountType.CREDIT_CARD }
                .flatMap { it.accounts }
        AlertDialog(
            onDismissRequest = { showPaymentPicker = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            title = {
                Text(
                    text = "Payment Account",
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
                            .verticalScroll(rememberScrollState()),
                ) {
                    if (eligiblePaymentAccounts.isEmpty()) {
                        Text(
                            text = "No eligible bank or cash accounts available.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                    } else {
                        eligiblePaymentAccounts.forEach { acc ->
                            val isSelected = acc.name == state.draftPaymentAccount
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onAction(AccountsAction.PaymentAccountChanged(acc.name))
                                            showPaymentPicker = false
                                        }
                                        .padding(vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = acc.name,
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
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPaymentPicker = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
                }
            },
        )
    }

    if (showSettlementPicker) {
        val days = (1..31).map { "Every $it" } + "End of month"
        AlertDialog(
            onDismissRequest = { showSettlementPicker = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            title = {
                Text(
                    text = "Date of Settlement",
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
                    days.forEach { day ->
                        val isSelected = day == state.draftSettlementDate
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onAction(AccountsAction.SettlementDateChanged(day))
                                        showSettlementPicker = false
                                    }
                                    .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = day,
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
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSettlementPicker = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
                }
            },
        )
    }

    if (showPaymentDatePicker) {
        val days = (1..31).map { "Every $it" } + "End of month"
        AlertDialog(
            onDismissRequest = { showPaymentDatePicker = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            title = {
                Text(
                    text = "Date of Payment",
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
                    days.forEach { day ->
                        val isSelected = day == state.draftPaymentDate
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onAction(AccountsAction.PaymentDateChanged(day))
                                        showPaymentDatePicker = false
                                    }
                                    .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = day,
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
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPaymentDatePicker = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
                }
            },
        )
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
