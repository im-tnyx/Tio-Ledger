@file:Suppress("FunctionName")

package com.tioledger.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tioledger.ui.components.TioAddIcon
import com.tioledger.ui.components.TioAppBar
import com.tioledger.ui.components.TioBackIcon
import com.tioledger.ui.components.TioBottomNavigation
import com.tioledger.ui.components.TioDialog
import com.tioledger.ui.components.TioIconAvatar
import com.tioledger.ui.components.TioListItem
import com.tioledger.ui.components.TioReorderHandleIcon
import com.tioledger.ui.design.TioIconToken
import com.tioledger.ui.design.TioSpacing
import com.tioledger.ui.navigation.MainRoute
import com.tioledger.ui.navigation.TioNavigationGraphs
import com.tioledger.ui.navigation.bottomNavigationModel
import org.koin.compose.koinInject

@Composable
fun AccountGroupRoute(
    onNavigate: (MainRoute) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    store: AccountGroupStore = koinInject(),
) {
    val groups by store.groups.collectAsState()
    AccountGroupScreen(
        onNavigate = onNavigate,
        onNavigateBack = onNavigateBack,
        groups = groups,
        onAddGroup = { title, policy -> store.addGroup(title, policy) },
        onUpdateGroup = { id, title -> store.updateGroup(id, title) },
        onDeleteGroup = { id -> store.removeGroup(id) },
        onReorder = { from, to -> store.reorder(from, to) },
    )
}

@Composable
fun AccountGroupScreen(
    onNavigate: (MainRoute) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialGroups: List<AccountGroupEntry> = DEFAULT_ACCOUNT_GROUPS,
    groups: List<AccountGroupEntry>? = null,
    onAddGroup: ((String, AccountGroupPolicy) -> Unit)? = null,
    onUpdateGroup: ((String, String) -> Unit)? = null,
    onDeleteGroup: ((String) -> Unit)? = null,
    onReorder: ((Int, Int) -> Unit)? = null,
) {
    val bottomNavigation = TioNavigationGraphs.main.bottomNavigationModel(MainRoute.AccountGroup)
    var localGroups by remember(initialGroups) { mutableStateOf(initialGroups) }
    val currentGroups = groups ?: localGroups

    var isAddDialogVisible by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<AccountGroupEntry?>(null) }
    var itemToDelete by remember { mutableStateOf<AccountGroupEntry?>(null) }
    val stepPx = with(LocalDensity.current) { 48.dp.toPx() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TioAppBar(
                title = MainRoute.AccountGroup.title,
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics { contentDescription = "Navigate back" },
                    ) {
                        TioBackIcon()
                    }
                },
                actions = {
                    IconButton(
                        onClick = { isAddDialogVisible = true },
                        modifier = Modifier.semantics { contentDescription = "Add account group" },
                    ) {
                        TioAddIcon()
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
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding,
        ) {
            items(currentGroups, key = AccountGroupEntry::id) { entry ->
                TioListItem(
                    title = entry.title,
                    subtitle = entry.subtitle,
                    leading = {
                        if (entry.isPermanent) {
                            Box(
                                modifier = Modifier.semantics { contentDescription = "${entry.title} group icon" },
                            ) {
                                TioIconAvatar(TioIconToken.Account)
                            }
                        } else {
                            IconButton(
                                onClick = { itemToDelete = entry },
                                modifier =
                                    Modifier
                                        .size(36.dp)
                                        .semantics { contentDescription = "Delete ${entry.title}" },
                            ) {
                                Surface(
                                    modifier = Modifier.size(24.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError,
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "−",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            lineHeight = 16.sp,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }
                            }
                        }
                    },
                    trailing = {
                        var accumulatedDrag by remember { mutableStateOf(0f) }
                        var showMenu by remember { mutableStateOf(false) }
                        val latestGroups by rememberUpdatedState(currentGroups)

                        Box(
                            modifier =
                                Modifier
                                    .size(44.dp)
                                    .clickable { showMenu = true }
                                    .pointerInput(entry.id) {
                                        detectVerticalDragGestures(
                                            onDragStart = { accumulatedDrag = 0f },
                                            onDragEnd = { accumulatedDrag = 0f },
                                            onDragCancel = { accumulatedDrag = 0f },
                                            onVerticalDrag = { change, dragAmount ->
                                                change.consume()
                                                accumulatedDrag += dragAmount
                                                val currentIndex = latestGroups.indexOfFirst { it.id == entry.id }
                                                if (accumulatedDrag <= -stepPx && currentIndex > 0) {
                                                    val targetIndex = currentIndex - 1
                                                    if (onReorder != null) {
                                                        onReorder(currentIndex, targetIndex)
                                                    } else {
                                                        val mutable = latestGroups.toMutableList()
                                                        val item = mutable.removeAt(currentIndex)
                                                        mutable.add(targetIndex, item)
                                                        localGroups = mutable
                                                    }
                                                    accumulatedDrag = 0f
                                                } else if (accumulatedDrag >= stepPx && currentIndex < latestGroups.lastIndex) {
                                                    val targetIndex = currentIndex + 1
                                                    if (onReorder != null) {
                                                        onReorder(currentIndex, targetIndex)
                                                    } else {
                                                        val mutable = latestGroups.toMutableList()
                                                        val item = mutable.removeAt(currentIndex)
                                                        mutable.add(targetIndex, item)
                                                        localGroups = mutable
                                                    }
                                                    accumulatedDrag = 0f
                                                }
                                            },
                                        )
                                    }
                                    .semantics { contentDescription = "Reorder ${entry.title}" },
                            contentAlignment = Alignment.Center,
                        ) {
                            TioReorderHandleIcon()

                            val currentIndex = currentGroups.indexOfFirst { it.id == entry.id }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                            ) {
                                if (currentIndex > 0) {
                                    DropdownMenuItem(
                                        text = { Text("Move up") },
                                        onClick = {
                                            if (onReorder != null) {
                                                onReorder(currentIndex, currentIndex - 1)
                                            } else {
                                                val mutable = currentGroups.toMutableList()
                                                val item = mutable.removeAt(currentIndex)
                                                mutable.add(currentIndex - 1, item)
                                                localGroups = mutable
                                            }
                                            showMenu = false
                                        },
                                    )
                                }
                                if (currentIndex < currentGroups.lastIndex) {
                                    DropdownMenuItem(
                                        text = { Text("Move down") },
                                        onClick = {
                                            if (onReorder != null) {
                                                onReorder(currentIndex, currentIndex + 1)
                                            } else {
                                                val mutable = currentGroups.toMutableList()
                                                val item = mutable.removeAt(currentIndex)
                                                mutable.add(currentIndex + 1, item)
                                                localGroups = mutable
                                            }
                                            showMenu = false
                                        },
                                    )
                                }
                            }
                        }
                    },
                    onClick = {
                        if (entry.policy == AccountGroupPolicy.LOAN) {
                            onNavigate(MainRoute.Loans)
                        } else {
                            itemToEdit = entry
                        }
                    },
                )
                HorizontalDivider()
            }
        }
    }

    if (isAddDialogVisible) {
        AddAccountGroupDialog(
            existingGroups = currentGroups,
            onDismiss = { isAddDialogVisible = false },
            onConfirm = { newName, selectedPolicy ->
                if (onAddGroup != null) {
                    onAddGroup(newName, selectedPolicy)
                } else {
                    val newEntry =
                        AccountGroupEntry(
                            id = "group_${newName.lowercase().replace(" ", "_")}_${localGroups.size + 1}",
                            title = newName,
                            subtitle = selectedPolicy.subtitle,
                            policy = selectedPolicy,
                            isPermanent = false,
                        )
                    localGroups = localGroups + newEntry
                }
                isAddDialogVisible = false
            },
        )
    }

    itemToEdit?.let { entry ->
        EditAccountGroupDialog(
            entry = entry,
            existingGroups = currentGroups,
            onDismiss = { itemToEdit = null },
            onConfirm = { updatedName ->
                if (onUpdateGroup != null) {
                    onUpdateGroup(entry.id, updatedName)
                } else {
                    localGroups =
                        localGroups.map { current ->
                            if (current.id == entry.id) current.copy(title = updatedName) else current
                        }
                }
                itemToEdit = null
            },
        )
    }

    itemToDelete?.let { entry ->
        TioDialog(
            title = "Delete Account Group",
            text = "Are you sure you want to delete '${entry.title}'?",
            confirmLabel = "Delete",
            dismissLabel = "Cancel",
            onConfirm = {
                if (!entry.isPermanent) {
                    if (onDeleteGroup != null) {
                        onDeleteGroup(entry.id)
                    } else {
                        localGroups = localGroups.filterNot { it.id == entry.id }
                    }
                }
                itemToDelete = null
            },
            onDismiss = {
                itemToDelete = null
            },
        )
    }
}

@Composable
private fun AddAccountGroupDialog(
    existingGroups: List<AccountGroupEntry>,
    onDismiss: () -> Unit,
    onConfirm: (String, AccountGroupPolicy) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var selectedPolicy by remember { mutableStateOf(AccountGroupPolicy.LOAN) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Account Group") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(TioSpacing.sm)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("Group name") },
                    singleLine = true,
                    isError = errorMessage != null,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text = "Policy",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AccountGroupPolicy.entries.chunked(3).forEach { rowPolicies ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            rowPolicies.forEach { policy ->
                                val isSelected = policy == selectedPolicy
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color =
                                        if (isSelected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        },
                                    contentColor =
                                        if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    modifier =
                                        Modifier
                                            .weight(1f)
                                            .clickable { selectedPolicy = policy }
                                            .padding(vertical = 2.dp),
                                ) {
                                    Text(
                                        text = policy.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = name.trim()
                    when {
                        trimmed.isBlank() -> errorMessage = "Group name cannot be blank"
                        existingGroups.any { it.title.equals(trimmed, ignoreCase = true) } -> {
                            errorMessage = "Group name already exists"
                        }
                        else -> onConfirm(trimmed, selectedPolicy)
                    }
                },
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun EditAccountGroupDialog(
    entry: AccountGroupEntry,
    existingGroups: List<AccountGroupEntry>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(entry.title) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Account Group") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(TioSpacing.sm)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("Group name") },
                    singleLine = true,
                    isError = errorMessage != null,
                    modifier = Modifier.fillMaxWidth(),
                )
                errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = name.trim()
                    when {
                        trimmed.isBlank() -> errorMessage = "Group name cannot be blank"
                        existingGroups.any { it.id != entry.id && it.title.equals(trimmed, ignoreCase = true) } -> {
                            errorMessage = "Group name already exists"
                        }
                        else -> onConfirm(trimmed)
                    }
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
