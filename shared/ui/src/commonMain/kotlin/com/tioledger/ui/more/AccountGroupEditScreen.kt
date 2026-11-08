@file:Suppress("FunctionName")

package com.tioledger.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.tioledger.ui.components.TioAppBar
import com.tioledger.ui.components.TioBackIcon
import org.koin.compose.koinInject

enum class AccountGroupPolicyOption(
    val policy: AccountGroupPolicy,
    val label: String,
) {
    DEFAULT(AccountGroupPolicy.BANK, "Default"),
    CREDIT_CARD(AccountGroupPolicy.CREDIT_CARD, "Account group for credit cards"),
    DEBIT_CARD(AccountGroupPolicy.DEBIT_CARD, "Account group for debit cards"),
}

@Composable
fun AccountGroupEditRoute(
    groupId: String? = null,
    onNavigateBack: () -> Unit = {},
    store: AccountGroupStore = koinInject(),
) {
    val groups by store.groups.collectAsState()
    val groupToEdit =
        remember(groupId, groups) {
            groupId?.let { id -> groups.firstOrNull { it.id == id } }
        }

    AccountGroupEditScreen(
        entry = groupToEdit,
        existingGroups = groups,
        onNavigateBack = onNavigateBack,
        onSave = { name, policy ->
            if (groupToEdit != null) {
                store.updateGroup(groupToEdit.id, name, policy)
            } else {
                store.addGroup(name, policy) != null
            }
        },
    )
}

@Composable
fun AccountGroupEditScreen(
    entry: AccountGroupEntry?,
    existingGroups: List<AccountGroupEntry>,
    onNavigateBack: () -> Unit,
    onSave: (String, AccountGroupPolicy) -> Boolean,
    modifier: Modifier = Modifier,
) {
    val isEditMode = entry != null
    val isPermanent = entry?.isPermanent == true
    var name by remember(entry) { mutableStateOf(entry?.title.orEmpty()) }
    var selectedPolicy by remember(entry) {
        mutableStateOf(
            entry?.policy ?: AccountGroupPolicy.BANK,
        )
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TioAppBar(
                title = "Account Group",
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics { contentDescription = "Navigate back" },
                    ) {
                        TioBackIcon()
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
                    .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            // Name input row matching reference design
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Name",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp,
                    modifier = Modifier.width(95.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    BasicTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            errorMessage = null
                        },
                        textStyle =
                            TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp,
                            ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        singleLine = true,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 1.dp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Radio options list
            AccountGroupPolicyOption.entries.forEach { option ->
                val isSelected =
                    when (option) {
                        AccountGroupPolicyOption.DEFAULT ->
                            selectedPolicy == AccountGroupPolicy.BANK || selectedPolicy == AccountGroupPolicy.CASH
                        AccountGroupPolicyOption.CREDIT_CARD -> selectedPolicy == AccountGroupPolicy.CREDIT_CARD
                        AccountGroupPolicyOption.DEBIT_CARD -> selectedPolicy == AccountGroupPolicy.DEBIT_CARD
                    }

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isPermanent) {
                                selectedPolicy = option.policy
                                errorMessage = null
                            }
                            .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = {
                            if (!isPermanent) {
                                selectedPolicy = option.policy
                                errorMessage = null
                            }
                        },
                        enabled = !isPermanent,
                        colors =
                            RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                                unselectedColor = MaterialTheme.colorScheme.outline,
                            ),
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            errorMessage?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            Button(
                onClick = {
                    val trimmed = name.trim()
                    when {
                        trimmed.isBlank() -> errorMessage = "Group name cannot be blank"
                        existingGroups.any {
                            it.id != entry?.id && it.title.equals(trimmed, ignoreCase = true)
                        } -> {
                            errorMessage = "Group name already exists"
                        }
                        else -> {
                            val success = onSave(trimmed, selectedPolicy)
                            if (success) {
                                onNavigateBack()
                            } else {
                                errorMessage = "Unable to save account group. Try again."
                            }
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp),
            ) {
                Text(
                    text = "Save",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }
        }
    }
}
