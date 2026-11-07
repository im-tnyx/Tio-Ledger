@file:Suppress("FunctionName")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tioledger.ui.accounts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tioledger.application.model.ApplicationResult
import com.tioledger.application.model.UseCaseOutcome
import com.tioledger.application.usecase.account.CreateAccountCommand
import com.tioledger.application.usecase.account.CreateAccountUseCase
import com.tioledger.core.util.IdGenerator
import com.tioledger.core.util.UuidGenerator
import com.tioledger.ui.components.TioAppBar
import com.tioledger.ui.design.TioSpacing
import com.tioledger.ui.more.AccountGroupEntry
import com.tioledger.ui.more.AccountGroupPolicy
import com.tioledger.ui.more.AccountGroupStore
import com.tioledger.ui.more.DEFAULT_ACCOUNT_GROUPS
import com.tioledger.ui.navigation.MainRoute
import kotlinx.datetime.Clock
import org.koin.compose.koinInject

@Composable
fun AddAccountRoute(
    onNavigate: (MainRoute) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    createAccountUseCase: CreateAccountUseCase = koinInject(),
    groupStore: AccountGroupStore = koinInject(),
) {
    val groups by groupStore.groups.collectAsState()
    AddAccountScreen(
        onNavigateBack = onNavigateBack,
        onAccountCreated = onNavigateBack,
        onCreateAccount = { command -> createAccountUseCase(command) },
        availableGroups = groups,
    )
}

@Composable
fun AddAccountScreen(
    onNavigateBack: () -> Unit,
    onAccountCreated: () -> Unit = {},
    onCreateAccount: (CreateAccountCommand) -> ApplicationResult<*> = { ApplicationResult.Success(UseCaseOutcome(Unit)) },
    availableGroups: List<AccountGroupEntry> = DEFAULT_ACCOUNT_GROUPS,
    modifier: Modifier = Modifier,
) {
    val idGenerator: IdGenerator = remember { UuidGenerator() }
    var selectedGroup by remember { mutableStateOf<AccountGroupEntry?>(null) }
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    // Policy specific fields
    var paymentAccount by remember { mutableStateOf("") }
    var settlementDate by remember { mutableIntStateOf(1) }
    var paymentDate by remember { mutableIntStateOf(1) }
    var isAutoPayEnabled by remember { mutableStateOf(false) }

    // Loan specific fields
    var loanInterestRate by remember { mutableStateOf("") }
    var loanTenorMonths by remember { mutableStateOf("") }

    // Bank specific fields
    var bankName by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }

    // Investment specific fields
    var investmentBroker by remember { mutableStateOf("") }
    var investmentExpectedReturn by remember { mutableStateOf("") }

    var isGroupSheetVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val groupSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val currentPolicy = selectedGroup?.policy
    val isCreditCard = currentPolicy == AccountGroupPolicy.CREDIT_CARD
    val isLoan = currentPolicy == AccountGroupPolicy.LOAN
    val isBank = currentPolicy == AccountGroupPolicy.BANK
    val isInvestment = currentPolicy == AccountGroupPolicy.INVESTMENT

    val cyclePreview =
        remember(settlementDate, paymentDate) {
            CreditCardCycleCalculator.calculate(
                settlementDay = settlementDate,
                paymentDueDay = paymentDate,
            )
        }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TioAppBar(
                title = "Add",
                navigationIcon = {
                    TextButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics { contentDescription = "Navigate back to accounts" },
                    ) {
                        Text(
                            text = "‹ Accounts",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = TioSpacing.lg, vertical = TioSpacing.md),
        ) {
            // Group Row (Clickable)
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { isGroupSheetVisible = true }
                        .padding(vertical = TioSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Group",
                    modifier = Modifier.width(110.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = selectedGroup?.title ?: "",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Name Row
            AddAccountTextFieldRow(
                label = "Name",
                value = name,
                onValueChange = {
                    name = it
                    errorMessage = null
                },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Amount Row
            AddAccountTextFieldRow(
                label = "Amount",
                value = amount,
                onValueChange = { amount = it },
                keyboardType = KeyboardType.Decimal,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Dynamic Payment Account Row (for Credit Card or Loan)
            if (isCreditCard || isLoan) {
                AddAccountTextFieldRow(
                    label = "Payment",
                    value = paymentAccount,
                    onValueChange = { paymentAccount = it },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }

            // Dynamic Bank Fields
            if (isBank) {
                AddAccountTextFieldRow(
                    label = "Bank Name",
                    value = bankName,
                    onValueChange = { bankName = it },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                AddAccountTextFieldRow(
                    label = "Account No.",
                    value = accountNumber,
                    onValueChange = { accountNumber = it },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }

            // Dynamic Investment Fields
            if (isInvestment) {
                AddAccountTextFieldRow(
                    label = "Broker",
                    value = investmentBroker,
                    onValueChange = { investmentBroker = it },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                AddAccountTextFieldRow(
                    label = "Return (%)",
                    value = investmentExpectedReturn,
                    onValueChange = { investmentExpectedReturn = it },
                    keyboardType = KeyboardType.Decimal,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }

            // Description Row
            AddAccountTextFieldRow(
                label = "Description",
                value = description,
                onValueChange = { description = it },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Dynamic Credit Card Section
            if (isCreditCard) {
                Spacer(modifier = Modifier.height(TioSpacing.lg))

                // Settlement Date Row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = TioSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Settlement\nDate",
                        modifier = Modifier.width(110.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = settlementDate.toString(),
                        onValueChange = {
                            val parsed = it.filter(Char::isDigit).toIntOrNull()
                            if (parsed != null && parsed in 1..31) {
                                settlementDate = parsed
                            }
                        },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = transparentTextFieldColors(),
                    )
                    Text(
                        text = "ⓘ",
                        modifier =
                            Modifier
                                .padding(horizontal = TioSpacing.sm)
                                .semantics { contentDescription = "Statement generation day of month" },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Payment Date Row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = TioSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Payment\nDate",
                        modifier = Modifier.width(110.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = paymentDate.toString(),
                        onValueChange = {
                            val parsed = it.filter(Char::isDigit).toIntOrNull()
                            if (parsed != null && parsed in 1..31) {
                                paymentDate = parsed
                            }
                        },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = transparentTextFieldColors(),
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Statement Cycle Preview Box (Matching Screenshot 2)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = TioSpacing.md),
                    shape = RoundedCornerShape(8.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        ),
                ) {
                    Column(
                        modifier = Modifier.padding(TioSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(TioSpacing.sm),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Balance Payable",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = cyclePreview.payablePeriod,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Outst. Balance",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = cyclePreview.outstandingPeriod,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }

                // Automatic Payment Row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = TioSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Automatic Payment",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Switch(
                        checked = isAutoPayEnabled,
                        onCheckedChange = { isAutoPayEnabled = it },
                    )
                }
            }

            // Dynamic Loan Section
            if (isLoan) {
                Spacer(modifier = Modifier.height(TioSpacing.lg))
                AddAccountTextFieldRow(
                    label = "Interest Rate",
                    value = loanInterestRate,
                    onValueChange = { loanInterestRate = it },
                    keyboardType = KeyboardType.Decimal,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                AddAccountTextFieldRow(
                    label = "Tenor (mo.)",
                    value = loanTenorMonths,
                    onValueChange = { loanTenorMonths = it },
                    keyboardType = KeyboardType.Number,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }

            errorMessage?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = TioSpacing.sm),
                )
            }

            Spacer(modifier = Modifier.height(TioSpacing.xl))

            // Prominent Coral-Red Save Button (Matching Screenshot)
            Button(
                onClick = {
                    val trimmedName = name.trim()
                    val group = selectedGroup
                    when {
                        group == null -> errorMessage = "Please select an account group"
                        trimmedName.isBlank() -> errorMessage = "Account name cannot be blank"
                        else -> {
                            isSaving = true
                            errorMessage = null
                            val mappedType = group.policy.accountType
                            val command =
                                CreateAccountCommand(
                                    id = idGenerator.nextId(),
                                    name = trimmedName,
                                    type = mappedType,
                                    currencyCode = "USD",
                                    createdAt = Clock.System.now().toEpochMilliseconds(),
                                )
                            when (val result = onCreateAccount(command)) {
                                is ApplicationResult.Success -> {
                                    isSaving = false
                                    onAccountCreated()
                                }
                                is ApplicationResult.Failure -> {
                                    isSaving = false
                                    errorMessage = result.error.toString()
                                }
                            }
                        }
                    }
                },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(8.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF5350),
                        contentColor = Color.White,
                    ),
            ) {
                Text(
                    text = if (isSaving) "Saving..." else "Save",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }

    // Account Group Bottom Sheet (Matching Screenshot 1)
    if (isGroupSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { isGroupSheetVisible = false },
            sheetState = groupSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = TioSpacing.xl),
            ) {
                Text(
                    text = "Account Group",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = TioSpacing.lg, vertical = TioSpacing.md),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                availableGroups.forEach { group ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedGroup = group
                                    isGroupSheetVisible = false
                                }
                                .padding(horizontal = TioSpacing.lg, vertical = TioSpacing.md),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = group.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        group.subtitle?.let { sub ->
                            Text(
                                text = sub,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                }

                Spacer(modifier = Modifier.height(TioSpacing.md))

                // Cancel Button at bottom of sheet
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = TioSpacing.lg),
                ) {
                    Button(
                        onClick = { isGroupSheetVisible = false },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ),
                    ) {
                        Text(
                            text = "Cancel",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddAccountTextFieldRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = TioSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.width(110.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            colors = transparentTextFieldColors(),
        )
    }
}

@Composable
private fun transparentTextFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color.Transparent,
        unfocusedBorderColor = Color.Transparent,
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
    )
