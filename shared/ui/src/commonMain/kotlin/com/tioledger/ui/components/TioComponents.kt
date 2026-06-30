@file:Suppress("FunctionName")

package com.tioledger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tioledger.ui.design.TioDimensions
import com.tioledger.ui.design.TioElevation
import com.tioledger.ui.design.TioIconToken
import com.tioledger.ui.design.TioSpacing

data class TioNavigationItem(
    val label: String,
    val icon: TioIconToken,
    val selected: Boolean,
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TioAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable () -> Unit = {},
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        modifier = modifier,
        navigationIcon = { navigationIcon?.invoke() },
        actions = { actions() },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
            ),
    )
}

@Composable
fun TioBottomNavigation(
    items: List<TioNavigationItem>,
    onItemSelected: (TioNavigationItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = TioDimensions.bottomNavigationHeight),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        items.forEach { item ->
            NavigationBarItem(
                selected = item.selected,
                onClick = { onItemSelected(item) },
                icon = { TioIcon(item.icon) },
                label = {
                    Text(
                        text = item.label,
                        maxLines = 1,
                    )
                },
            )
        }
    }
}

@Composable
fun TioFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: TioIconToken = TioIconToken.Add,
    contentDescription: String = "Add",
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        TioIcon(
            token = icon,
            contentDescription = contentDescription,
        )
    }
}

@Composable
fun TioCard(
    modifier: Modifier = Modifier,
    elevated: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(TioSpacing.lg),
    content: @Composable () -> Unit,
) {
    val cardContent: @Composable () -> Unit = {
        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }

    if (elevated) {
        ElevatedCard(
            modifier = modifier,
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = TioElevation.medium),
            content = { cardContent() },
        )
    } else {
        Card(
            modifier = modifier,
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            content = { cardContent() },
        )
    }
}

@Composable
fun TioListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(enabled = onClick != null) { onClick?.invoke() }
                .padding(horizontal = TioSpacing.lg, vertical = TioSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        if (leading != null) {
            Spacer(modifier = Modifier.width(TioSpacing.md))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun TioAccountRow(
    name: String,
    balance: String,
    modifier: Modifier = Modifier,
    accountType: String? = null,
    balanceTone: TioAmountTone = TioAmountTone.Neutral,
    onClick: (() -> Unit)? = null,
) {
    TioListItem(
        title = name,
        subtitle = accountType,
        modifier = modifier.heightIn(min = TioDimensions.accountRowHeight),
        leading = { TioIconAvatar(TioIconToken.Account) },
        trailing = {
            TioAmountText(
                amount = balance,
                tone = balanceTone,
            )
        },
        onClick = onClick,
    )
}

@Composable
fun TioCategoryRow(
    name: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    color: Color = MaterialTheme.colorScheme.secondary,
    onClick: (() -> Unit)? = null,
) {
    TioListItem(
        title = name,
        subtitle = subtitle,
        modifier = modifier.heightIn(min = TioDimensions.categoryRowHeight),
        leading = {
            Box(
                modifier =
                    Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(color),
            )
        },
        onClick = onClick,
    )
}

enum class TioAmountTone {
    Positive,
    Negative,
    Neutral,
}

@Composable
fun TioAmountText(
    amount: String,
    modifier: Modifier = Modifier,
    tone: TioAmountTone = TioAmountTone.Neutral,
) {
    val color =
        when (tone) {
            TioAmountTone.Positive -> Color(0xFF15803D)
            TioAmountTone.Negative -> MaterialTheme.colorScheme.error
            TioAmountTone.Neutral -> MaterialTheme.colorScheme.onSurface
        }

    Text(
        text = amount,
        modifier = modifier,
        color = color,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun TioCurrencyBadge(
    code: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            text = code,
            modifier = Modifier.padding(horizontal = TioSpacing.md, vertical = TioSpacing.xs),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun TioEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(TioSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TioSpacing.md),
    ) {
        TioIconAvatar(
            token = TioIconToken.Transaction,
            size = 48.dp,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        action?.invoke()
    }
}

@Composable
fun TioSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search",
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        leadingIcon = { TioIcon(TioIconToken.Search) },
        placeholder = { Text(placeholder) },
    )
}

@Composable
fun TioSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = TioSpacing.lg, vertical = TioSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        action?.invoke()
    }
}

@Composable
fun TioFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier,
        leadingIcon =
            if (selected) {
                { TioIcon(TioIconToken.Filter) }
            } else {
                null
            },
    )
}

@Composable
fun TioDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissLabel: String? = null,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel)
            }
        },
        dismissButton =
            dismissLabel?.let { label ->
                {
                    TextButton(onClick = onDismiss) {
                        Text(label)
                    }
                }
            },
    )
}

@Composable
fun TioBottomSheetContent(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = TioElevation.high,
    ) {
        Column(
            modifier = Modifier.padding(TioSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(TioSpacing.md),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(width = 40.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outline),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            HorizontalDivider()
            content()
        }
    }
}

@Composable
fun TioIcon(
    token: TioIconToken,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Text(
        text = token.label.take(1),
        modifier = modifier,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
fun TioIconAvatar(
    token: TioIconToken,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 36.dp,
) {
    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            TioIcon(token = token)
        }
    }
}

private val TioIconToken.label: String
    get() =
        when (this) {
            TioIconToken.Account -> "Account"
            TioIconToken.Analytics -> "Analytics"
            TioIconToken.Budget -> "Budget"
            TioIconToken.Calendar -> "Calendar"
            TioIconToken.Category -> "Category"
            TioIconToken.Close -> "Close"
            TioIconToken.Filter -> "Filter"
            TioIconToken.Home -> "Home"
            TioIconToken.Loan -> "Loan"
            TioIconToken.Notification -> "Notification"
            TioIconToken.Search -> "Search"
            TioIconToken.Settings -> "Settings"
            TioIconToken.Add -> "Add"
            TioIconToken.Transaction -> "Transaction"
            TioIconToken.Transfer -> "Transfer"
        }
